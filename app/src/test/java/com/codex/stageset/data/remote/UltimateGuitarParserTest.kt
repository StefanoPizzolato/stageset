package com.codex.stageset.data.remote

import org.junit.Assert.assertEquals
import org.junit.Test

class UltimateGuitarParserTest {
    @Test
    fun formatMarkup_convertsChordTagsIntoChordLine() {
        val formatted = UltimateGuitarParser.formatMarkup("[ch]G[/ch]Amazing [ch]D[/ch]grace")

        assertEquals(
            "G       D\nAmazing grace",
            formatted,
        )
    }

    @Test
    fun formatMarkup_preservesSectionMarkers() {
        val formatted = UltimateGuitarParser.formatMarkup(
            """
            [Verse 1]
            [ch]G[/ch]Amazing [ch]D[/ch]grace
            
            [chorus]
            [ch]C[/ch]Sing it [ch]G[/ch]out
            """.trimIndent(),
        )

        assertEquals(
            """
            [Verse 1]
            G       D
            Amazing grace
            
            [Chorus]
            C       G
            Sing it out
            """.trimIndent(),
            formatted,
        )
    }

    @Test
    fun parse_extractsMetadataAndChartFromStructuredPage() {
        val html = """
            <html>
            <head>
                <title>Great Is Thy Faithfulness CHORDS by Hymn Writer @ Ultimate-Guitar.Com</title>
            </head>
            <body>
                <script>
                    window.UGAPP.store.page = {
                        "data": {
                            "tab_view": {
                                "artist_name": "Hymn Writer",
                                "song_name": "Great Is Thy Faithfulness",
                                "wiki_tab": {
                                    "content": "[ch]C[/ch]Great is Thy [ch]F[/ch]faithfulness"
                                }
                            }
                        }
                    };
                </script>
            </body>
            </html>
        """.trimIndent()

        val imported = UltimateGuitarParser.parse(html)

        assertEquals("Great Is Thy Faithfulness", imported.name)
        assertEquals("Hymn Writer", imported.artist)
        assertEquals("C            F\nGreat is Thy faithfulness", imported.chart)
    }

    @Test
    fun parse_preservesSectionMarkersFromStructuredPage() {
        val html = """
            <html>
            <head>
                <title>My Song CHORDS by My Artist @ Ultimate-Guitar.Com</title>
            </head>
            <body>
                <script>
                    window.UGAPP.store.page = {
                        "data": {
                            "tab_view": {
                                "artist_name": "My Artist",
                                "song_name": "My Song",
                                "wiki_tab": {
                                    "content": "[verse][ch]Em[/ch]First [ch]C[/ch]line[br][chorus][ch]G[/ch]Lift it [ch]D[/ch]up"
                                }
                            }
                        }
                    };
                </script>
            </body>
            </html>
        """.trimIndent()

        val imported = UltimateGuitarParser.parse(html)

        assertEquals(
            """
            [Verse]
            Em    C
            First line
            
            [Chorus]
            G       D
            Lift it up
            """.trimIndent(),
            imported.chart,
        )
    }
}
