package com.codex.stageset.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UltimateGuitarSearchParserTest {
    @Test
    fun parseChordResults_extractsOnlyChordEntriesFromStructuredPage() {
        val html = """
            <html>
            <body>
                <script>
                    window.__PRELOADED_STATE__ = {
                        "data": {
                            "search": {
                                "results": [
                                    {
                                        "song_name": "I Wish",
                                        "artist_name": "Stevie Wonder",
                                        "tab_url": "https://tabs.ultimate-guitar.com/tab/stevie-wonder/i-wish-chords-101",
                                        "type": "Chords",
                                        "version": 2
                                    },
                                    {
                                        "song_name": "I Wish",
                                        "artist_name": "Stevie Wonder",
                                        "tab_url": "https://tabs.ultimate-guitar.com/tab/stevie-wonder/i-wish-tabs-102",
                                        "type": "Tab"
                                    },
                                    {
                                        "song_name": "I Wish",
                                        "artist_name": "Stevie Wonder",
                                        "tab_url": "https://tabs.ultimate-guitar.com/tab/stevie-wonder/i-wish-ukulele-chords-103",
                                        "type": "Ukulele Chords"
                                    }
                                ]
                            }
                        }
                    };
                </script>
            </body>
            </html>
        """.trimIndent()

        val results = UltimateGuitarSearchParser.parseChordResults(html)

        assertEquals(1, results.size)
        assertEquals("I Wish", results.single().title)
        assertEquals("Stevie Wonder", results.single().artist)
        assertEquals(2, results.single().version)
        assertEquals(
            "https://tabs.ultimate-guitar.com/tab/stevie-wonder/i-wish-chords-101",
            results.single().url,
        )
    }

    @Test
    fun parseChordResults_fallsBackToChordLinksWhenStructuredDataIsMissing() {
        val html = """
            <html>
            <body>
                <a href="https://tabs.ultimate-guitar.com/tab/chaka-khan/aint-nobody-chords-401">Aint Nobody</a>
                <a href="https://tabs.ultimate-guitar.com/tab/chaka-khan/aint-nobody-tabs-402">Aint Nobody Tab</a>
            </body>
            </html>
        """.trimIndent()

        val results = UltimateGuitarSearchParser.parseChordResults(html)

        assertEquals(1, results.size)
        assertEquals("Aint Nobody", results.single().title)
        assertTrue(results.single().url.contains("chords-401"))
    }
}
