package com.muna.filebackups

import java.io.File
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * In-memory model for a backup task.
 *
 * [id], [filePath], [maxBackups], and [isRunning] are persisted in Room (`backup_tasks`).
 * [currentBackups] is **not** stored in the database: it reflects timestamped folders under
 * the watched file’s `backups/` directory (excluding `current/`), and is refreshed from disk
 * when loading from the DB and live while a task runs (see `BackupService`).
 */
data class BackupTask @OptIn(ExperimentalUuidApi::class) constructor(
    /** The id of the backup task. */
    val id: Uuid,

    /** The path to the file to be backed up. */
    val filePath: String,

    /**
     * Maximum number of timestamped backups to keep under the file's `backups/` folder.
     * The `backups/current/` directory does not count toward this limit.
     * When a new backup is created and the count (excluding `current`) exceeds this value,
     * the oldest backup folder is removed until the count is at most this value.
     */
    val maxBackups: Int,

    /** Whether the backup task is currently running. */
    val isRunning: Boolean = false,

    /**
     * Number of backup entries under `backups/` next to the watched file, excluding
     * the `current/` folder (i.e. timestamped backup folders only).
     */
    val currentBackups: Int = 0,
) {
    val fileName: String get() = File(filePath).name
}
