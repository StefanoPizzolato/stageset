package com.codex.stageset.data.local

import androidx.room.migration.Migration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        SongEntity::class,
        SetlistEntity::class,
        SetlistSongEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class StageSetDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun setlistDao(): SetlistDao

    companion object {
        val Migration1To2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    ALTER TABLE songs
                    ADD COLUMN preset TEXT NOT NULL DEFAULT ''
                    """.trimIndent(),
                )
            }
        }
    }
}
