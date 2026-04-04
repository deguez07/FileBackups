package com.muna.filebackups

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import com.muna.filebackups.components.EditBackupTaskDialog
import com.muna.filebackups.components.NewBackupTaskDialog
import com.muna.filebackups.service.BackupTaskManager
import com.muna.filebackups.utils.showBackupTaskStateToggleConfirmation
import com.muna.filebackups.utils.showDeleteBackupTaskConfirmation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
fun main() = application {
    var showNewBackupTaskDialog by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<BackupTask?>(null) }
    val tasks = remember { mutableStateListOf<BackupTask>() }

    val managerScope = remember { CoroutineScope(SupervisorJob()) }
    val backupTaskManager = remember {
        BackupTaskManager(
            scope = managerScope,
            onBackupCountChanged = { taskId, count ->
                val index = tasks.indexOfFirst { it.id == taskId }
                if (index >= 0) {
                    tasks[index] = tasks[index].copy(currentBackups = count)
                }
            },
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            backupTaskManager.stopAll()
            managerScope.cancel()
        }
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "filebackups",
    ) {
        MenuBar {
            Menu("Actions") {
                Item(
                    text = "New backup task",
                    shortcut = KeyShortcut(Key.N, meta = System.getProperty("os.name").contains("Mac", ignoreCase = true), ctrl = !System.getProperty("os.name").contains("Mac", ignoreCase = true)),
                    onClick = { showNewBackupTaskDialog = true },
                )
            }
        }
        App(
            tasks = tasks,
            onToggleRunning = { task ->
                val starting = !task.isRunning
                if (showBackupTaskStateToggleConfirmation(task.filePath, starting)) {
                    val index = tasks.indexOfFirst { it.id == task.id }
                    if (index >= 0) {
                        val updated = tasks[index].copy(isRunning = starting)
                        tasks[index] = updated
                        if (starting) {
                            backupTaskManager.start(updated)
                        } else {
                            backupTaskManager.stop(updated.id)
                        }
                    }
                }
            },
            onEdit = { task -> editingTask = task },
            onDelete = { task ->
                if (showDeleteBackupTaskConfirmation(task.filePath)) {
                    backupTaskManager.remove(task.id)
                    val index = tasks.indexOfFirst { it.id == task.id }
                    if (index >= 0) {
                        tasks.removeAt(index)
                    }
                }
            },
        )
    }

    if (showNewBackupTaskDialog) {
        Window(
            onCloseRequest = { showNewBackupTaskDialog = false },
            title = "New Backup Task",
            state = WindowState(size = DpSize(900.dp, 500.dp)),
            resizable = false,
        ) {
            NewBackupTaskDialog(
                onDismiss = { showNewBackupTaskDialog = false },
                onCreateTask = { task -> tasks.add(task) },
            )
        }
    }

    editingTask?.let { task ->
        Window(
            onCloseRequest = { editingTask = null },
            title = "Edit backup task — ${task.fileName}",
            state = WindowState(size = DpSize(520.dp, 400.dp)),
            resizable = false,
        ) {
            EditBackupTaskDialog(
                task = task,
                onDismiss = { editingTask = null },
                onUpdate = { maxBackups ->
                    val index = tasks.indexOfFirst { it.id == task.id }
                    if (index >= 0) {
                        val updated = tasks[index].copy(maxBackups = maxBackups)
                        tasks[index] = updated
                        if (updated.isRunning) {
                            backupTaskManager.restart(updated)
                        }
                    }
                },
            )
        }
    }
}
