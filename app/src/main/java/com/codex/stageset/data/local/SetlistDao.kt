package com.codex.stageset.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SetlistDao {
    @Query(
        """
        SELECT setlists.id, setlists.name, setlists.notes, setlists.createdAt, COUNT(setlist_songs.id) AS songCount
        FROM setlists
        LEFT JOIN setlist_songs ON setlist_songs.setlistId = setlists.id
        GROUP BY setlists.id
        ORDER BY setlists.name COLLATE NOCASE ASC
        """,
    )
    fun observeSetlistSummaries(): Flow<List<SetlistSummaryRow>>

    @Query("SELECT * FROM setlists WHERE id = :setlistId")
    fun observeSetlist(setlistId: Long): Flow<SetlistEntity?>

    @Query("SELECT * FROM setlists WHERE id = :setlistId")
    suspend fun getSetlist(setlistId: Long): SetlistEntity?

    @Query(
        """
        SELECT
            setlist_songs.id AS entryId,
            setlist_songs.songId AS songId,
            setlist_songs.position AS position,
            songs.name AS name,
            songs.artist AS artist,
            songs.keySignature AS keySignature
        FROM setlist_songs
        INNER JOIN songs ON songs.id = setlist_songs.songId
        WHERE setlist_songs.setlistId = :setlistId
        ORDER BY setlist_songs.position ASC, setlist_songs.id ASC
        """,
    )
    fun observeSetlistSongs(setlistId: Long): Flow<List<SetlistSongRow>>

    @Query(
        """
        SELECT
            setlist_songs.id AS entryId,
            setlist_songs.songId AS songId,
            setlist_songs.position AS position,
            songs.name AS name,
            songs.artist AS artist,
            songs.preset AS preset,
            songs.keySignature AS keySignature,
            songs.chart AS chart
        FROM setlist_songs
        INNER JOIN songs ON songs.id = setlist_songs.songId
        WHERE setlist_songs.setlistId = :setlistId
        ORDER BY setlist_songs.position ASC, setlist_songs.id ASC
        """,
    )
    fun observeSetlistPreviewSongs(setlistId: Long): Flow<List<SetlistPreviewSongRow>>

    @Query(
        """
        SELECT
            setlist_songs.id AS entryId,
            setlist_songs.songId AS songId,
            setlist_songs.position AS position,
            songs.name AS name,
            songs.artist AS artist,
            songs.preset AS preset,
            songs.keySignature AS keySignature,
            songs.chart AS chart
        FROM setlist_songs
        INNER JOIN songs ON songs.id = setlist_songs.songId
        WHERE setlist_songs.setlistId = :setlistId
        ORDER BY setlist_songs.position ASC, setlist_songs.id ASC
        """,
    )
    suspend fun getSetlistPreviewSongs(setlistId: Long): List<SetlistPreviewSongRow>

    @Insert
    suspend fun insertSetlist(setlist: SetlistEntity): Long

    @Update
    suspend fun updateSetlist(setlist: SetlistEntity)

    @Query("DELETE FROM setlists WHERE id = :setlistId")
    suspend fun deleteSetlist(setlistId: Long)

    @Query("DELETE FROM setlist_songs WHERE setlistId = :setlistId")
    suspend fun deleteSongsForSetlist(setlistId: Long)

    @Insert
    suspend fun insertSetlistSongs(entries: List<SetlistSongEntity>)
}
