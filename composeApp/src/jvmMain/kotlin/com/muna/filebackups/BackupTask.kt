package com.muna.filebackups

import java.io.File
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Represents a task to back up a specific file.
 *
 * Contains configuration and state for backup operations, including the source file path,
 * backup limits, and current operational status.
 */
data class BackupTask @OptIn(ExperimentalUuidApi::class) constructor(
    /** The id of the backup task. */
    val id: Uuid,

    /** The path to the file to be backed up. */
    val filePath: String,

    /** The maximum number of backups to keep. */
    val maxBackups: Int,

    /** Whether the backup task is currently running. */
    val isRunning: Boolean = false,

    /** The current number of backups for this task. */
    val currentBackups: Int = 0,
) {
    val fileName: String get() = File(filePath).name
}
