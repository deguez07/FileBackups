package com.muna.filebackups

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.muna.filebackups.components.BackupTaskTable

@Composable
fun App(
    tasks: List<BackupTask>,
    onToggleRunning: (BackupTask) -> Unit,
    onEdit: (BackupTask) -> Unit,
    onDelete: (BackupTask) -> Unit,
) {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            BackupTaskTable(
                tasks = tasks,
                onToggleRunning = onToggleRunning,
                onEdit = onEdit,
                onDelete = onDelete,
            )
        }
    }
}
