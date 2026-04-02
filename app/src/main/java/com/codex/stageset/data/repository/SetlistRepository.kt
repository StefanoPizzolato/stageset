package com.codex.stageset.data.repository

import androidx.room.withTransaction
import com.codex.stageset.data.local.SetlistEntity
import com.codex.stageset.data.local.SetlistSongEntity
import com.codex.stageset.data.local.StageSetDatabase
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
}
