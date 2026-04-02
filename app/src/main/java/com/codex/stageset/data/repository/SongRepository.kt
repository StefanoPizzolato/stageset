package com.codex.stageset.data.repository

import com.codex.stageset.data.local.SongDao
import com.codex.stageset.data.local.SongEntity
import com.codex.stageset.data.remote.UltimateGuitarImporter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class SongRepository(
    private val songDao: SongDao,
    private val importer: UltimateGuitarImporter,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    fun observeSongSummaries(): Flow<List<SongSummary>> = songDao.observeSongSummaries().map { songs ->
        songs.map { row ->
            SongSummary(
                id = row.id,
                name = row.name,
                artist = row.artist,
                preset = row.preset,
                keySignature = row.keySignature,
                lineCount = row.lineCount,
            )
        }
    }

    fun observeSongs(): Flow<List<Song>> = songDao.observeSongs().map { songs ->
        songs.map { it.toModel() }
    }

    fun observeSong(songId: Long): Flow<Song?> = songDao.observeSong(songId).map { it?.toModel() }

    suspend fun saveSong(songId: Long?, draft: SongDraft): Long = withContext(ioDispatcher) {
        val entity = SongEntity(
            id = songId ?: 0,
            name = draft.name.trim(),
            artist = draft.artist.trim(),
            preset = draft.preset.trim(),
            keySignature = draft.keySignature.trim(),
            chart = draft.chart.trimEnd(),
            lastModified = System.currentTimeMillis(),
        )
        if (songId == null) {
            songDao.insertSong(entity)
        } else {
            songDao.updateSong(entity)
            songId
        }
    }

    suspend fun deleteSong(songId: Long) = withContext(ioDispatcher) {
        songDao.deleteSong(songId)
    }

    suspend fun importFromUltimateGuitar(url: String): Result<ImportedSongDraft> = withContext(ioDispatcher) {
        runCatching {
            importer.import(url)
        }
    }

    private fun SongEntity.toModel() = Song(
        id = id,
        name = name,
        artist = artist,
        preset = preset,
        keySignature = keySignature,
        chart = chart,
        lastModified = lastModified,
    )
}
