package com.muna.filebackups.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * CRUD and reactive reads for [BackupTaskEntity]. The app observes [observeAll] for the table UI;
 * [getRunningTasks] is used on startup to resume watchers.
 */
@Dao
interface BackupTaskDao {
    @Query("SELECT * FROM backup_tasks")
    fun observeAll(): Flow<List<BackupTaskEntity>>

    @Query("SELECT * FROM backup_tasks WHERE isRunning = 1")
    suspend fun getRunningTasks(): List<BackupTaskEntity>

    @Insert
    suspend fun insert(task: BackupTaskEntity)

    @Update
    suspend fun update(task: BackupTaskEntity)

    @Query("DELETE FROM backup_tasks WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE backup_tasks SET isRunning = :isRunning WHERE id = :id")
    suspend fun updateRunningState(id: String, isRunning: Boolean)

    @Query("UPDATE backup_tasks SET maxBackups = :maxBackups WHERE id = :id")
    suspend fun updateMaxBackups(id: String, maxBackups: Int)
}
