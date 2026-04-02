package com.codex.stageset.data.repository

import androidx.room.withTransaction
import com.codex.stageset.data.local.SetlistEntity
import com.codex.stageset.data.local.SetlistSongEntity
import com.codex.stageset.data.local.StageSetDatabase
import com.codex.stageset.data.local.SongEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class SetlistRepository(
    private val database: StageSetDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val dao = database.setlistDao()
    private val songDao = database.songDao()

    fun observeSetlists(): Flow<List<SetlistSummary>> = dao.observeSetlistSummaries().map { rows ->
        rows.map { row ->
            SetlistSummary(
                id = row.id,
                name = row.name,
                notes = row.notes,
                createdAt = row.createdAt,
                songCount = row.songCount,
            )
        }
    }

    fun observeSetlist(setlistId: Long): Flow<SetlistDetail?> = combine(
        dao.observeSetlist(setlistId),
        dao.observeSetlistSongs(setlistId),
    ) { setlist, songs ->
        setlist?.let {
            SetlistDetail(
                id = it.id,
                name = it.name,
                notes = it.notes,
                createdAt = it.createdAt,
                songs = songs.map { row ->
                    SetlistSong(
                        entryId = row.entryId,
                        songId = row.songId,
                        position = row.position,
                        name = row.name,
                        artist = row.artist,
                        keySignature = row.keySignature,
                    )
                },
            )
        }
    }

    fun observeSetlistPreview(setlistId: Long): Flow<SetlistPreviewDetail?> = combine(
        dao.observeSetlist(setlistId),
        dao.observeSetlistPreviewSongs(setlistId),
    ) { setlist, songs ->
        setlist?.let {
            SetlistPreviewDetail(
                id = it.id,
                name = it.name,
                notes = it.notes,
                createdAt = it.createdAt,
                songs = songs.map { row ->
                    SetlistPreviewSong(
                        entryId = row.entryId,
                        songId = row.songId,
                        position = row.position,
                        name = row.name,
                        artist = row.artist,
                        preset = row.preset,
                        keySignature = row.keySignature,
                        chart = row.chart,
                    )
                },
            )
        }
    }

    suspend fun saveSetlist(setlistId: Long?, draft: SetlistDraft): Long = withContext(ioDispatcher) {
        database.withTransaction {
            val targetId = if (setlistId == null) {
                dao.insertSetlist(
                    SetlistEntity(
                        name = draft.name.trim(),
                        notes = draft.notes.trim(),
                        createdAt = System.currentTimeMillis(),
                    ),
                )
            } else {
                val existingSetlist = dao.getSetlist(setlistId)
                dao.updateSetlist(
                    SetlistEntity(
                        id = setlistId,
                        name = draft.name.trim(),
                        notes = draft.notes.trim(),
                        createdAt = existingSetlist?.createdAt ?: System.currentTimeMillis(),
                    ),
                )
                setlistId
            }

            dao.deleteSongsForSetlist(targetId)
            if (draft.songIds.isNotEmpty()) {
                dao.insertSetlistSongs(
                    draft.songIds.mapIndexed { index, songId ->
                        SetlistSongEntity(
                            setlistId = targetId,
                            songId = songId,
                            position = index,
                        )
                    },
                )
            }
            targetId
        }
    }

    suspend fun deleteSetlist(setlistId: Long) = withContext(ioDispatcher) {
        dao.deleteSetlist(setlistId)
    }

    suspend fun exportSetlistArchive(setlistId: Long): Result<SetlistArchiveDocument> = withContext(ioDispatcher) {
        runCatching {
            val setlist = dao.getSetlist(setlistId)
                ?: throw IllegalArgumentException("That setlist could not be found.")
            val songs = dao.getSetlistPreviewSongs(setlistId)
            require(songs.isNotEmpty()) {
                "Add at least one song before saving an archive."
            }

            val refsBySongId = linkedMapOf<Long, String>()
            val archiveSongs = songs.distinctBy { it.songId }.mapIndexed { index, song ->
                val ref = "song-${index + 1}"
                refsBySongId[song.songId] = ref
                SetlistArchiveSong(
                    ref = ref,
                    name = song.name,
                    artist = song.artist,
                    preset = song.preset,
                    keySignature = song.keySignature,
                    chart = song.chart,
                )
            }

            val payload = SetlistArchivePayload(
                version = 1,
                exportedAt = System.currentTimeMillis(),
                setlist = SetlistArchiveSetlist(
                    name = setlist.name,
                    notes = setlist.notes,
                    createdAt = setlist.createdAt,
                ),
                songs = archiveSongs,
                entries = songs.map { song ->
                    SetlistArchiveEntry(
                        songRef = refsBySongId.getValue(song.songId),
                        position = song.position,
                    )
                },
            )

            SetlistArchiveDocument(
                suggestedFileName = SetlistArchiveCodec.suggestedFileName(setlist.name),
                json = SetlistArchiveCodec.encode(payload),
            )
        }
    }

    suspend fun importSetlistArchive(json: String): Result<SetlistArchiveImportResult> = withContext(ioDispatcher) {
        runCatching {
            val payload = SetlistArchiveCodec.decode(json)

            database.withTransaction {
                var importedSongs = 0
                var reusedSongs = 0

                val songIdsByRef = payload.songs.associate { archiveSong ->
                    val matchingSongId = songDao.findMatchingSongId(
                        name = archiveSong.name,
                        artist = archiveSong.artist,
                        preset = archiveSong.preset,
                        keySignature = archiveSong.keySignature,
                        chart = archiveSong.chart,
                    )
                    val songId = if (matchingSongId != null) {
                        reusedSongs++
                        matchingSongId
                    } else {
                        importedSongs++
                        songDao.insertSong(
                            SongEntity(
                                name = archiveSong.name,
                                artist = archiveSong.artist,
                                preset = archiveSong.preset,
                                keySignature = archiveSong.keySignature,
                                chart = archiveSong.chart,
                                lastModified = System.currentTimeMillis(),
                            ),
                        )
                    }
                    archiveSong.ref to songId
                }

                val resolvedSetlistName = payload.setlist.name.ifBlank { "Imported Setlist" }
                val setlistId = dao.insertSetlist(
                    SetlistEntity(
                        name = resolvedSetlistName,
                        notes = payload.setlist.notes,
                        createdAt = System.currentTimeMillis(),
                    ),
                )

                dao.insertSetlistSongs(
                    payload.entries.sortedBy { it.position }.mapIndexed { index, entry ->
                        SetlistSongEntity(
                            setlistId = setlistId,
                            songId = songIdsByRef[entry.songRef]
                                ?: throw IllegalArgumentException("Archive entry references a missing song."),
                            position = index,
                        )
                    },
                )

                SetlistArchiveImportResult(
                    setlistId = setlistId,
                    setlistName = resolvedSetlistName,
                    songCount = payload.entries.size,
                    importedSongs = importedSongs,
                    reusedSongs = reusedSongs,
                )
            }
        }
    }
}
