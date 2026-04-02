package com.codex.stageset.data.repository

data class Song(
    val id: Long,
    val name: String,
    val artist: String,
    val preset: String,
    val keySignature: String,
    val chart: String,
    val lastModified: Long,
)

data class SongSummary(
    val id: Long,
    val name: String,
    val artist: String,
    val preset: String,
    val keySignature: String,
    val lineCount: Int,
)

data class SongDraft(
    val name: String,
    val artist: String,
    val preset: String,
    val keySignature: String,
    val chart: String,
)

data class ImportedSongDraft(
    val name: String,
    val artist: String,
    val preset: String = "",
    val keySignature: String,
    val chart: String,
)

data class SetlistSong(
    val entryId: Long,
    val songId: Long,
    val position: Int,
    val name: String,
    val artist: String,
    val keySignature: String,
)

data class SetlistPreviewSong(
    val entryId: Long,
    val songId: Long,
    val position: Int,
    val name: String,
    val artist: String,
    val preset: String,
    val keySignature: String,
    val chart: String,
)

data class SetlistSummary(
    val id: Long,
    val name: String,
    val notes: String,
    val createdAt: Long,
    val songCount: Int,
)

data class SetlistDetail(
    val id: Long,
    val name: String,
    val notes: String,
    val createdAt: Long,
    val songs: List<SetlistSong>,
)

data class SetlistPreviewDetail(
    val id: Long,
    val name: String,
    val notes: String,
    val createdAt: Long,
    val songs: List<SetlistPreviewSong>,
)

data class SetlistDraft(
    val name: String,
    val notes: String,
    val songIds: List<Long>,
)
