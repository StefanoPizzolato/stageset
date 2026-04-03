package com.codex.stageset.chart

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MelodyNotationTest {
    @Test
    fun parseMelodyNotation_supports_key_meter_clef_rests_and_ties() {
        val result = parseMelodyNotation("key=Ebm meter=3/4 cleff=bass o3 l4 b e a8 a&a r8")

        val notation = (result as MelodyParseResult.Success).notation
        val symbols = notation.bars.flatMap { bar -> bar.events.map { it.symbol } }

        assertEquals(null, notation.tempoBpm)
        assertEquals("Ebm", notation.keySignature.name)
        assertEquals(MelodyKeyAccidentalType.Flat, notation.keySignature.accidentalType)
        assertEquals(6, notation.keySignature.accidentalCount)
        assertEquals(3, notation.meter.numerator)
        assertEquals(4, notation.meter.denominator)
        assertEquals(MelodyClef.Bass, notation.clef)
        assertTrue(notation.showKeySignature)
        assertTrue(notation.showMeter)
        assertTrue(notation.showClef)
        assertEquals(2, notation.bars.size)

        val leadingB = symbols[0] as MelodyNote
        val leadingE = symbols[1] as MelodyNote
        val leadingA = symbols[2] as MelodyNote
        val tiedStart = symbols[3] as MelodyNote
        val tiedEnd = symbols[4] as MelodyNote

        assertEquals(-1, leadingB.pitch.accidental)
        assertEquals(-1, leadingE.pitch.accidental)
        assertEquals(-1, leadingA.pitch.accidental)
        assertEquals(8, leadingA.duration.denominator)
        assertTrue(tiedStart.tieToNext)
        assertTrue(tiedStart.pitch.matchesPitch(tiedEnd.pitch))
        assertTrue(symbols.last() is MelodyRest)
    }

    @Test
    fun parseMelodyNotation_groups_notes_using_the_current_meter() {
        val result = parseMelodyNotation("meter=3/4 o4 l4 c d e f g")

        val notation = (result as MelodyParseResult.Success).notation

        assertEquals(2, notation.bars.size)
        assertEquals(3, notation.bars[0].events.size)
        assertEquals(2, notation.bars[1].events.size)
        assertEquals(0, notation.bars[0].events.first().startTick)
        assertEquals(64, notation.bars[0].events.last().startTick)
        assertEquals(0, notation.bars[1].events.first().startTick)
    }

    @Test
    fun parseMelodyNotation_rejects_ties_to_different_pitches() {
        val result = parseMelodyNotation("o4 l8 c&d")

        assertTrue(result is MelodyParseResult.Error)
        assertEquals(
            "Tie must join the same pitch.",
            (result as MelodyParseResult.Error).message,
        )
    }

    @Test
    fun parseMelodyNotation_updates_default_length_from_explicit_values_but_not_dots() {
        val result = parseMelodyNotation("o4 l4 c8 d e. f r16 g")

        val notation = (result as MelodyParseResult.Success).notation
        val symbols = notation.bars.flatMap { it.events.map { event -> event.symbol } }
        val notes = symbols.mapNotNull { it as? MelodyNote }
        val rest = symbols.filterIsInstance<MelodyRest>().single()

        assertEquals(5, notes.size)
        assertEquals(4, notes[0].pitch.octave)
        assertEquals(8, notes[0].duration.denominator)
        assertEquals(8, notes[1].duration.denominator)
        assertEquals(8, notes[2].duration.denominator)
        assertTrue(notes[2].duration.dotted)
        assertEquals(8, notes[3].duration.denominator)
        assertEquals(16, rest.duration.denominator)
        assertEquals(16, notes[4].duration.denominator)
        assertFalse(notes[4].duration.dotted)
    }

    @Test
    fun parseMelodyNotation_supports_explicit_natural_accidentals() {
        val result = parseMelodyNotation("key=Ebm o4 l4 an fn")

        val notation = (result as MelodyParseResult.Success).notation
        val notes = notation.bars.flatMap { it.events }.mapNotNull { it.symbol as? MelodyNote }

        assertEquals(2, notes.size)
        assertEquals(0, notes[0].pitch.accidental)
        assertEquals(0, notes[1].pitch.accidental)
        assertEquals("\uE261", notes[0].pitch.displayAccidentalSymbol)
        assertEquals("\uE261", notes[1].pitch.displayAccidentalSymbol)
    }

    @Test
    fun parseMelodyNotation_accepts_inline_octave_shifts_and_optional_tempo() {
        val result = parseMelodyNotation("o4l8cde>c<bb")

        val notation = (result as MelodyParseResult.Success).notation
        val notes = notation.bars.flatMap { it.events }.mapNotNull { it.symbol as? MelodyNote }

        assertEquals(null, notation.tempoBpm)
        assertFalse(notation.showKeySignature)
        assertFalse(notation.showMeter)
        assertFalse(notation.showClef)
        assertEquals(5, notes.size)
        assertEquals(4, notes[0].pitch.octave)
        assertEquals(5, notes[3].pitch.octave)
        assertEquals(-1, notes[4].pitch.accidental)
        assertFalse(notes.any { it.duration.denominator != 8 })
    }

    @Test
    fun trebleClef_maps_scientific_pitches_to_expected_staff_steps() {
        assertEquals(-2, MelodyClef.Treble.staffStepFor(MelodyPitch(step = MelodyStep.C, octave = 4)))
        assertEquals(0, MelodyClef.Treble.staffStepFor(MelodyPitch(step = MelodyStep.E, octave = 4)))
        assertEquals(2, MelodyClef.Treble.staffStepFor(MelodyPitch(step = MelodyStep.G, octave = 4)))
        assertEquals(4, MelodyClef.Treble.staffStepFor(MelodyPitch(step = MelodyStep.B, octave = 4)))
        assertEquals(8, MelodyClef.Treble.staffStepFor(MelodyPitch(step = MelodyStep.F, octave = 5)))
    }
}
