package com.muna.filebackups.utils

import javax.swing.JOptionPane

private fun String.htmlEscape(): String =
    replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")

/**
 * Shows a native OS-styled confirmation dialog (Swing [JOptionPane]).
 *
 * @return `true` if the user chose **Delete**, `false` for **Cancel** or closing the dialog.
 */
fun showDeleteBackupTaskConfirmation(filePath: String): Boolean {
    val title = "Are you sure you want to delete this backup task?"
    val safePath = filePath.htmlEscape()
    val message =
        "<html><body style='width: 420px;'>The task to create backups of <i>$safePath</i> will be removed</body></html>"
    val options = arrayOf("Cancel", "Delete")
    val result = JOptionPane.showOptionDialog(
        null,
        message,
        title,
        JOptionPane.DEFAULT_OPTION,
        JOptionPane.WARNING_MESSAGE,
        null,
        options,
        options[0],
    )
    return result == 1
}

/**
 * Native confirmation before switching a backup task between paused and running.
 *
 * @param filePath full path (shown in italics in the message, HTML-escaped)
 * @param starting `true` when going from paused to running, `false` when pausing
 * @return `true` if the user confirmed **Start** or **Pause**, `false` for **Cancel** or close
 */
fun showBackupTaskStateToggleConfirmation(filePath: String, starting: Boolean): Boolean {
    val safePath = filePath.htmlEscape()
    val title: String
    val message: String
    val confirmLabel: String
    if (starting) {
        title = "Start this backup task?"
        message =
            "<html><body style='width: 420px;'>Backups for <i>$safePath</i> will run while the task is started.</body></html>"
        confirmLabel = "Start"
    } else {
        title = "Pause this backup task?"
        message =
            "<html><body style='width: 420px;'>Backups for <i>$safePath</i> will not run until you start the task again.</body></html>"
        confirmLabel = "Pause"
    }
    val options = arrayOf("Cancel", confirmLabel)
    val result = JOptionPane.showOptionDialog(
        null,
        message,
        title,
        JOptionPane.DEFAULT_OPTION,
        JOptionPane.QUESTION_MESSAGE,
        null,
        options,
        options[0],
    )
    return result == 1
}
