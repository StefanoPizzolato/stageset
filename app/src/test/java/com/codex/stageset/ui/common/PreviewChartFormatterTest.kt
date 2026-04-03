package com.codex.stageset.ui.common

import com.codex.stageset.chart.parseChordPlacements
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PreviewChartFormatterTest {
    @Test
    fun buildPreviewLines_canHideLyricsWhileKeepingStructureAndColumnizedChords() {
        val chart = """
            [Verse]
            Dm       F
            A                  Am
            Amazing grace
            
            [Chorus]
            C       G
            Sing it out
        """.trimIndent()

        val rendered = buildPreviewLines(
            chart = chart,
            options = PreviewRenderOptions(showLyrics = false),
        )

        assertEquals("[Verse]", rendered[0].text)
        assertEquals("[Chorus]", rendered[4].text)
        assertFalse(rendered.any { it.type == PreviewLineType.Lyric })
        assertEquals(
            visualChordStarts(rendered[1].text)[1],
            visualChordStarts(rendered[2].text)[1],
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
            Eb   Bb
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

    @Test
    fun buildPreviewLines_hidesRepeatedNumberedSectionFamiliesWhenChordsMatch() {
        val chart = """
            [Chorus 1]
            C       G
            Am     F
            First chorus

            [Verse]
            Em      D
            Something else

            [Chorus 2]
            C   G
            Am        F
            Second chorus
        """.trimIndent()

        assertEquals(
            """
            [Chorus 1]
            C       G
            Am     F
            First chorus

            [Verse]
            Em      D
            Something else

            [Chorus 2]
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

    @Test
    fun buildPreviewLines_compressModeHidesRepeatedNumberedSectionFamiliesInChordOnlyPreview() {
        val chart = """
            [Verse 1]
            Ebm    Ab7
            Gb     Bb7

            [Verse 2]
            Ebm Ab7
            Gb  Bb7
        """.trimIndent()

        assertEquals(
            """
            [Verse 1]
            Ebm    Ab7    Gb    Bb7

            [Verse 2]
            """.trimIndent(),
            buildPreviewLines(
                chart = chart,
                options = PreviewRenderOptions(
                    showLyrics = false,
                    compressChords = true,
                ),
            ).joinToString("\n") { it.text },
        )
    }

    @Test
    fun buildPreviewLines_hidesRepeatedSectionChordsEvenWhenMelodyBlocksDiffer() {
        val chart = """
            [Chorus 1]
            C       G
            @
            o4 l4 c d
            @
            First chorus

            [Chorus 2]
            C   G
            @
            o4 l4 e f
            @
            Second chorus
        """.trimIndent()

        assertEquals(
            """
            [Chorus 1]
            C       G
            o4 l4 c d
            First chorus

            [Chorus 2]
            o4 l4 e f
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

    @Test
    fun buildPreviewLines_compressModeStillCompressesSectionsThatContainMelodyBlocks() {
        val chart = """
            [Verse]
            F      Am
            F      Am
            @
            o4 l4 c d
            @
        """.trimIndent()

        assertEquals(
            """
            [Verse]
            :F  Am: x2
            o4 l4 c d
            """.trimIndent(),
            buildPreviewLines(
                chart = chart,
                options = PreviewRenderOptions(
                    showLyrics = false,
                    compressChords = true,
                ),
            ).joinToString("\n") { it.text },
        )
    }

    @Test
    fun buildPreviewLines_canCompressRepeatedChordRuns() {
        val chart = """
            [Verse]
            F      Am
            F           Am
            G      C
            F     Am
        """.trimIndent()

        val rendered = buildPreviewLines(
            chart = chart,
            options = PreviewRenderOptions(
                showLyrics = false,
                compressChords = true,
            ),
        )

        assertEquals("[Verse]", rendered[0].text)
        assertEquals(":F  Am: x2   G  C  F  Am", rendered[1].text)
        assertTrue(rendered[1].accentSpans.isNotEmpty())
    }

    @Test
    fun buildPreviewLines_canCompressLongRepeatedChordRuns() {
        val chart = """
            [Chorus]
            Ebm    Ab
            Ebm    Ab
            Ebm    Ab
            Ebm    Ab
            Ebm    Ab
            Ebm    Ab
            G   Gb7
        """.trimIndent()

        assertEquals(
            """
            [Chorus]
            :Ebm    Ab: x6   G    Gb7
            """.trimIndent(),
            buildPreviewLines(
                chart = chart,
                options = PreviewRenderOptions(
                    showLyrics = false,
                    compressChords = true,
                ),
            ).joinToString("\n") { it.text },
        )
    }

    @Test
    fun buildPreviewLines_compressesAcrossHiddenLyricBreaksWithinASection() {
        val chart = """
            [Chorus 1]
                         EbmAb7 Ebm         Ab7
            I wish those days, could, come back once more,
                          EbmAb7Ebm     Ab7
            Why did those days, e   -ver have to go?
            
                         EbmAb7 Ebm         Ab7
            I wish those days, could, come back once more,
                          EbmAb7Ebm     Ab7               Ebm
            Why did those days, e   -ver have to go? 'Cos I love them so.
        """.trimIndent()

        assertEquals(
            """
            [Chorus 1]
            :Ebm    Ab7: x8   Ebm
            """.trimIndent(),
            buildPreviewLines(
                chart = chart,
                options = PreviewRenderOptions(
                    showLyrics = false,
                    compressChords = true,
                    hideRepeatedSectionChords = true,
                ),
            ).joinToString("\n") { it.text },
        )
    }

    @Test
    fun buildPreviewLines_prefersShortestRepeatingMotifAcrossExpandedSection() {
        val chart = """
            [Verse 1]
            Ebm    Ab7    Ebm    Ab7    Ebm    Ab7    Ebm    Ab7    Ebm    Ab7    Ebm    Ab7    Ebm    Ab7    Ebm    Ab7
            Ebm    Ab7    Ebm    Ab7    Ebm    Ab7    Ebm    Ab7    Ebm    Ab7    Ebm    Ab7    Ebm    Ab7    Ebm    Ab7
        """.trimIndent()

        assertEquals(
            """
            [Verse 1]
            :Ebm    Ab7: x16
            """.trimIndent(),
            buildPreviewLines(
                chart = chart,
                options = PreviewRenderOptions(
                    showLyrics = false,
                    compressChords = true,
                ),
            ).joinToString("\n") { it.text },
        )
    }

    @Test
    fun buildPreviewLines_compactModeCollapsesAdjacentDuplicateChordsBeforeGrouping() {
        val chart = """
            [Verse]
            Am    C    D    D
            Am C D
        """.trimIndent()

        assertEquals(
            """
            [Verse]
            :Am  C  D: x2
            """.trimIndent(),
            buildPreviewLines(
                chart = chart,
                options = PreviewRenderOptions(
                    showLyrics = false,
                    compressChords = true,
                ),
            ).joinToString("\n") { it.text },
        )
    }

    @Test
    fun buildPreviewLines_nonCompressedChordViewNeverSquashesAdjacentChords() {
        val chart = """
            [Verse]
            EmD
            Em   D
        """.trimIndent()

        val rendered = buildPreviewLines(
            chart = chart,
            options = PreviewRenderOptions(showLyrics = false),
        )

        assertEquals("[Verse]", rendered[0].text)
        assertNotEquals("EmD", rendered[1].text)
        assertNotEquals("EmD", rendered[2].text)
        assertTrue(rendered[1].text.contains(Regex("""Em\s+D""")))
        assertTrue(rendered[2].text.contains(Regex("""Em\s+D""")))
    }

    @Test
    fun buildPreviewLines_groupsMatchingSectionFamiliesForSharedHeadingColor() {
        val chart = """
            [Verse 1]
            C   G

            [Chorus]
            F   Am

            [Verse 2]
            C   G
        """.trimIndent()

        val sectionGroups = buildPreviewLines(chart)
            .filter { it.type == PreviewLineType.Section }
            .map { it.sectionColorGroup }

        assertEquals(listOf("verse", "chorus", "verse"), sectionGroups)
    }

    @Test
    fun buildPreviewLines_appliesSectionColorGroupToChordLines() {
        val chart = """
            [Verse 1]
            C   G
            first line

            [Verse 2]
            Dm   Am
            second line
        """.trimIndent()

        val chordGroups = buildPreviewLines(chart)
            .filter { it.type == PreviewLineType.Chord }
            .map { it.sectionColorGroup }

        assertEquals(listOf("verse", "verse"), chordGroups)
    }

    @Test
    fun buildPreviewLines_appliesSectionColorGroupToCompressedChordLines() {
        val chart = """
            [Bridge 1]
            Ebm    Ab7
            Gb     Bb7
        """.trimIndent()

        val chordLines = buildPreviewLines(
            chart = chart,
            options = PreviewRenderOptions(
                showLyrics = false,
                compressChords = true,
            ),
        ).filter { it.type == PreviewLineType.Chord }

        assertEquals(1, chordLines.size)
        assertEquals("bridge", chordLines.single().sectionColorGroup)
    }

    @Test
    fun buildPreviewLines_canHideChordLinesWhileKeepingOtherVisibleContent() {
        val chart = """
            [Verse]
            C       G
            @
            o4 l4 c d
            @
            Sing it out
        """.trimIndent()

        val rendered = buildPreviewLines(
            chart = chart,
            options = PreviewRenderOptions(
                showChords = false,
            ),
        ).filter { it.type != PreviewLineType.Empty }

        assertEquals(
            listOf(
                PreviewLineType.Section,
                PreviewLineType.Melody,
                PreviewLineType.Lyric,
            ),
            rendered.map { it.type },
        )
    }

    @Test
    fun buildPreviewLines_canHideNotationWhileKeepingChordLines() {
        val chart = """
            [Verse]
            C       G
            @
            o4 l4 c d
            @
            Sing it out
        """.trimIndent()

        val rendered = buildPreviewLines(
            chart = chart,
            options = PreviewRenderOptions(
                showNotation = false,
            ),
        ).filter { it.type != PreviewLineType.Empty }

        assertEquals(
            listOf(
                PreviewLineType.Section,
                PreviewLineType.Chord,
                PreviewLineType.Lyric,
            ),
            rendered.map { it.type },
        )
    }

    @Test
    fun buildPreviewLines_canShowLyricsCueFromFirstFourWordsOfSection() {
        val chart = """
            [Verse]
            C       G
            Amazing grace how sweet the sound
            That saved a wretch like me
        """.trimIndent()

        val rendered = buildPreviewLines(
            chart = chart,
            options = PreviewRenderOptions(
                showLyricsCue = true,
            ),
        ).filter { it.type != PreviewLineType.Empty }

        assertEquals(
            listOf(
                PreviewLineType.Section,
                PreviewLineType.LyricCue,
                PreviewLineType.Chord,
                PreviewLineType.Lyric,
                PreviewLineType.Lyric,
            ),
            rendered.map { it.type },
        )
        assertEquals("Amazing grace how sweet...", rendered[1].text)
    }

    @Test
    fun buildPreviewLines_canShowLyricsCueInCompressedChordPreview() {
        val chart = """
            [Verse]
            F      Am
            Amazing grace how sweet the sound
        """.trimIndent()

        assertEquals(
            """
            [Verse]
            Amazing grace how sweet...
            F  Am
            """.trimIndent(),
            buildPreviewLines(
                chart = chart,
                options = PreviewRenderOptions(
                    showLyrics = false,
                    showLyricsCue = true,
                    compressChords = true,
                ),
            ).joinToString("\n") { it.text },
        )
    }

    @Test
    fun buildPreviewLines_twoColumnModeTightensChordSpacing() {
        val chart = """
            [Verse]
            Ebm                Ab7
            Stevie sings
        """.trimIndent()

        val rendered = buildPreviewLines(
            chart = chart,
            options = PreviewRenderOptions(
                showLyrics = true,
                twoColumns = true,
            ),
        )

        assertEquals("[Verse]", rendered[0].text)
        assertTrue(rendered[1].text.contains(Regex("""Ebm\s+Ab7""")))
        assertTrue(rendered[1].text.length < "Ebm                Ab7".length)
        assertEquals("Stevie sings", rendered[2].text)
    }

    @Test
    fun buildPreviewLines_inserts_melody_blocks_in_section_order() {
        val chart = """
            [Verse]
            C       G
            @
            t120 o4 l4 c d e f
            @
            Sing it out
        """.trimIndent()

        val rendered = buildPreviewLines(chart)
            .filter { it.type != PreviewLineType.Empty }

        assertEquals(
            listOf(
                PreviewLineType.Section,
                PreviewLineType.Chord,
                PreviewLineType.Melody,
                PreviewLineType.Lyric,
            ),
            rendered.map { it.type },
        )
        assertEquals(120, rendered[2].melodyNotation?.tempoBpm)
        assertEquals(1, rendered[2].melodyNotation?.bars?.size)
        assertEquals("Sing it out", rendered[3].text)
    }

    @Test
    fun buildPreviewLines_surfaces_melody_parse_errors() {
        val chart = """
            [Verse]
            @
            o4 l8 c&d
            @
        """.trimIndent()

        val rendered = buildPreviewLines(chart)
            .filter { it.type != PreviewLineType.Empty }

        assertEquals(PreviewLineType.Section, rendered[0].type)
        assertEquals(PreviewLineType.MelodyError, rendered[1].type)
        assertTrue(rendered[1].text.contains("Tie must join the same pitch."))
    }

    @Test
    fun splitPreviewLinesForTwoColumns_keepsSectionsGroupedInReadingOrder() {
        val chart = """
            [Intro]
            C

            [Verse]
            D

            [Chorus]
            E

            [Bridge]
            F
        """.trimIndent()

        val columns = splitPreviewLinesForTwoColumns(
            buildPreviewLines(
                chart = chart,
                options = PreviewRenderOptions(showLyrics = false),
            ),
        )

        assertEquals(2, columns.size)
        assertEquals(
            listOf("[Intro]", "[Verse]"),
            columns[0].filter { it.type == PreviewLineType.Section }.map { it.text },
        )
        assertEquals(
            listOf("[Chorus]", "[Bridge]"),
            columns[1].filter { it.type == PreviewLineType.Section }.map { it.text },
        )
    }

    private fun visualChordStarts(line: String): List<Int> {
        val placements = parseChordPlacements(line).orEmpty()
        return placements.map { placement ->
            visualWidth(line.take(placement.start))
        }
    }

    private fun visualWidth(text: String): Int {
        var column = 0
        text.forEach { character ->
            column = if (character == '\t') {
                ((column / 8) + 1) * 8
            } else {
                column + 1
            }
        }
        return column
    }
}
