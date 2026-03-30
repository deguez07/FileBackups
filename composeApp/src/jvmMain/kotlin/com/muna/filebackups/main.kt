package com.muna.filebackups

import androidx.compose.runtime.getValue
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

fun main() = application {
    var showNewBackupTaskDialog by remember { mutableStateOf(false) }

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
        App()
    }

    if (showNewBackupTaskDialog) {
        Window(
            onCloseRequest = { showNewBackupTaskDialog = false },
            title = "New Backup Task",
            state = WindowState(size = DpSize(700.dp, 400.dp)),
            resizable = false,
        ) {
            NewBackupTaskDialog(onDismiss = { showNewBackupTaskDialog = false })
        }
    }
}