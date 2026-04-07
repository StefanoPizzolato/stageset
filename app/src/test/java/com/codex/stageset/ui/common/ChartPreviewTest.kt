package com.codex.stageset.ui.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartPreviewTest {
    @Test
    fun protectSlashChordBreaks_keepsSlashChordsTogether() {
        val protected = protectSlashChordBreaks("E/G#    A/C#")

        assertEquals("E\u2060/\u2060G#    A\u2060/\u2060C#", protected.text)
    }

    @Test
    fun protectSlashChordBreaks_remapsAccentSpansAroundInsertedJoiners() {
        val protected = protectSlashChordBreaks(
            text = ":E/G#: x2",
            accentSpans = listOf(
                PreviewSpan(start = 0, endExclusive = 1),
                PreviewSpan(start = 5, endExclusive = 6),
                PreviewSpan(start = 7, endExclusive = 9),
            ),
        )

        assertEquals(":E\u2060/\u2060G#: x2", protected.text)
        assertEquals(
            listOf(
                PreviewSpan(start = 0, endExclusive = 1),
                PreviewSpan(start = 7, endExclusive = 8),
                PreviewSpan(start = 9, endExclusive = 11),
            ),
            protected.accentSpans,
        )
        assertTrue(protected.text.contains('\u2060'))
    }
}
