package com.muna.filebackups

import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragData
import androidx.compose.ui.draganddrop.dragData
import com.muna.filebackups.components.BackupTaskTable
import java.io.File
import java.net.URI

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun App(
    tasks: List<BackupTask>,
    onToggleRunning: (BackupTask) -> Unit,
    onEdit: (BackupTask) -> Unit,
    onDelete: (BackupTask) -> Unit,
    onFilesDropped: (String) -> Unit,
) {
    val onFilesDroppedState by rememberUpdatedState(onFilesDropped)
    val dropTarget = remember {
        object : DragAndDropTarget {
            override fun onDrop(event: DragAndDropEvent): Boolean {
                val filesList = event.dragData() as? DragData.FilesList ?: return false
                val path = filesList.readFiles().firstNotNullOfOrNull { uriString ->
                    runCatching {
                        val file = File(URI(uriString))
                        file.takeIf { it.isFile }?.absolutePath
                    }.getOrNull()
                } ?: return false
                onFilesDroppedState(path)
                return true
            }
        }
    }

    MaterialTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .dragAndDropTarget(
                    shouldStartDragAndDrop = { startEvent ->
                        startEvent.dragData() is DragData.FilesList
                    },
                    target = dropTarget,
                ),
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
