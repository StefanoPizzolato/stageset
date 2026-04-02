package com.codex.stageset.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SetlistArchiveCodecTest {
    @Test
    fun encodeAndDecode_roundTripsSetlistSongsAndOrder() {
        val payload = SetlistArchivePayload(
            version = 1,
            exportedAt = 1234L,
            setlist = SetlistArchiveSetlist(
                name = "Wedding Set",
                notes = "Second half",
                createdAt = 9876L,
            ),
            songs = listOf(
                SetlistArchiveSong(
                    ref = "song-1",
                    name = "Signed Sealed Delivered",
                    artist = "Stevie Wonder",
                    preset = "A1",
                    keySignature = "F",
                    chart = "F   Bb",
                ),
                SetlistArchiveSong(
                    ref = "song-2",
                    name = "I Wish",
                    artist = "Stevie Wonder",
                    preset = "A2",
                    keySignature = "Ebm",
                    chart = "Ebm   Ab7",
                ),
            ),
            entries = listOf(
                SetlistArchiveEntry(songRef = "song-2", position = 1),
                SetlistArchiveEntry(songRef = "song-1", position = 0),
            ),
        )

        val decoded = SetlistArchiveCodec.decode(SetlistArchiveCodec.encode(payload))

        assertEquals(payload.setlist, decoded.setlist)
        assertEquals(payload.songs, decoded.songs)
        assertEquals(listOf("song-1", "song-2"), decoded.entries.map { it.songRef })
    }

    @Test
    fun decode_withoutEntriesFallsBackToSongOrder() {
        val archiveJson = """
            {
              "format": "stage-set-archive",
              "version": 1,
              "setlist": {
                "name": "Imported",
                "notes": "",
                "createdAt": 1
              },
              "songs": [
                { "ref": "first", "name": "Song A", "artist": "", "preset": "", "keySignature": "C", "chart": "C" },
                { "ref": "second", "name": "Song B", "artist": "", "preset": "", "keySignature": "D", "chart": "D" }
              ]
            }
        """.trimIndent()

        val decoded = SetlistArchiveCodec.decode(archiveJson)

        assertEquals(listOf("first", "second"), decoded.entries.map { it.songRef })
        assertEquals(listOf(0, 1), decoded.entries.map { it.position })
    }

    @Test
    fun suggestedFileName_sanitizesUnsafeCharacters() {
        val fileName = SetlistArchiveCodec.suggestedFileName("Friday / Wedding: Set?")

        assertTrue(fileName.endsWith(".stageset.json"))
        assertEquals("Friday_-_Wedding-_Set.stageset.json", fileName)
    }
}
