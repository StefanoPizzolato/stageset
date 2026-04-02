package com.codex.stageset.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query(
        """
        SELECT
            id,
            name,
            artist,
            preset,
            keySignature,
            lastModified,
            CASE
                WHEN TRIM(chart) = '' THEN 0
                ELSE LENGTH(chart) - LENGTH(REPLACE(chart, CHAR(10), '')) + 1
            END AS lineCount
        FROM songs
        ORDER BY name COLLATE NOCASE ASC
        """,
    )
    fun observeSongSummaries(): Flow<List<SongSummaryRow>>

    @Query("SELECT * FROM songs ORDER BY name COLLATE NOCASE ASC")
    fun observeSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE id = :songId")
    fun observeSong(songId: Long): Flow<SongEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: SongEntity): Long

    @Update
    suspend fun updateSong(song: SongEntity)

    @Query("DELETE FROM songs WHERE id = :songId")
    suspend fun deleteSong(songId: Long)
}
