package com.codex.stageset.chart

import org.junit.Assert.assertEquals
import org.junit.Test

class ChordTransposerTest {
    @Test
    fun transposeChordSymbol_usesSharpsWhenGoingUpAndFlatsWhenGoingDown() {
        assertEquals("D#", transposeChordSymbol("D", TransposeDirection.Up))
        assertEquals("E", transposeChordSymbol("D#", TransposeDirection.Up))
        assertEquals("Eb", transposeChordSymbol("E", TransposeDirection.Down))
        assertEquals("D", transposeChordSymbol("Eb", TransposeDirection.Down))
    }

    @Test
    fun transposeChordSymbol_transposesChordQualityAndBassNote() {
        assertEquals("Bmaj7/G#", transposeChordSymbol("Bbmaj7/G", TransposeDirection.Up))
        assertEquals("Am7/G", transposeChordSymbol("Bbm7/Ab", TransposeDirection.Down))
    }

    @Test
    fun transposeChart_onlyChangesChordLines() {
        val chart = """
            [Verse]
            D      A/C#
            Hold me close
            Em   D
        """.trimIndent()

        assertEquals(
            """
            [Verse]
            D#     A#/D
            Hold me close
            Fm   D#
            """.trimIndent(),
            transposeChart(chart, TransposeDirection.Up),
        )
    }

    @Test
    fun transposeChordLine_preservesVisibleSeparationBetweenChords() {
        assertEquals("Fm  D#", transposeChordLine("EmD", TransposeDirection.Up))
        assertEquals("Ebm  Db", transposeChordLine("EmD", TransposeDirection.Down))
    }

    @Test
    fun transposeKeySignature_tracksDirectionalEnharmonics() {
        val up = transposeKeySignature("D", TransposeDirection.Up)
        val upAgain = transposeKeySignature(up, TransposeDirection.Up)
        val down = transposeKeySignature(upAgain, TransposeDirection.Down)
        val downAgain = transposeKeySignature(down, TransposeDirection.Down)

        assertEquals("D#", up)
        assertEquals("E", upAgain)
        assertEquals("Eb", down)
        assertEquals("D", downAgain)
    }
}
