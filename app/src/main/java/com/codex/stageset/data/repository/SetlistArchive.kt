package com.codex.stageset.data.repository

import org.json.JSONArray
import org.json.JSONObject

data class SetlistArchiveDocument(
    val suggestedFileName: String,
    val json: String,
)

data class SetlistArchiveImportResult(
    val setlistId: Long,
    val setlistName: String,
    val songCount: Int,
    val importedSongs: Int,
    val reusedSongs: Int,
)

internal data class SetlistArchivePayload(
    val version: Int,
    val exportedAt: Long,
    val setlist: SetlistArchiveSetlist,
    val songs: List<SetlistArchiveSong>,
    val entries: List<SetlistArchiveEntry>,
)

internal data class SetlistArchiveSetlist(
    val name: String,
    val notes: String,
    val createdAt: Long,
)

internal data class SetlistArchiveSong(
    val ref: String,
    val name: String,
    val artist: String,
    val preset: String,
    val keySignature: String,
    val chart: String,
    val compressedChart: String? = null,
)

internal data class SetlistArchiveEntry(
    val songRef: String,
    val position: Int,
)

internal object SetlistArchiveCodec {
    private const val CurrentVersion = 2
    private const val FormatName = "stage-set-archive"

    fun encode(payload: SetlistArchivePayload): String {
        val root = JSONObject()
            .put("format", FormatName)
            .put("version", payload.version)
            .put("exportedAt", payload.exportedAt)
            .put(
                "setlist",
                JSONObject()
                    .put("name", payload.setlist.name)
                    .put("notes", payload.setlist.notes)
                    .put("createdAt", payload.setlist.createdAt),
            )
            .put(
                "songs",
                JSONArray().apply {
                    payload.songs.forEach { song ->
                        put(
                            JSONObject()
                                .put("ref", song.ref)
                                .put("name", song.name)
                                .put("artist", song.artist)
                                .put("preset", song.preset)
                                .put("keySignature", song.keySignature)
                                .put("chart", song.chart)
                                .apply {
                                    song.compressedChart?.takeIf { it.isNotBlank() }?.let {
                                        put("compressedChart", it)
                                    }
                                },
                        )
                    }
                },
            )
            .put(
                "entries",
                JSONArray().apply {
                    payload.entries.forEach { entry ->
                        put(
                            JSONObject()
                                .put("songRef", entry.songRef)
                                .put("position", entry.position),
                        )
                    }
                },
            )

        return root.toString(2)
    }

    fun decode(json: String): SetlistArchivePayload {
        val root = JSONObject(json)
        val format = root.optString("format").ifBlank { FormatName }
        require(format == FormatName) {
            "This archive is not a StageSet export."
        }

        val version = root.optInt("version", -1)
        require(version in 1..CurrentVersion) {
            "Unsupported archive version."
        }

        val setlistJson = root.optJSONObject("setlist")
            ?: throw IllegalArgumentException("Archive is missing setlist details.")
        val songsJson = root.optJSONArray("songs")
            ?: throw IllegalArgumentException("Archive does not contain any songs.")
        val entriesJson = root.optJSONArray("entries")
            ?: JSONArray()

        val songs = buildList {
            for (index in 0 until songsJson.length()) {
                val songJson = songsJson.optJSONObject(index)
                    ?: throw IllegalArgumentException("Archive song ${index + 1} is invalid.")
                val ref = songJson.optString("ref").ifBlank { "song-${index + 1}" }
                add(
                        SetlistArchiveSong(
                            ref = ref,
                            name = songJson.optString("name").trim(),
                            artist = songJson.optString("artist").trim(),
                            preset = songJson.optString("preset").trim(),
                            keySignature = songJson.optString("keySignature").trim(),
                            chart = songJson.optString("chart").trimEnd(),
                            compressedChart = songJson.optString("compressedChart")
                                .trimEnd()
                                .ifBlank { null },
                        ),
                    )
                }
        }.also { songs ->
            require(songs.isNotEmpty()) {
                "Archive does not contain any songs."
            }
        }

        val songRefs = songs.map(SetlistArchiveSong::ref).toSet()
        val entries = if (entriesJson.length() == 0) {
            songs.mapIndexed { index, song ->
                SetlistArchiveEntry(
                    songRef = song.ref,
                    position = index,
                )
            }
        } else {
            buildList {
                for (index in 0 until entriesJson.length()) {
                    val entryJson = entriesJson.optJSONObject(index)
                        ?: throw IllegalArgumentException("Archive setlist entry ${index + 1} is invalid.")
                    add(
                        SetlistArchiveEntry(
                            songRef = entryJson.optString("songRef").trim(),
                            position = entryJson.optInt("position", index),
                        ),
                    )
                }
            }
        }.sortedBy(SetlistArchiveEntry::position).also { entries ->
            require(entries.isNotEmpty()) {
                "Archive does not contain a playable setlist."
            }
            require(entries.all { it.songRef in songRefs }) {
                "Archive references songs that are missing from the archive."
            }
        }

        return SetlistArchivePayload(
            version = version,
            exportedAt = root.optLong("exportedAt", System.currentTimeMillis()),
            setlist = SetlistArchiveSetlist(
                name = setlistJson.optString("name").trim(),
                notes = setlistJson.optString("notes").trim(),
                createdAt = setlistJson.optLong("createdAt", System.currentTimeMillis()),
            ),
            songs = songs,
            entries = entries,
        )
    }

    fun suggestedFileName(setlistName: String): String {
        val safeBase = setlistName.trim()
            .ifBlank { "stage-set" }
            .replace(Regex("""[\\/:*?"<>|]+"""), "-")
            .replace(Regex("""\s+"""), "_")
            .trim('_', '-', ' ')
            .ifBlank { "stage-set" }

        return "$safeBase.stageset.json"
    }
}
