package com.muna.filebackups.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.muna.filebackups.BackupTask
import com.muna.filebackups.utils.WindowAwareTooltipPositionProvider
import com.muna.filebackups.utils.validateMaxBackupsInput

/**
 * Dialog for editing [BackupTask.maxBackups]. Shows the file name and full path as static text.
 *
 * @param onDismiss invoked for Cancel or window close without saving.
 * @param onUpdate invoked with the new max backups count when the user confirms.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditBackupTaskDialog(
    task: BackupTask,
    onDismiss: () -> Unit,
    onUpdate: (maxBackups: Int) -> Unit,
) {
    var maxBackupsText by remember(task) { mutableStateOf(task.maxBackups.toString()) }
    var maxBackupsError by remember(task) { mutableStateOf<String?>(null) }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Max backups",
                    style = MaterialTheme.typography.headlineSmall,
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = task.fileName,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = task.filePath,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                OutlinedTextField(
                    value = maxBackupsText,
                    onValueChange = { newValue ->
                        maxBackupsText = newValue
                        maxBackupsError = validateMaxBackupsInput(newValue)
                    },
                    label = { Text("Max Backups") },
                    isError = maxBackupsError != null,
                    supportingText = maxBackupsError?.let { error -> { Text(error) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.weight(1f))

                val isMaxBackupsValid = maxBackupsError == null && maxBackupsText.isNotBlank()
                val tooltipText = if (!isMaxBackupsValid) {
                    "Enter a valid Max Backups value (1–99)"
                } else {
                    ""
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TooltipBox(
                        positionProvider = WindowAwareTooltipPositionProvider(windowMargin = 4),
                        tooltip = {
                            if (!isMaxBackupsValid) {
                                PlainTooltip(modifier = Modifier.widthIn(max = 200.dp)) {
                                    Text(tooltipText, softWrap = true)
                                }
                            }
                        },
                        state = rememberTooltipState(),
                    ) {
                        Button(
                            onClick = {
                                onUpdate(maxBackupsText.trim().toInt())
                                onDismiss()
                            },
                            enabled = isMaxBackupsValid,
                        ) {
                            Text("Update")
                        }
                    }
                }
            }
        }
    }
}
