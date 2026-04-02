package com.codex.stageset

import android.content.Context
import androidx.room.Room
import com.codex.stageset.data.local.StageSetDatabase
import com.codex.stageset.data.remote.UltimateGuitarImporter
import com.codex.stageset.data.repository.PreviewSettingsRepository
import com.codex.stageset.data.repository.SetlistRepository
import com.codex.stageset.data.repository.SongRepository

class AppContainer(context: Context) {
    private val database = Room.databaseBuilder(
        context,
        StageSetDatabase::class.java,
        "stage-set.db",
    ).addMigrations(
        StageSetDatabase.Migration1To2,
    ).build()

    private val ultimateGuitarImporter = UltimateGuitarImporter()

    val previewSettingsRepository = PreviewSettingsRepository(context.applicationContext)

    val songRepository = SongRepository(
        songDao = database.songDao(),
        importer = ultimateGuitarImporter,
    )

    val setlistRepository = SetlistRepository(database)
}
