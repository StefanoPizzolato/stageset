package com.codex.stageset.ui.common

import android.graphics.Paint
import android.graphics.Rect
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import com.codex.stageset.R
import com.codex.stageset.chart.MelodyBar
import com.codex.stageset.chart.MelodyClef
import com.codex.stageset.chart.MelodyDuration
import com.codex.stageset.chart.MelodyMeter
import com.codex.stageset.chart.MelodyNotation
import com.codex.stageset.chart.MelodyNote
import com.codex.stageset.chart.MelodyRest
import com.codex.stageset.chart.MelodyWholeNoteTicks
import kotlin.math.abs
import kotlin.math.max

@Composable
fun MelodyStaffPreview(
    notation: MelodyNotation,
    scale: Float = 1f,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f)
    val staffColor = Color.White
    val layoutScale = scale.coerceIn(0.7f, 1.7f)
    val musicTypeface = remember(context) {
        ResourcesCompat.getFont(context, R.font.petaluma)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = surfaceColor,
        shape = RoundedCornerShape(16.dp),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = (12f * layoutScale).dp,
                    vertical = (10f * layoutScale).dp,
                ),
        ) {
            val measuresPerRow = max(1, (maxWidth / (180.dp * layoutScale)).toInt())
            val rows = remember(notation, measuresPerRow) {
                notation.bars.chunked(measuresPerRow)
            }
            val rowHeightDp = 76f
            val rowGapDp = 4f
            val canvasHeight = (
                (
                    (rows.size * rowHeightDp) +
                        (((rows.size - 1).coerceAtLeast(0)) * rowGapDp)
                    ) * layoutScale
                ).dp

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy((6f * layoutScale).dp),
            ) {
                notation.tempoBpm?.let { bpm ->
                    Text(
                        text = "$bpm BPM",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                    )
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(canvasHeight),
                ) {
                    val lineSpacing = 12.dp.toPx() * layoutScale
                    val rowTopPadding = 14.dp.toPx() * layoutScale
                    val rowBottomPadding = 14.dp.toPx() * layoutScale
                    val rowHeight = rowTopPadding + (lineSpacing * 4f) + rowBottomPadding
                    val rowGap = 4.dp.toPx() * layoutScale
                    val horizontalPadding = 6.dp.toPx() * layoutScale
                    val measureGap = 12.dp.toPx() * layoutScale
                    val ledgerHalfWidth = 12.dp.toPx() * layoutScale
                    val barInsetStart = 12.dp.toPx() * layoutScale
                    val barInsetEnd = 10.dp.toPx() * layoutScale
                    val middleLineYOffset = lineSpacing * 2f

                    val accidentalPaint = createMusicPaint(
                        color = staffColor,
                        textSize = 18.dp.toPx() * layoutScale,
                        typeface = musicTypeface,
                    )
                    val clefPaint = createMusicPaint(
                        color = staffColor,
                        textSize = 38.dp.toPx() * layoutScale,
                        typeface = musicTypeface,
                    )
                    val meterPaint = createMusicPaint(
                        color = staffColor,
                        textSize = 22.dp.toPx() * layoutScale,
                        typeface = musicTypeface,
                    )
                    val restPaint = createMusicPaint(
                        color = staffColor,
                        textSize = 30.dp.toPx() * layoutScale,
                        typeface = musicTypeface,
                    )
                    val noteheadPaint = createMusicPaint(
                        color = staffColor,
                        textSize = 30.dp.toPx() * layoutScale,
                        typeface = musicTypeface,
                    )
                    val noteGlyphPaint = createMusicPaint(
                        color = staffColor,
                        textSize = 30.dp.toPx() * layoutScale,
                        typeface = musicTypeface,
                    )
                    val dotPaint = createMusicPaint(
                        color = staffColor,
                        textSize = 18.dp.toPx() * layoutScale,
                        typeface = musicTypeface,
                    )

                    val anchors = mutableMapOf<Int, NoteAnchor>()

                    rows.forEachIndexed { rowIndex, rowBars ->
                        val rowTop = rowIndex * (rowHeight + rowGap)
                        val staffTop = rowTop + rowTopPadding
                        val staffBottom = staffTop + (lineSpacing * 4f)
                        val rowStartX = horizontalPadding
                        val rowEndX = size.width - horizontalPadding
                        val showRowClef = notation.showClef && rowIndex == 0
                        val showRowKeySignature = notation.showKeySignature && rowIndex == 0
                        val showRowMeter = notation.showMeter && rowIndex == 0
                        val prefixWidth = measurePrefixWidth(
                            notation = notation,
                            pxPerDp = 1.dp.toPx(),
                            scale = layoutScale,
                            showClef = showRowClef,
                            showKeySignature = showRowKeySignature,
                            showMeter = showRowMeter,
                        )
                        val systemStartX = rowStartX + prefixWidth
                        val measureWidth = if (rowBars.isEmpty()) {
                            rowEndX - systemStartX
                        } else {
                            (rowEndX - systemStartX - (measureGap * (rowBars.size - 1))) / rowBars.size
                        }
                        val middleLineY = staffTop + middleLineYOffset

                        for (lineIndex in 0..4) {
                            val y = staffTop + (lineSpacing * lineIndex)
                            drawLine(
                                color = staffColor,
                                start = Offset(rowStartX, y),
                                end = Offset(rowEndX, y),
                                strokeWidth = 1.3.dp.toPx(),
                            )
                        }

                        drawStaffPrefix(
                            clef = notation.clef,
                            notation = notation,
                            rowStartX = rowStartX,
                            staffTop = staffTop,
                            staffBottom = staffBottom,
                            lineSpacing = lineSpacing,
                            clefPaint = clefPaint,
                            accidentalPaint = accidentalPaint,
                            meterPaint = meterPaint,
                            scale = layoutScale,
                            showClef = showRowClef,
                            showKeySignature = showRowKeySignature,
                            showMeter = showRowMeter,
                        )

                        rowBars.forEachIndexed { barIndex, bar ->
                            val measureStartX = systemStartX + (barIndex * (measureWidth + measureGap))
                            val measureEndX = measureStartX + measureWidth
                            val contentStartX = measureStartX + barInsetStart
                            val contentEndX = measureEndX - barInsetEnd
                            val contentWidth = (contentEndX - contentStartX).coerceAtLeast(24.dp.toPx())

                            val positionedEvents = bar.events.map { event ->
                                val x = contentStartX +
                                    (contentWidth * (event.startTick / notation.meter.barTicks.toFloat()))
                                when (val symbol = event.symbol) {
                                    is MelodyNote -> {
                                        val staffStep = notation.clef.staffStepFor(symbol.pitch)
                                        val y = staffCenterYForStep(
                                            staffBottom = staffBottom,
                                            lineSpacing = lineSpacing,
                                            staffStep = staffStep,
                                        )
                                        val noteheadLayout = layoutGlyph(
                                            paint = noteheadPaint,
                                            glyph = noteheadGlyphFor(symbol.duration),
                                            centerX = x,
                                            centerY = y,
                                        )
                                        PositionedBarEventLayout(
                                            startTick = event.startTick,
                                            symbol = symbol,
                                            note = PositionedNote(
                                                note = symbol,
                                                x = x,
                                                y = y,
                                                staffStep = staffStep,
                                                rowIndex = rowIndex,
                                                notehead = noteheadLayout,
                                            ),
                                        )
                                    }

                                    is MelodyRest -> PositionedBarEventLayout(
                                        startTick = event.startTick,
                                        symbol = symbol,
                                        rest = PositionedRest(
                                            rest = symbol,
                                            x = x,
                                        ),
                                    )
                                }
                            }

                            val beamGroups = buildBeamGroups(
                                bar = bar,
                                positionedEvents = positionedEvents,
                                meter = notation.meter,
                                middleLineY = middleLineY,
                                lineSpacing = lineSpacing,
                            )
                            val beamedSequences = beamGroups
                                .flatMap { group -> group.notes.map { note -> note.note.sequenceIndex } }
                                .toSet()

                            positionedEvents.forEach { positioned ->
                                when (val symbol = positioned.symbol) {
                                    is MelodyNote -> {
                                        val note = positioned.note ?: return@forEach
                                        drawLedgerLines(
                                            x = note.x,
                                            staffStep = note.staffStep,
                                            staffBottom = staffBottom,
                                            lineSpacing = lineSpacing,
                                            halfWidth = max(
                                                ledgerHalfWidth,
                                                ((note.notehead.right - note.notehead.left) / 2f) +
                                                    (4.dp.toPx() * layoutScale),
                                            ),
                                            color = staffColor,
                                        )
                                        drawNoteAccidental(
                                            note = note,
                                            accidentalPaint = accidentalPaint,
                                            scale = layoutScale,
                                        )
                                        anchors[symbol.sequenceIndex] = NoteAnchor(
                                            note = symbol,
                                            notehead = note.notehead,
                                            rowIndex = rowIndex,
                                        )
                                        if (symbol.sequenceIndex !in beamedSequences) {
                                            drawStandaloneNote(
                                                note = note,
                                                middleLineY = middleLineY,
                                                lineSpacing = lineSpacing,
                                                noteGlyphPaint = noteGlyphPaint,
                                                dotPaint = dotPaint,
                                                scale = layoutScale,
                                            )
                                        }
                                    }

                                    is MelodyRest -> {
                                        val rest = positioned.rest ?: return@forEach
                                        drawRest(
                                            rest = rest,
                                            staffTop = staffTop,
                                            lineSpacing = lineSpacing,
                                            restPaint = restPaint,
                                            dotPaint = dotPaint,
                                            scale = layoutScale,
                                        )
                                    }
                                }
                            }

                            beamGroups.forEach { group ->
                                drawBeamGroup(
                                    group = group,
                                    lineSpacing = lineSpacing,
                                    color = staffColor,
                                    scale = layoutScale,
                                )
                            }

                            beamGroups
                                .flatMap { it.notes }
                                .distinctBy { it.note.sequenceIndex }
                                .forEach { note ->
                                    drawNoteHead(
                                        note = note,
                                        noteheadPaint = noteheadPaint,
                                    )
                                    drawAugmentationDot(
                                        note = note,
                                        lineSpacing = lineSpacing,
                                        dotPaint = dotPaint,
                                        scale = layoutScale,
                                    )
                                }

                            drawLine(
                                color = staffColor,
                                start = Offset(measureEndX, staffTop),
                                end = Offset(measureEndX, staffBottom),
                                strokeWidth = if (
                                    rowIndex == rows.lastIndex &&
                                    barIndex == rowBars.lastIndex
                                ) {
                                    2.2.dp.toPx()
                                } else {
                                    1.5.dp.toPx()
                                },
                            )
                        }
                    }

                    anchors.values.forEach { anchor ->
                        if (!anchor.note.tieToNext) {
                            return@forEach
                        }
                        val nextAnchor = anchors[anchor.note.sequenceIndex + 1] ?: return@forEach
                        if (anchor.rowIndex != nextAnchor.rowIndex) {
                            return@forEach
                        }

                        val tieStartX = anchor.notehead.right
                        val tieEndX = nextAnchor.notehead.left
                        val tieY = max(anchor.notehead.bottom, nextAnchor.notehead.bottom) +
                            (10.dp.toPx() * layoutScale)
                        val tiePath = Path().apply {
                            moveTo(tieStartX, tieY)
                            quadraticTo(
                                (tieStartX + tieEndX) / 2f,
                                tieY + (10.dp.toPx() * layoutScale),
                                tieEndX,
                                tieY,
                            )
                        }
                        drawPath(
                            path = tiePath,
                            color = staffColor,
                            style = Stroke(width = 2.dp.toPx() * layoutScale, cap = StrokeCap.Round),
                        )
                    }
                }
            }
        }
    }
}

private data class GlyphLayout(
    val glyph: String,
    val drawX: Float,
    val baselineY: Float,
    val bounds: Rect,
) {
    val left: Float
        get() = drawX + bounds.left

    val right: Float
        get() = drawX + bounds.right

    val top: Float
        get() = baselineY + bounds.top

    val bottom: Float
        get() = baselineY + bounds.bottom
}

private data class PositionedNote(
    val note: MelodyNote,
    val x: Float,
    val y: Float,
    val staffStep: Int,
    val rowIndex: Int,
    val notehead: GlyphLayout,
)

private data class PositionedRest(
    val rest: MelodyRest,
    val x: Float,
)

private data class PositionedBarEventLayout(
    val startTick: Int,
    val symbol: Any,
    val note: PositionedNote? = null,
    val rest: PositionedRest? = null,
)

private data class BeamGroup(
    val notes: List<PositionedNote>,
    val denominator: Int,
    val stemUp: Boolean,
    val slope: Float,
    val intercept: Float,
) {
    val beamCount: Int
        get() = when (denominator) {
            8 -> 1
            16 -> 2
            32 -> 3
            else -> 0
        }

    fun beamEdgeYAt(x: Float): Float = (slope * x) + intercept
}

private data class BeamLayoutCandidate(
    val stemUp: Boolean,
    val slope: Float,
    val intercept: Float,
    val totalStemLength: Float,
)

private data class NoteAnchor(
    val note: MelodyNote,
    val notehead: GlyphLayout,
    val rowIndex: Int,
)

private fun createMusicPaint(
    color: Color,
    textSize: Float,
    typeface: android.graphics.Typeface?,
): Paint {
    return Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color.toArgb()
        this.textSize = textSize
        this.textAlign = Paint.Align.LEFT
        this.typeface = typeface
    }
}

private fun DrawScope.drawStaffPrefix(
    clef: MelodyClef,
    notation: MelodyNotation,
    rowStartX: Float,
    staffTop: Float,
    staffBottom: Float,
    lineSpacing: Float,
    clefPaint: Paint,
    accidentalPaint: Paint,
    meterPaint: Paint,
    scale: Float,
    showClef: Boolean,
    showKeySignature: Boolean,
    showMeter: Boolean,
) {
    val shouldShowKeySignature = showKeySignature && notation.keySignature.accidentalCount > 0
    var currentX = rowStartX

    if (showClef) {
        val clefCenterX = currentX + (13.dp.toPx() * scale)
        val clefCenterY = staffCenterYForStep(
            staffBottom = staffBottom,
            lineSpacing = lineSpacing,
            staffStep = clef.symbolStaffStepFromBottom,
        ) + clefVisualCenterYOffset(
            clef = clef,
            lineSpacing = lineSpacing,
        )
        val layout = layoutGlyph(
            paint = clefPaint,
            glyph = clef.symbol,
            centerX = clefCenterX,
            centerY = clefCenterY,
        )
        drawGlyph(layout, clefPaint)
        currentX = layout.right + (3.dp.toPx() * scale)
    }

    if (shouldShowKeySignature) {
        val accidentalSymbol = notation.keySignature.accidentalType.symbol
        val signatureSteps = clef.keySignatureStaffSteps(
            type = notation.keySignature.accidentalType,
            count = notation.keySignature.accidentalCount,
        )
        signatureSteps.forEach { staffStep ->
            val accidentalCenterX = currentX + (3.dp.toPx() * scale)
            val accidentalCenterY = staffCenterYForStep(
                staffBottom = staffBottom,
                lineSpacing = lineSpacing,
                staffStep = staffStep,
            )
            val layout = layoutGlyph(
                paint = accidentalPaint,
                glyph = accidentalSymbol,
                centerX = accidentalCenterX,
                centerY = accidentalCenterY,
            )
            drawGlyph(layout, accidentalPaint)
            currentX = layout.right + (1.5.dp.toPx() * scale)
        }
        currentX += 3.dp.toPx() * scale
    }

    if (showMeter) {
        val numeratorLayout = layoutGlyph(
            paint = meterPaint,
            glyph = notation.meter.numerator.toPetalumaTimeSig(),
            centerX = currentX + (8.dp.toPx() * scale),
            centerY = staffTop + (lineSpacing * 1.5f),
        )
        val denominatorLayout = layoutGlyph(
            paint = meterPaint,
            glyph = notation.meter.denominator.toPetalumaTimeSig(),
            centerX = currentX + (8.dp.toPx() * scale),
            centerY = staffTop + (lineSpacing * 3.25f),
        )
        drawGlyph(numeratorLayout, meterPaint)
        drawGlyph(denominatorLayout, meterPaint)
    }
}

private fun DrawScope.drawLedgerLines(
    x: Float,
    staffStep: Int,
    staffBottom: Float,
    lineSpacing: Float,
    halfWidth: Float,
    color: Color,
) {
    if (staffStep < 0) {
        var ledgerStep = -2
        while (ledgerStep >= staffStep) {
            val ledgerY = staffCenterYForStep(
                staffBottom = staffBottom,
                lineSpacing = lineSpacing,
                staffStep = ledgerStep,
            )
            drawLine(
                color = color,
                start = Offset(x - halfWidth, ledgerY),
                end = Offset(x + halfWidth, ledgerY),
                strokeWidth = 1.2.dp.toPx(),
            )
            ledgerStep -= 2
        }
    } else if (staffStep > 8) {
        var ledgerStep = 10
        while (ledgerStep <= staffStep) {
            val ledgerY = staffCenterYForStep(
                staffBottom = staffBottom,
                lineSpacing = lineSpacing,
                staffStep = ledgerStep,
            )
            drawLine(
                color = color,
                start = Offset(x - halfWidth, ledgerY),
                end = Offset(x + halfWidth, ledgerY),
                strokeWidth = 1.2.dp.toPx(),
            )
            ledgerStep += 2
        }
    }
}

private fun DrawScope.drawNoteHead(
    note: PositionedNote,
    noteheadPaint: Paint,
) {
    drawGlyph(note.notehead, noteheadPaint)
}

private fun DrawScope.drawStandaloneNote(
    note: PositionedNote,
    middleLineY: Float,
    lineSpacing: Float,
    noteGlyphPaint: Paint,
    dotPaint: Paint,
    scale: Float,
) {
    val stemUp = note.y >= middleLineY
    val glyph = standaloneNoteGlyphFor(
        duration = note.note.duration,
        stemUp = stemUp,
    )
    val layout = if (note.note.duration.denominator == 1) {
        layoutGlyph(
            paint = noteGlyphPaint,
            glyph = glyph,
            centerX = note.x,
            centerY = note.y,
        )
    } else {
        layoutStandaloneNoteGlyph(
            paint = noteGlyphPaint,
            glyph = glyph,
            notehead = note.notehead,
            stemUp = stemUp,
        )
    }
    drawGlyph(layout, noteGlyphPaint)
    drawAugmentationDot(
        note = note,
        lineSpacing = lineSpacing,
        dotPaint = dotPaint,
        scale = scale,
    )
}

private fun DrawScope.drawNoteAccidental(
    note: PositionedNote,
    accidentalPaint: Paint,
    scale: Float,
) {
    val accidental = note.note.pitch.displayAccidentalSymbol ?: return
    val layout = layoutGlyph(
        paint = accidentalPaint,
        glyph = accidental,
        centerX = note.notehead.left - (6.dp.toPx() * scale),
        centerY = note.y,
    )
    drawGlyph(layout, accidentalPaint)
}

private fun DrawScope.drawAugmentationDot(
    note: PositionedNote,
    lineSpacing: Float,
    dotPaint: Paint,
    scale: Float,
) {
    if (!note.note.duration.dotted) {
        return
    }

    val dotCenterY = if (note.staffStep % 2 == 0) {
        note.y - (lineSpacing / 2f)
    } else {
        note.y
    }
    val layout = layoutGlyph(
        paint = dotPaint,
        glyph = smuflGlyph(0xE1E7),
        centerX = note.notehead.right + (7.dp.toPx() * scale),
        centerY = dotCenterY,
    )
    drawGlyph(layout, dotPaint)
}

private fun DrawScope.drawRest(
    rest: PositionedRest,
    staffTop: Float,
    lineSpacing: Float,
    restPaint: Paint,
    dotPaint: Paint,
    scale: Float,
) {
    val glyph = when (rest.rest.duration.denominator) {
        1 -> smuflGlyph(0xE4E3)
        2 -> smuflGlyph(0xE4E4)
        4 -> smuflGlyph(0xE4E5)
        8 -> smuflGlyph(0xE4E6)
        16 -> smuflGlyph(0xE4E7)
        32 -> smuflGlyph(0xE4E8)
        else -> smuflGlyph(0xE4E5)
    }
    val centerY = when (rest.rest.duration.denominator) {
        1 -> staffTop + (lineSpacing * 1.9f)
        2 -> staffTop + (lineSpacing * 2.4f)
        4 -> staffTop + (lineSpacing * 2.2f)
        else -> staffTop + (lineSpacing * 2.3f)
    }
    val layout = layoutGlyph(
        paint = restPaint,
        glyph = glyph,
        centerX = rest.x,
        centerY = centerY,
    )
    drawGlyph(layout, restPaint)

    if (rest.rest.duration.dotted) {
        val dotLayout = layoutGlyph(
            paint = dotPaint,
            glyph = smuflGlyph(0xE1E7),
            centerX = layout.right + (6.dp.toPx() * scale),
            centerY = centerY,
        )
        drawGlyph(dotLayout, dotPaint)
    }
}

private fun DrawScope.drawBeamGroup(
    group: BeamGroup,
    lineSpacing: Float,
    color: Color,
    scale: Float,
) {
    val stemThickness = 1.6.dp.toPx() * scale
    val beamThickness = 4.dp.toPx() * scale
    val beamGap = 3.dp.toPx() * scale

    group.notes.forEach { note ->
        val stemX = note.stemX(stemUp = group.stemUp, lineSpacing = lineSpacing)
        val beamEdgeY = group.beamEdgeYAt(stemX)
        drawLine(
            color = color,
            start = Offset(stemX, note.y),
            end = Offset(stemX, beamEdgeY),
            strokeWidth = stemThickness,
            cap = StrokeCap.Round,
        )
    }

    val firstStemX = group.notes.first().stemX(stemUp = group.stemUp, lineSpacing = lineSpacing)
    val lastStemX = group.notes.last().stemX(stemUp = group.stemUp, lineSpacing = lineSpacing)
    repeat(group.beamCount) { beamIndex ->
        val offset = beamIndex * (beamThickness + beamGap)
        val startEdgeY = if (group.stemUp) {
            group.beamEdgeYAt(firstStemX) - offset
        } else {
            group.beamEdgeYAt(firstStemX) + offset
        }
        val endEdgeY = if (group.stemUp) {
            group.beamEdgeYAt(lastStemX) - offset
        } else {
            group.beamEdgeYAt(lastStemX) + offset
        }
        val path = Path().apply {
            if (group.stemUp) {
                moveTo(firstStemX, startEdgeY)
                lineTo(lastStemX, endEdgeY)
                lineTo(lastStemX, endEdgeY - beamThickness)
                lineTo(firstStemX, startEdgeY - beamThickness)
            } else {
                moveTo(firstStemX, startEdgeY)
                lineTo(lastStemX, endEdgeY)
                lineTo(lastStemX, endEdgeY + beamThickness)
                lineTo(firstStemX, startEdgeY + beamThickness)
            }
            close()
        }
        drawPath(
            path = path,
            color = color,
        )
    }
}

private fun buildBeamGroups(
    bar: MelodyBar,
    positionedEvents: List<PositionedBarEventLayout>,
    meter: MelodyMeter,
    middleLineY: Float,
    lineSpacing: Float,
): List<BeamGroup> {
    val groups = mutableListOf<BeamGroup>()
    var currentNotes = mutableListOf<PositionedNote>()
    var currentDenominator: Int? = null
    var currentBeatGroup = -1
    var previousEndTick = -1

    fun flushGroup() {
        val notes = currentNotes.toList()
        if (notes.size > 1 && currentDenominator != null) {
            groups += createBeamGroup(
                notes = notes,
                denominator = currentDenominator ?: return,
                middleLineY = middleLineY,
                lineSpacing = lineSpacing,
            )
        }
        currentNotes = mutableListOf()
        currentDenominator = null
        currentBeatGroup = -1
        previousEndTick = -1
    }

    positionedEvents.forEachIndexed { index, positioned ->
        val note = positioned.note
        if (note == null) {
            flushGroup()
            return@forEachIndexed
        }

        val duration = note.note.duration
        val denominator = duration.denominator
        val groupTicks = meter.beamGroupTicks(denominator)
        val beamable = denominator in setOf(8, 16, 32) &&
            !duration.dotted &&
            !note.note.tieToNext
        if (!beamable) {
            flushGroup()
            return@forEachIndexed
        }

        val beatGroup = positioned.startTick / groupTicks
        val noteEndTick = positioned.startTick + duration.ticks
        val continuesCurrent = currentNotes.isNotEmpty() &&
            currentDenominator == denominator &&
            currentBeatGroup == beatGroup &&
            previousEndTick == positioned.startTick &&
            bar.events.getOrNull(index - 1)?.symbol is MelodyNote

        if (!continuesCurrent) {
            flushGroup()
        }

        if (currentNotes.isEmpty()) {
            currentDenominator = denominator
            currentBeatGroup = beatGroup
        }
        currentNotes += note
        previousEndTick = noteEndTick
    }
    flushGroup()
    return groups
}

private fun createBeamGroup(
    notes: List<PositionedNote>,
    denominator: Int,
    middleLineY: Float,
    lineSpacing: Float,
): BeamGroup {
    val averageStemUp = notes.map { it.y }.average().toFloat() >= middleLineY
    val upward = createBeamLayoutCandidate(
        notes = notes,
        stemUp = true,
        lineSpacing = lineSpacing,
    )
    val downward = createBeamLayoutCandidate(
        notes = notes,
        stemUp = false,
        lineSpacing = lineSpacing,
    )
    val best = when {
        abs(upward.totalStemLength - downward.totalStemLength) < lineSpacing / 3f ->
            if (averageStemUp) upward else downward
        upward.totalStemLength <= downward.totalStemLength -> upward
        else -> downward
    }
    return BeamGroup(
        notes = notes,
        denominator = denominator,
        stemUp = best.stemUp,
        slope = best.slope,
        intercept = best.intercept,
    )
}

private fun createBeamLayoutCandidate(
    notes: List<PositionedNote>,
    stemUp: Boolean,
    lineSpacing: Float,
): BeamLayoutCandidate {
    val firstStemX = notes.first().stemX(stemUp = stemUp, lineSpacing = lineSpacing)
    val lastStemX = notes.last().stemX(stemUp = stemUp, lineSpacing = lineSpacing)
    val slopeSpan = (lastStemX - firstStemX).coerceAtLeast(1f)
    val rawSlope = ((notes.last().y - notes.first().y) / slopeSpan) * 0.12f
    val maxSlope = (lineSpacing * 0.4f) / slopeSpan
    val slope = rawSlope.coerceIn(-abs(maxSlope), abs(maxSlope))
    val minStemLength = lineSpacing * 1.9f
    val intercept = if (stemUp) {
        notes.minOf { note ->
            note.y - minStemLength - (slope * note.stemX(stemUp = true, lineSpacing = lineSpacing))
        }
    } else {
        notes.maxOf { note ->
            note.y + minStemLength - (slope * note.stemX(stemUp = false, lineSpacing = lineSpacing))
        }
    }
    val totalStemLength = notes.sumOf { note ->
        val stemX = note.stemX(stemUp = stemUp, lineSpacing = lineSpacing)
        abs(((slope * stemX) + intercept) - note.y).toDouble()
    }.toFloat()
    return BeamLayoutCandidate(
        stemUp = stemUp,
        slope = slope,
        intercept = intercept,
        totalStemLength = totalStemLength,
    )
}

private fun PositionedNote.stemX(
    stemUp: Boolean,
    lineSpacing: Float,
): Float {
    val inset = lineSpacing * 0.08f
    return if (stemUp) {
        notehead.right - inset
    } else {
        notehead.left + inset
    }
}

private fun staffCenterYForStep(
    staffBottom: Float,
    lineSpacing: Float,
    staffStep: Int,
): Float {
    return staffBottom - (staffStep * (lineSpacing / 2f))
}

private fun clefVisualCenterYOffset(
    clef: MelodyClef,
    lineSpacing: Float,
): Float {
    return when (clef) {
        MelodyClef.Treble -> (lineSpacing * (7f / 12f)) - lineSpacing
        MelodyClef.Bass -> lineSpacing * (5f / 12f)
        MelodyClef.Alto, MelodyClef.Tenor -> lineSpacing * 0.5f
    }
}

private fun layoutGlyph(
    paint: Paint,
    glyph: String,
    centerX: Float,
    centerY: Float,
): GlyphLayout {
    val bounds = Rect()
    paint.getTextBounds(glyph, 0, glyph.length, bounds)
    val drawX = centerX - bounds.exactCenterX()
    val baselineY = centerY - bounds.exactCenterY()
    return GlyphLayout(
        glyph = glyph,
        drawX = drawX,
        baselineY = baselineY,
        bounds = bounds,
    )
}

private fun layoutStandaloneNoteGlyph(
    paint: Paint,
    glyph: String,
    notehead: GlyphLayout,
    stemUp: Boolean,
): GlyphLayout {
    val bounds = Rect()
    paint.getTextBounds(glyph, 0, glyph.length, bounds)
    val drawX = if (stemUp) {
        notehead.left - bounds.left
    } else {
        notehead.right - bounds.right
    }
    val baselineY = if (stemUp) {
        notehead.bottom - bounds.bottom
    } else {
        notehead.top - bounds.top
    }
    return GlyphLayout(
        glyph = glyph,
        drawX = drawX,
        baselineY = baselineY,
        bounds = bounds,
    )
}

private fun DrawScope.drawGlyph(
    layout: GlyphLayout,
    paint: Paint,
) {
    drawIntoCanvas { canvas ->
        canvas.nativeCanvas.drawText(
            layout.glyph,
            layout.drawX,
            layout.baselineY,
            paint,
        )
    }
}

private fun noteheadGlyphFor(duration: MelodyDuration): String {
    return when (duration.denominator) {
        1 -> smuflGlyph(0xE0A2)
        2 -> smuflGlyph(0xE0A3)
        else -> smuflGlyph(0xE0A4)
    }
}

private fun standaloneNoteGlyphFor(
    duration: MelodyDuration,
    stemUp: Boolean,
): String {
    return when (duration.denominator) {
        1 -> smuflGlyph(0xE1D2)
        2 -> if (stemUp) smuflGlyph(0xE1D3) else smuflGlyph(0xE1D4)
        4 -> if (stemUp) smuflGlyph(0xE1D5) else smuflGlyph(0xE1D6)
        8 -> if (stemUp) smuflGlyph(0xE1D7) else smuflGlyph(0xE1D8)
        16 -> if (stemUp) smuflGlyph(0xE1D9) else smuflGlyph(0xE1DA)
        32 -> if (stemUp) smuflGlyph(0xE1DB) else smuflGlyph(0xE1DC)
        else -> smuflGlyph(0xE1D5)
    }
}

private fun MelodyMeter.primaryBeatTicks(): Int {
    val unitTicks = MelodyWholeNoteTicks / denominator
    return if (denominator == 8 && numerator > 3 && numerator % 3 == 0) {
        unitTicks * 3
    } else {
        unitTicks
    }
}

private fun MelodyMeter.beamGroupTicks(noteDenominator: Int): Int {
    val baseTicks = primaryBeatTicks()
    return when {
        numerator == 4 && denominator == 4 && noteDenominator == 8 -> baseTicks * 2
        else -> baseTicks
    }
}

private fun Int.toPetalumaTimeSig(): String {
    return toString().map { digit ->
        String(Character.toChars(0xE080 + digit.digitToInt()))
    }.joinToString(separator = "")
}

private fun Color.toArgb(): Int {
    return android.graphics.Color.argb(
        (alpha * 255).toInt(),
        (red * 255).toInt(),
        (green * 255).toInt(),
        (blue * 255).toInt(),
    )
}

private fun measurePrefixWidth(
    notation: MelodyNotation,
    pxPerDp: Float,
    scale: Float,
    showClef: Boolean,
    showKeySignature: Boolean,
    showMeter: Boolean,
): Float {
    var width = 0f

    if (showClef) {
        width += 24f
    }
    if (showKeySignature && notation.keySignature.accidentalCount > 0) {
        if (width > 0f) {
            width += 3f
        }
        width += notation.keySignature.accidentalCount * 7.5f
    }
    if (showMeter) {
        if (width > 0f) {
            width += 4f
        }
        width += 18f
    }

    return if (width > 0f) {
        (width + 8f) * pxPerDp * scale
    } else {
        0f
    }
}

private fun smuflGlyph(codePoint: Int): String {
    return String(Character.toChars(codePoint))
}
