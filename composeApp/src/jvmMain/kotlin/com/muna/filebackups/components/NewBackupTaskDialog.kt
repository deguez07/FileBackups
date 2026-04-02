package com.muna.filebackups.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import com.muna.filebackups.BackupTask
import com.muna.filebackups.utils.WindowAwareTooltipPositionProvider
import com.muna.filebackups.utils.validateMaxBackupsInput
import java.awt.FileDialog
import java.awt.Frame
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Dialog form for creating a new backup task.
 *
 * Presents a file picker (via native OS dialog), a validated "Max Backups" numeric
 * input (positive integer, < 100), and Cancel / Create Task action buttons.
 * The "Create Task" button stays disabled until both fields are valid, with a
 * tooltip explaining what is missing. Hovering the file input shows the full path
 * in a tooltip when the text overflows the field.
 *
 * @param onDismiss called when the dialog should close (Cancel or Create Task).
 * @param onCreateTask called with the newly created [BackupTask] when the form is submitted.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalUuidApi::class)
@Composable
fun NewBackupTaskDialog(onDismiss: () -> Unit, onCreateTask: (BackupTask) -> Unit) {
    var selectedFilePath by remember { mutableStateOf("") }
    var maxBackupsText by remember { mutableStateOf("") }
    var maxBackupsError by remember { mutableStateOf<String?>(null) }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "New Backup Task",
                    style = MaterialTheme.typography.headlineSmall,
                )

                Spacer(modifier = Modifier.height(8.dp))

                // --- File picker with overflow tooltip ---

                var fieldWidthPx by remember { mutableIntStateOf(0) }
                val textMeasurer = rememberTextMeasurer()
                val textStyle = MaterialTheme.typography.bodyLarge
                val density = LocalDensity.current
                // ~32dp accounts for OutlinedTextField internal horizontal padding
                val horizontalPaddingPx = with(density) { 32.dp.roundToPx() }

                // Reactively checks whether the file path text exceeds the visible field width
                val isFilePathOverflowing by remember {
                    derivedStateOf {
                        if (selectedFilePath.isEmpty() || fieldWidthPx <= 0) return@derivedStateOf false
                        val availableWidth = fieldWidthPx - horizontalPaddingPx
                        val measuredWidth = textMeasurer.measure(
                            text = selectedFilePath,
                            style = textStyle,
                            constraints = Constraints(maxWidth = Int.MAX_VALUE),
                        ).size.width
                        measuredWidth > availableWidth
                    }
                }

                var isHoveringFileField by remember { mutableStateOf(false) }
                val showFileTooltip = isHoveringFileField && isFilePathOverflowing

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = selectedFilePath,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("File") },
                            placeholder = { Text("No file selected") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onSizeChanged { size -> fieldWidthPx = size.width }
                                .pointerInput(Unit) {
                                    awaitPointerEventScope {
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            when (event.type) {
                                                PointerEventType.Enter -> isHoveringFileField = true
                                                PointerEventType.Exit -> isHoveringFileField = false
                                            }
                                        }
                                    }
                                },
                            singleLine = true,
                        )

                        // Custom tooltip popup shown on hover when the path overflows the field
                        if (showFileTooltip) {
                            Popup(
                                popupPositionProvider = WindowAwareTooltipPositionProvider(windowMargin = 4),
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.inverseSurface,
                                    modifier = Modifier
                                        .widthIn(max = 400.dp)
                                        .shadow(2.dp, RoundedCornerShape(4.dp)),
                                ) {
                                    Text(
                                        text = selectedFilePath,
                                        color = MaterialTheme.colorScheme.inverseOnSurface,
                                        style = MaterialTheme.typography.bodySmall,
                                        softWrap = true,
                                        modifier = Modifier.padding(8.dp),
                                    )
                                }
                            }
                        }
                    }
                    // Opens the native OS file dialog; updates selectedFilePath on selection
                    Button(onClick = {
                        val dialog = FileDialog(null as Frame?, "Select a file", FileDialog.LOAD)
                        dialog.isVisible = true
                        if (dialog.file != null) {
                            selectedFilePath = "${dialog.directory}${dialog.file}"
                        }
                    }) {
                        Text("Browse…")
                    }
                }

                // --- Max Backups input with inline validation errors ---

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

                // --- Form validity and Create Task tooltip ---

                val isFileSelected = selectedFilePath.isNotBlank()
                val isMaxBackupsValid = maxBackupsError == null && maxBackupsText.isNotBlank()
                val isFormValid = isFileSelected && isMaxBackupsValid

                // Context-specific message explaining why Create Task is disabled
                val tooltipText = when {
                    !isFileSelected && !isMaxBackupsValid -> "Select a file and enter a valid Max Backups value"
                    !isFileSelected -> "Select a file to back up"
                    !isMaxBackupsValid -> "Enter a valid Max Backups value (1–99)"
                    else -> ""
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
                            if (!isFormValid) {
                                PlainTooltip(modifier = Modifier.widthIn(max = 200.dp)) {
                                    Text(tooltipText, softWrap = true)
                                }
                            }
                        },
                        state = rememberTooltipState(),
                    ) {
                        Button(
                            onClick = {
                                val task = BackupTask(
                                    id = Uuid.random(),
                                    filePath = selectedFilePath.trim(),
                                    maxBackups = maxBackupsText.trim().toInt(),
                                )
                                onCreateTask(task)
                                onDismiss()
                            },
                            enabled = isFormValid,
                        ) {
                            Text("Create Task")
                        }
                    }
                }
            }
        }
    }
}
