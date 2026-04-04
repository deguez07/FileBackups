package com.muna.filebackups

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import androidx.compose.runtime.collectAsState
import com.muna.filebackups.components.EditBackupTaskDialog
import com.muna.filebackups.components.NewBackupTaskDialog
import com.muna.filebackups.data.BackupTaskRepository
import com.muna.filebackups.data.createDatabase
import com.muna.filebackups.service.BackupTaskManager
import com.muna.filebackups.utils.showBackupTaskStateToggleConfirmation
import com.muna.filebackups.utils.showDeleteBackupTaskConfirmation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Desktop entry: loads tasks from Room (`~/.filebackups/filebackups.db`), shows them in Compose, and
 * persists create / run state / max backups / delete. Tasks marked running in the DB are started
 * after launch if the source file still exists; otherwise they are saved as paused. While services
 * run, snapshot counts from the manager are merged over the DB-backed list for live UI updates.
 */
@OptIn(ExperimentalUuidApi::class)
fun main() {
    val database = createDatabase()
    val repository = BackupTaskRepository(database.backupTaskDao())
    val managerScope = CoroutineScope(SupervisorJob())

    val backupCounts = mutableStateMapOf<Uuid, Int>()

    val backupTaskManager = BackupTaskManager(
        scope = managerScope,
        onBackupCountChanged = { taskId, count ->
            backupCounts[taskId] = count
        },
    )

    managerScope.launch(Dispatchers.IO) {
        val previouslyRunning = repository.getRunningTasks()
        for (task in previouslyRunning) {
            if (java.io.File(task.filePath).isFile) {
                backupTaskManager.start(task)
            } else {
                repository.updateRunningState(task.id, false)
            }
        }
    }

    application {
        var showNewBackupTaskDialog by remember { mutableStateOf(false) }
        var newTaskInitialFilePath by remember { mutableStateOf("") }
        var editingTask by remember { mutableStateOf<BackupTask?>(null) }
        val dbTasks by repository.observeAll().collectAsState(initial = emptyList())
        val uiScope = rememberCoroutineScope()

        val tasks by remember {
            derivedStateOf {
                dbTasks.map { task ->
                    val liveCount = backupCounts[task.id]
                    if (liveCount != null) task.copy(currentBackups = liveCount) else task
                }
            }
        }

        DisposableEffect(Unit) {
            onDispose {
                backupTaskManager.stopAll()
                managerScope.cancel()
                database.close()
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
                        onClick = {
                            newTaskInitialFilePath = ""
                            showNewBackupTaskDialog = true
                        },
                    )
                }
            }
            App(
                tasks = tasks,
                onFilesDropped = { path ->
                    newTaskInitialFilePath = path
                    showNewBackupTaskDialog = true
                },
                onToggleRunning = { task ->
                    val starting = !task.isRunning
                    if (showBackupTaskStateToggleConfirmation(task.filePath, starting)) {
                        uiScope.launch {
                            repository.updateRunningState(task.id, starting)
                            if (starting) {
                                backupTaskManager.start(task.copy(isRunning = true))
                            } else {
                                backupTaskManager.stop(task.id)
                            }
                        }
                    }
                },
                onEdit = { task -> editingTask = task },
                onDelete = { task ->
                    if (showDeleteBackupTaskConfirmation(task.filePath)) {
                        backupTaskManager.remove(task.id)
                        uiScope.launch {
                            repository.delete(task.id)
                        }
                    }
                },
            )
        }

        if (showNewBackupTaskDialog) {
            Window(
                onCloseRequest = {
                    showNewBackupTaskDialog = false
                    newTaskInitialFilePath = ""
                },
                title = "New Backup Task",
                state = WindowState(size = DpSize(900.dp, 500.dp)),
                resizable = false,
            ) {
                NewBackupTaskDialog(
                    initialSelectedFilePath = newTaskInitialFilePath,
                    onDismiss = {
                        showNewBackupTaskDialog = false
                        newTaskInitialFilePath = ""
                    },
                    onCreateTask = { task ->
                        uiScope.launch {
                            repository.insert(task)
                        }
                    },
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
                        uiScope.launch {
                            repository.updateMaxBackups(task.id, maxBackups)
                            if (task.isRunning) {
                                backupTaskManager.restart(task.copy(maxBackups = maxBackups))
                            }
                        }
                    },
                )
            }
        }
    }
}
