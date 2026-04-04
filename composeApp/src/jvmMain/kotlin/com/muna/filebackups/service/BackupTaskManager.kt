package com.muna.filebackups.service

import com.muna.filebackups.BackupTask
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Manages the lifecycle of [BackupService] instances for each [BackupTask].
 *
 * Call [start] / [stop] when the user toggles a task, and [remove] when a task is deleted.
 * Persisted `isRunning` / task rows are updated by the UI layer (Room), not here.
 * The [onBackupCountChanged] callback is dispatched on [Dispatchers.Swing] so callers can
 * safely mutate Compose/UI state directly.
 */
@OptIn(ExperimentalUuidApi::class)
class BackupTaskManager(
    private val scope: CoroutineScope,
    private val onBackupCountChanged: (taskId: Uuid, count: Int) -> Unit,
) {
    private val activeJobs = mutableMapOf<Uuid, Job>()
    private val activeServices = mutableMapOf<Uuid, BackupService>()

    fun start(task: BackupTask) {
        if (activeJobs.containsKey(task.id)) return

        val service = BackupService(
            filePath = task.filePath,
            maxBackups = task.maxBackups,
            onBackupCountChanged = { count ->
                scope.launch(Dispatchers.Swing) {
                    onBackupCountChanged(task.id, count)
                }
            },
        )
        activeServices[task.id] = service

        val job = scope.launch(Dispatchers.IO) {
            service.start()
            service.pollLoop()
        }
        activeJobs[task.id] = job
    }

    fun stop(taskId: Uuid) {
        activeServices[taskId]?.stop()
        activeJobs[taskId]?.cancel()
        activeServices.remove(taskId)
        activeJobs.remove(taskId)
    }

    fun remove(taskId: Uuid) {
        stop(taskId)
    }

    /**
     * Restarts a running task with a new [maxBackups] limit.
     * If the task is not currently running this is a no-op.
     */
    fun restart(task: BackupTask) {
        if (!activeJobs.containsKey(task.id)) return
        stop(task.id)
        start(task)
    }

    fun stopAll() {
        activeServices.keys.toList().forEach { stop(it) }
    }
}
