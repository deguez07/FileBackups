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
