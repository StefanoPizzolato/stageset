package com.codex.stageset.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "songs",
    indices = [Index(value = ["name"])],
)
data class SongEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val artist: String,
    val preset: String,
    val keySignature: String,
    val chart: String,
    val lastModified: Long,
)

@Entity(tableName = "setlists")
data class SetlistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val notes: String,
    val createdAt: Long,
)

@Entity(
    tableName = "setlist_songs",
    foreignKeys = [
        ForeignKey(
            entity = SetlistEntity::class,
            parentColumns = ["id"],
            childColumns = ["setlistId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = SongEntity::class,
            parentColumns = ["id"],
            childColumns = ["songId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["setlistId"]),
        Index(value = ["songId"]),
    ],
)
data class SetlistSongEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val setlistId: Long,
    val songId: Long,
    val position: Int,
)

data class SetlistSummaryRow(
    val id: Long,
    val name: String,
    val notes: String,
    val createdAt: Long,
    val songCount: Int,
)

data class SongSummaryRow(
    val id: Long,
    val name: String,
    val artist: String,
    val preset: String,
    val keySignature: String,
    val lastModified: Long,
    val lineCount: Int,
)

data class SetlistSongRow(
    val entryId: Long,
    val songId: Long,
    val position: Int,
    val name: String,
    val artist: String,
    val keySignature: String,
)

data class SetlistPreviewSongRow(
    val entryId: Long,
    val songId: Long,
    val position: Int,
    val name: String,
    val artist: String,
    val preset: String,
    val keySignature: String,
    val chart: String,
)
