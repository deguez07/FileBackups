package com.muna.filebackups.data

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import java.io.File

/**
 * Opens the app SQLite file at `~/.filebackups/filebackups.db` with the bundled driver and IO dispatcher for queries.
 */
fun createDatabase(): AppDatabase {
    val dbDir = File(System.getProperty("user.home"), ".filebackups")
    dbDir.mkdirs()
    val dbFile = File(dbDir, "filebackups.db")
    return Room.databaseBuilder<AppDatabase>(name = dbFile.absolutePath)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}
