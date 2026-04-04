package com.muna.filebackups.service

import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchKey
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * Watches a single file for modifications and keeps timestamped backup snapshots.
 *
 * Layout under the file's parent directory:
 * ```
 * backups/
 *   current/{fileName}          ← working copy of the last known file (not counted as a backup)
 *   {yyyyMMdd_HHmmss_SSS}/{fileName} ← one retained backup per folder
 * ```
 *
 * [maxBackups] applies only to timestamped folders under `backups/` — `current/` is excluded.
 * After each new backup folder is created, if the number of those folders is **greater than**
 * [maxBackups], the **oldest** folder (by timestamp name) is removed, and this repeats until
 * the count is at most [maxBackups].
 *
 * [currentBackupCount] reports the number of directories under `backups/` **excluding** `current/`.
 *
 * Watch events are **debounced**: rapid consecutive `ENTRY_MODIFY` events (typical of editors that
 * save in multiple steps) reset a 1-second timer; a backup runs only after that period passes with
 * no further events for this file.
 *
 * When a backup runs:
 * 1. Copies `backups/current/{fileName}` → `backups/{timestamp}/{fileName}` (the previous version).
 * 2. Overwrites `backups/current/{fileName}` with the live file (the new version for next time).
 * 3. Deletes oldest timestamped backups while their count exceeds [maxBackups].
 */
class BackupService(
    private val filePath: String,
    private val maxBackups: Int,
    private val onBackupCountChanged: (Int) -> Unit,
) {
    private val sourceFile = File(filePath)
    private val backupsRoot: File = sourceFile.parentFile.resolve("backups")
    private val currentDir: File = backupsRoot.resolve("current")

    private val timestampFormat: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS")
            .withZone(ZoneId.systemDefault())

    @Volatile
    private var running = false
    private var watchKey: WatchKey? = null

    private val debounceExecutor =
        Executors.newSingleThreadScheduledExecutor { runnable ->
            Thread(runnable, "filebackups-debounce-${sourceFile.name}").apply { isDaemon = true }
        }
    private val debounceLock = Any()
    private var pendingDebounce: ScheduledFuture<*>? = null

    fun start() {
        check(sourceFile.isFile) { "Source file does not exist: $filePath" }
        ensureDirectories()
        seedCurrentCopy()
        pruneOldSnapshots()
        running = true
        notifyCount()
    }

    /**
     * Blocking poll loop — call from a dedicated thread / coroutine dispatcher.
     * Returns when [stop] is called or the watch key becomes invalid.
     */
    fun pollLoop() {
        val watchService = FileSystems.getDefault().newWatchService()
        val parentPath: Path = sourceFile.parentFile.toPath()
        watchKey = parentPath.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY)

        try {
            while (running) {
                val key = watchService.poll(500, java.util.concurrent.TimeUnit.MILLISECONDS)
                    ?: continue

                for (event in key.pollEvents()) {
                    val changed = event.context() as? Path ?: continue
                    if (changed.toString() == sourceFile.name) {
                        scheduleDebouncedBackup()
                    }
                }

                if (!key.reset()) break
            }
        } finally {
            watchService.close()
        }
    }

    fun stop() {
        running = false
        watchKey?.cancel()
        synchronized(debounceLock) {
            pendingDebounce?.cancel(false)
            pendingDebounce = null
        }
        debounceExecutor.shutdownNow()
    }

    /** Number of backup folders under `backups/`, excluding `current/`. */
    fun currentBackupCount(): Int = snapshotDirs().size

    private fun scheduleDebouncedBackup() {
        synchronized(debounceLock) {
            if (!running) return
            pendingDebounce?.cancel(false)
            pendingDebounce = debounceExecutor.schedule(
                {
                    if (!running) return@schedule
                    performStabilizedBackup()
                },
                DEBOUNCE_QUIET_MS,
                TimeUnit.MILLISECONDS,
            )
        }
    }

    private fun performStabilizedBackup() {
        val currentCopy = File(currentDir, sourceFile.name)
        if (currentCopy.exists()) {
            val timestamp = timestampFormat.format(Instant.now())
            val snapshotDir = File(backupsRoot, timestamp)
            snapshotDir.mkdirs()
            Files.copy(
                currentCopy.toPath(),
                snapshotDir.resolve(sourceFile.name).toPath(),
                StandardCopyOption.REPLACE_EXISTING,
            )
        }

        refreshCurrentCopy()
        pruneOldSnapshots()
        notifyCount()
    }

    private fun ensureDirectories() {
        currentDir.mkdirs()
    }

    private fun seedCurrentCopy() {
        val dest = File(currentDir, sourceFile.name)
        Files.copy(
            sourceFile.toPath(),
            dest.toPath(),
            StandardCopyOption.REPLACE_EXISTING,
        )
    }

    private fun refreshCurrentCopy() {
        if (!sourceFile.exists()) return
        val dest = File(currentDir, sourceFile.name)
        Files.copy(
            sourceFile.toPath(),
            dest.toPath(),
            StandardCopyOption.REPLACE_EXISTING,
        )
    }

    private fun pruneOldSnapshots() {
        while (true) {
            val snapshots = snapshotDirs().sortedBy { it.name }
            if (snapshots.size <= maxBackups) break
            snapshots.first().deleteRecursively()
        }
    }

    private fun snapshotDirs(): List<File> =
        backupsRoot.listFiles()
            ?.filter { it.isDirectory && it.name != "current" }
            ?: emptyList()

    private fun notifyCount() {
        onBackupCountChanged(currentBackupCount())
    }

    private companion object {
        private const val DEBOUNCE_QUIET_MS = 1000L
    }
}
