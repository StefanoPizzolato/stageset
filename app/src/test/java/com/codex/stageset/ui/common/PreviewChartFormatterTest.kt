package com.codex.stageset.ui.common

import org.junit.Assert.assertEquals
import org.junit.Test

class PreviewChartFormatterTest {
    @Test
    fun buildPreviewLines_canHideLyricsWhileKeepingStructureAndChords() {
        val chart = """
            [Verse]
            Dm       F
            A                  Am
            Amazing grace
            
            [Chorus]
            C       G
            Sing it out
        """.trimIndent()

        assertEquals(
            """
            [Verse]
            Dm	F
            A	   Am
            
            [Chorus]
            C      G
            """.trimIndent(),
            buildPreviewLines(
                chart = chart,
                options = PreviewRenderOptions(showLyrics = false),
            ).joinToString("\n") { it.text },
        )
    }

    @Test
    fun buildPreviewLines_hidesRepeatedMatchingSectionChordsAfterFirstOccurrence() {
        val chart = """
            [Chorus]
            C       G
            Am     F
            Sing it out
            
            [Verse]
            Em      D
            Amazing grace
            
            [Chorus]
            C   G
            Am          F
            Sing it louder
        """.trimIndent()

        assertEquals(
            """
            [Chorus]
            C       G
            Am     F
            Sing it out
            
            [Verse]
            Em      D
            Amazing grace
            
            [Chorus]
            Sing it louder
            """.trimIndent(),
            buildPreviewLines(
                chart = chart,
                options = PreviewRenderOptions(
                    showLyrics = true,
                    hideRepeatedSectionChords = true,
                ),
            ).joinToString("\n") { it.text },
        )
    }

    @Test
    fun buildPreviewLines_compactsAdjacentChordSymbolsWithoutTables() {
        val chart = """
            [Verse]
            Bb7F
            EbBb
            Hold the line
        """.trimIndent()

        assertEquals(
            """
            [Verse]
            Bb7  F
            Eb  Bb
            """.trimIndent(),
            buildPreviewLines(
                chart = chart,
                options = PreviewRenderOptions(showLyrics = false),
            ).joinToString("\n") { it.text },
        )
    }

    @Test
    fun buildPreviewLines_hidesRepeatedSectionsEvenWhenChordSpacingChanges() {
        val chart = """
            [Chorus]
            C       G
            Am     F
            First chorus

            [Chorus]
            CG
            AmF
            Second chorus
        """.trimIndent()

        assertEquals(
            """
            [Chorus]
            C       G
            Am     F
            First chorus

            [Chorus]
            Second chorus
            """.trimIndent(),
            buildPreviewLines(
                chart = chart,
                options = PreviewRenderOptions(
                    showLyrics = true,
                    hideRepeatedSectionChords = true,
                ),
            ).joinToString("\n") { it.text },
        )
    }
}
