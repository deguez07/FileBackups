package com.muna.filebackups.data

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

/**
 * Room database for persisted backup tasks. Desktop builds the instance in
 * `jvmMain` (`createDatabase()` + bundled SQLite driver).
 */
@Database(entities = [BackupTaskEntity::class], version = 1)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun backupTaskDao(): BackupTaskDao
}

/** KSP/Room supplies the JVM `actual` implementation for this constructor. */
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}
