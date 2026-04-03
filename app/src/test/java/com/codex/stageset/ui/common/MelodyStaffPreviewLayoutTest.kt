package com.codex.stageset.ui.common

import com.codex.stageset.chart.MelodyParseResult
import com.codex.stageset.chart.parseMelodyNotation
import org.junit.Assert.assertEquals
import org.junit.Test

class MelodyStaffPreviewLayoutTest {
    @Test
    fun layoutStaffRows_keeps_two_simple_bars_on_one_row_in_narrow_width() {
        val notation = (parseMelodyNotation("o4 l4 c d e f g a b > c") as MelodyParseResult.Success).notation

        val rows = layoutStaffRows(
            notation = notation,
            availableWidthDp = 260f,
            scale = 1f,
        )

        assertEquals(listOf(2), rows.map { it.bars.size })
    }
}
