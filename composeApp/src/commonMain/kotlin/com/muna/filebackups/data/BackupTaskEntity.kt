package com.muna.filebackups.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persisted row for a backup task (mirrors the desktop `BackupTask` model).
 * Snapshot count is **not** stored; it is read from the file’s `backups/` folder when mapping to UI.
 */
@Entity(tableName = "backup_tasks")
data class BackupTaskEntity(
    @PrimaryKey
    val id: String,
    val filePath: String,
    val maxBackups: Int,
    val isRunning: Boolean,
)
