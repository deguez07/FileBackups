package com.muna.filebackups.utils

/**
 * Validates the Max Backups text field (trimmed).
 * @return an error message if invalid, or null if valid (1–99 inclusive).
 */
fun validateMaxBackupsInput(text: String): String? {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return "Max Backups is required"
    val number = trimmed.toIntOrNull()
    if (number == null) return "Must be a valid integer"
    if (number < 1) return "Must be a positive integer"
    if (number >= 100) return "Must be less than 100"
    return null
}
