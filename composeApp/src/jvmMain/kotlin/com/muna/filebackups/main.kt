package com.muna.filebackups

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "filebackups",
    ) {
        MenuBar {
            Menu("Actions") {
                Item(
                    text = "New backup task",
                    shortcut = KeyShortcut(Key.N, meta = System.getProperty("os.name").contains("Mac", ignoreCase = true), ctrl = !System.getProperty("os.name").contains("Mac", ignoreCase = true)),
                    onClick = { println("New backup task initiated") },
                )
            }
        }
        App()
    }
}