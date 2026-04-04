package com.muna.filebackups.data

import com.muna.filebackups.BackupTask
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Maps Room entities to [BackupTask], filling [BackupTask.currentBackups] from the `backups/` folder
 * next to each file. All UI mutations should go through these methods so SQLite stays in sync.
 */
@OptIn(ExperimentalUuidApi::class)
class BackupTaskRepository(private val dao: BackupTaskDao) {

    fun observeAll(): Flow<List<BackupTask>> = dao.observeAll().map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun getRunningTasks(): List<BackupTask> =
        dao.getRunningTasks().map { it.toDomain() }

    suspend fun insert(task: BackupTask) {
        dao.insert(task.toEntity())
    }

    suspend fun updateRunningState(taskId: Uuid, isRunning: Boolean) {
        dao.updateRunningState(taskId.toString(), isRunning)
    }

    suspend fun updateMaxBackups(taskId: Uuid, maxBackups: Int) {
        dao.updateMaxBackups(taskId.toString(), maxBackups)
    }

    suspend fun delete(taskId: Uuid) {
        dao.deleteById(taskId.toString())
    }
}

@OptIn(ExperimentalUuidApi::class)
private fun BackupTaskEntity.toDomain(): BackupTask = BackupTask(
    id = Uuid.parse(id),
    filePath = filePath,
    maxBackups = maxBackups,
    isRunning = isRunning,
    currentBackups = countSnapshotDirs(filePath),
)

@OptIn(ExperimentalUuidApi::class)
private fun BackupTask.toEntity(): BackupTaskEntity = BackupTaskEntity(
    id = id.toString(),
    filePath = filePath,
    maxBackups = maxBackups,
    isRunning = isRunning,
)

/** Counts timestamped backup directories (excludes `current/`), same rules as `BackupService`. */
private fun countSnapshotDirs(filePath: String): Int {
    val backupsRoot = File(filePath).parentFile?.resolve("backups") ?: return 0
    return backupsRoot.listFiles()
        ?.count { it.isDirectory && it.name != "current" }
        ?: 0
}
