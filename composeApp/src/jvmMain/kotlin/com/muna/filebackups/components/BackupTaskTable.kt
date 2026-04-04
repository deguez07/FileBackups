package com.muna.filebackups.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.muna.filebackups.BackupTask
import kotlin.uuid.ExperimentalUuidApi

/**
 * A tabular representation of [BackupTask] items.
 *
 * Displays task details including file name, path, backup limits, and operational state.
 * Provides interaction points to toggle task status, edit, or delete individual tasks.
 *
 * @param tasks The list of backup tasks to display.
 * @param onToggleRunning Callback triggered when the task's running state is toggled (Running/Paused).
 * @param onEdit Callback triggered to initiate editing of a specific task.
 * @param onDelete Callback triggered to initiate deletion of a specific task.
 */
@OptIn(ExperimentalUuidApi::class)
@Composable
fun BackupTaskTable(
    tasks: List<BackupTask>,
    onToggleRunning: (BackupTask) -> Unit,
    onEdit: (BackupTask) -> Unit,
    onDelete: (BackupTask) -> Unit,
) {
    if (tasks.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "No backup tasks yet. Use Actions → New backup task to create one.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        HeaderRow()
        HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.outlineVariant)

        LazyColumn(modifier = Modifier.weight(1f)) {
            itemsIndexed(tasks, key = { _, task -> task.id.toString() }) { index, task ->
                TaskRow(
                    task = task,
                    onToggleRunning = { onToggleRunning(task) },
                    onEdit = { onEdit(task) },
                    onDelete = { onDelete(task) },
                )
                if (index < tasks.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

private val columnWeights = floatArrayOf(0.15f, 0.30f, 0.10f, 0.13f, 0.12f, 0.12f)

private val columnGap = 8.dp

private const val pathEllipsis = "..."

/**
 * When [path] is wider than [maxWidthPx], returns an ellipsis prefix plus the longest trailing
 * suffix that fits; otherwise returns [path] unchanged.
 */
private fun ellipsizePathKeepTail(
    path: String,
    maxWidthPx: Int,
    style: TextStyle,
    measurer: TextMeasurer,
): String {
    if (path.isEmpty() || maxWidthPx <= 0) return path
    val unbounded = Constraints(maxWidth = Int.MAX_VALUE)
    fun measureWidth(text: String) = measurer.measure(
        text = text,
        style = style,
        softWrap = false,
        maxLines = 1,
        constraints = unbounded,
    ).size.width

    if (measureWidth(path) <= maxWidthPx) return path

    if (measureWidth(pathEllipsis) > maxWidthPx) return pathEllipsis

    var low = 1
    var high = path.length
    var best = pathEllipsis
    while (low <= high) {
        val mid = (low + high + 1) / 2
        val candidate = pathEllipsis + path.takeLast(mid)
        if (measureWidth(candidate) <= maxWidthPx) {
            best = candidate
            low = mid + 1
        } else {
            high = mid - 1
        }
    }
    return best
}

@Composable
private fun HeaderRow() {
    val headers = listOf("File Name", "File Path", "Max Backups", "Current Backups", "State", "Actions")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(columnGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        headers.forEachIndexed { i, header ->
            Text(
                text = header,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.weight(columnWeights[i]),
            )
        }
    }
}

@Composable
private fun TaskRow(
    task: BackupTask,
    onToggleRunning: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(columnGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = task.fileName,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(columnWeights[0]),
        )
        BoxWithConstraints(modifier = Modifier.weight(columnWeights[1])) {
            val textMeasurer = rememberTextMeasurer()
            val pathStyle = MaterialTheme.typography.bodyMedium
            val maxW = constraints.maxWidth
            val pathText = remember(task.filePath, maxW, pathStyle) {
                ellipsizePathKeepTail(task.filePath, maxW, pathStyle, textMeasurer)
            }
            Text(
                text = pathText,
                style = pathStyle,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Text(
            text = task.maxBackups.toString(),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(columnWeights[2]),
        )
        Text(
            text = task.currentBackups.toString(),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(columnWeights[3]),
        )

        Box(modifier = Modifier.weight(columnWeights[4])) {
            val stateColor = if (task.isRunning)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.error

            TextButton(onClick = onToggleRunning) {
                Text(
                    text = if (task.isRunning) "Running" else "Paused",
                    color = stateColor,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                )
            }
        }

        Row(
            modifier = Modifier.weight(columnWeights[5]),
            horizontalArrangement = Arrangement.Start,
        ) {
            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
