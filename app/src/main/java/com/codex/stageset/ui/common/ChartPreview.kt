package com.codex.stageset.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codex.stageset.chart.MelodyNotation
import kotlin.math.roundToInt

private val FallbackSectionHeadingPalette = listOf(
    Color(0xFF6DD6FF),
    Color(0xFF7EE787),
    Color(0xFFFF7BC1),
    Color(0xFF9A8CFF),
    Color(0xFF6FE8D8),
    Color(0xFF9FD356),
    Color(0xFFFF6B9A),
    Color(0xFF8CB4FF),
)

private val KnownSectionHeadingColors = mapOf(
    "verse" to Color(0xFF6DD6FF),
    "chorus" to Color(0xFF7EE787),
    "bridge" to Color(0xFFFF7BC1),
    "intro" to Color(0xFF9A8CFF),
    "outro" to Color(0xFF6FE8D8),
    "pre chorus" to Color(0xFF9FD356),
    "post chorus" to Color(0xFFFF6B9A),
    "refrain" to Color(0xFF8CB4FF),
    "instrumental" to Color(0xFF5FE1D6),
    "interlude" to Color(0xFF86E6FF),
    "solo" to Color(0xFFFF8AD8),
    "hook" to Color(0xFF7AC8FF),
    "tag" to Color(0xFFB7F171),
    "turnaround" to Color(0xFFB18CFF),
    "ending" to Color(0xFF66E4C4),
)

private const val WordJoiner = '\u2060'

internal data class ProtectedChordText(
    val text: String,
    val accentSpans: List<PreviewSpan>,
)

@Composable
fun ChartPreview(
    title: String,
    artist: String,
    keySignature: String,
    chart: String,
    compressedChart: String? = null,
    showHeader: Boolean = true,
    textSizeSp: Int = 16,
    previewOptions: PreviewRenderOptions = PreviewRenderOptions(),
    modifier: Modifier = Modifier,
) {
    val previewChartSource = remember(chart, compressedChart, previewOptions) {
        resolvePreviewChartSource(
            chart = chart,
            compressedChart = compressedChart,
            options = previewOptions,
        )
    }
    val previewLines = remember(previewChartSource) {
        buildPreviewLines(previewChartSource.chart, previewChartSource.options)
    }
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val defaultSectionColor = MaterialTheme.colorScheme.secondary
    val sectionHeadingColors = remember(
        previewLines,
        previewOptions.colorizeSectionHeadings,
        defaultSectionColor,
    ) {
        buildSectionHeadingColors(
            previewLines = previewLines,
            enabled = previewOptions.colorizeSectionHeadings,
            defaultColor = defaultSectionColor,
        )
    }
    val chartTextStyle = MaterialTheme.typography.bodyLarge.copy(
        fontSize = textSizeSp.sp,
        lineHeight = (textSizeSp * 1.45f).sp,
    )
    val titleTextStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold)
    val subtitleTextStyle = MaterialTheme.typography.bodyLarge
    val sectionTextStyle = MaterialTheme.typography.titleMedium.copy(
        fontSize = (textSizeSp * 0.9f).sp,
        lineHeight = (textSizeSp * 1.2f).sp,
    )
    val tempoTextStyle = MaterialTheme.typography.labelLarge
    val notationScale = (textSizeSp / 20f).coerceIn(0.7f, 1.7f)

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.large,
    ) {
        SelectionContainer {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
            ) {
                val useTwoColumns = previewOptions.twoColumns && maxWidth >= 720.dp
                val lineColumns = remember(
                    previewLines,
                    useTwoColumns,
                    maxWidth,
                    maxHeight,
                    showHeader,
                    title,
                    artist,
                    keySignature,
                    titleTextStyle,
                    subtitleTextStyle,
                    chartTextStyle,
                    sectionTextStyle,
                    tempoTextStyle,
                    notationScale,
                    density,
                    textMeasurer,
                ) {
                    if (useTwoColumns) {
                        resolveTwoColumnPreviewLineColumns(
                            previewLines = previewLines,
                            title = title,
                            artist = artist,
                            keySignature = keySignature,
                            showHeader = showHeader,
                            titleTextStyle = titleTextStyle,
                            subtitleTextStyle = subtitleTextStyle,
                            chartTextStyle = chartTextStyle,
                            sectionTextStyle = sectionTextStyle,
                            tempoTextStyle = tempoTextStyle,
                            notationScale = notationScale,
                            maxWidth = maxWidth,
                            maxHeight = maxHeight,
                            density = density,
                            textMeasurer = textMeasurer,
                        )
                    } else {
                        listOf(previewLines)
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .verticalScroll(scrollState)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (showHeader) {
                        Text(
                            text = title.ifBlank { "Untitled song" },
                            style = titleTextStyle,
                        )
                        if (artist.isNotBlank() || keySignature.isNotBlank()) {
                            Text(
                                text = listOfNotNull(
                                    artist.takeIf { it.isNotBlank() },
                                    keySignature.takeIf { it.isNotBlank() }?.let { "Key $it" },
                                ).joinToString(" - "),
                                style = subtitleTextStyle,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (previewChartSource.chart.isBlank()) {
                        Text(
                            text = "Your lyrics and chord chart will preview here.",
                            style = chartTextStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else if (previewLines.isEmpty()) {
                        Text(
                            text = "No chart content is visible with the current preview settings.",
                            style = chartTextStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else if (useTwoColumns && lineColumns.size > 1) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                        ) {
                            lineColumns.forEach { lines ->
                                PreviewLinesColumn(
                                    lines = lines,
                                    sectionHeadingColors = sectionHeadingColors,
                                    defaultSectionColor = defaultSectionColor,
                                    chartTextStyle = chartTextStyle,
                                    sectionTextStyle = sectionTextStyle,
                                    notationScale = notationScale,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    } else {
                        PreviewLinesColumn(
                            lines = previewLines,
                            sectionHeadingColors = sectionHeadingColors,
                            defaultSectionColor = defaultSectionColor,
                            chartTextStyle = chartTextStyle,
                            sectionTextStyle = sectionTextStyle,
                            notationScale = notationScale,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewLinesColumn(
    lines: List<PreviewLine>,
    sectionHeadingColors: Map<String, Color>,
    defaultSectionColor: Color,
    chartTextStyle: androidx.compose.ui.text.TextStyle,
    sectionTextStyle: androidx.compose.ui.text.TextStyle,
    notationScale: Float,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        lines.forEach { line ->
            when (line.type) {
                PreviewLineType.Section -> {
                    Text(
                        text = line.text,
                        style = sectionTextStyle,
                        fontWeight = FontWeight.SemiBold,
                        color = line.sectionColorGroup
                            ?.let(sectionHeadingColors::get)
                            ?: defaultSectionColor,
                    )
                }

                PreviewLineType.Chord -> {
                    val sectionChordColor = line.sectionColorGroup?.let(sectionHeadingColors::get)
                    val chordColor = sectionChordColor ?: MaterialTheme.colorScheme.primary
                    val protectedChordText = protectSlashChordBreaks(
                        text = line.text,
                        accentSpans = line.accentSpans,
                    )
                    Text(
                        text = buildAnnotatedString {
                            append(protectedChordText.text)
                            protectedChordText.accentSpans.forEach { span ->
                                addStyle(
                                    SpanStyle(
                                        color = sectionChordColor ?: MaterialTheme.colorScheme.secondary,
                                        fontWeight = FontWeight.SemiBold,
                                    ),
                                    start = span.start,
                                    end = span.endExclusive,
                                )
                            }
                        },
                        style = chartTextStyle,
                        fontFamily = FontFamily.Monospace,
                        color = chordColor,
                    )
                }

                PreviewLineType.Lyric -> {
                    Text(
                        text = line.text,
                        style = chartTextStyle,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                PreviewLineType.LyricCue -> {
                    Text(
                        text = line.text,
                        style = chartTextStyle,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                PreviewLineType.Melody -> {
                    line.melodyNotation?.let { notation ->
                        MelodyStaffPreview(
                            notation = notation,
                            scale = notationScale,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } ?: Text(
                        text = line.text,
                        style = chartTextStyle,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                PreviewLineType.MelodyError -> {
                    Text(
                        text = line.text,
                        style = chartTextStyle,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                PreviewLineType.Empty -> {
                    Text(
                        text = " ",
                        style = chartTextStyle,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

private fun resolveTwoColumnPreviewLineColumns(
    previewLines: List<PreviewLine>,
    title: String,
    artist: String,
    keySignature: String,
    showHeader: Boolean,
    titleTextStyle: TextStyle,
    subtitleTextStyle: TextStyle,
    chartTextStyle: TextStyle,
    sectionTextStyle: TextStyle,
    tempoTextStyle: TextStyle,
    notationScale: Float,
    maxWidth: Dp,
    maxHeight: Dp,
    density: androidx.compose.ui.unit.Density,
    textMeasurer: TextMeasurer,
): List<List<PreviewLine>> {
    val balancedColumns = splitPreviewLinesForTwoColumns(previewLines)
    if (balancedColumns.size <= 1 || maxHeight == Dp.Infinity) {
        return balancedColumns
    }

    val contentPaddingPx = with(density) { 24.dp.roundToPx() }
    val columnSpacingPx = with(density) { 24.dp.roundToPx() }
    val lineSpacingPx = with(density) { 8.dp.roundToPx() }
    val contentWidthPx = (with(density) { maxWidth.roundToPx() } - (contentPaddingPx * 2)).coerceAtLeast(0)
    val columnWidthPx = ((contentWidthPx - columnSpacingPx) / 2).coerceAtLeast(0)
    if (columnWidthPx <= 0) {
        return balancedColumns
    }

    val availableColumnHeightPx = (
        with(density) { maxHeight.roundToPx() } -
            (contentPaddingPx * 2) -
            measurePreviewHeaderHeightPx(
                title = title,
                artist = artist,
                keySignature = keySignature,
                showHeader = showHeader,
                titleTextStyle = titleTextStyle,
                subtitleTextStyle = subtitleTextStyle,
                textMeasurer = textMeasurer,
                maxWidthPx = contentWidthPx,
                density = density,
            )
        ).coerceAtLeast(0)

    if (availableColumnHeightPx <= 0) {
        return balancedColumns
    }

    val balancedOverflow = balancedColumns.any { column ->
        measurePreviewColumnHeightPx(
            measurePreviewLineHeightsPx(
                lines = column,
                columnWidthPx = columnWidthPx,
                chartTextStyle = chartTextStyle,
                sectionTextStyle = sectionTextStyle,
                tempoTextStyle = tempoTextStyle,
                notationScale = notationScale,
                textMeasurer = textMeasurer,
                density = density,
            ),
            lineSpacingPx = lineSpacingPx,
        ) > availableColumnHeightPx
    }

    if (!balancedOverflow) {
        return balancedColumns
    }

    return splitPreviewLinesForHeightConstrainedColumns(
        lines = previewLines,
        lineHeightsPx = measurePreviewLineHeightsPx(
            lines = previewLines,
            columnWidthPx = columnWidthPx,
            chartTextStyle = chartTextStyle,
            sectionTextStyle = sectionTextStyle,
            tempoTextStyle = tempoTextStyle,
            notationScale = notationScale,
            textMeasurer = textMeasurer,
            density = density,
        ),
        maxFirstColumnHeightPx = availableColumnHeightPx,
        lineSpacingPx = lineSpacingPx,
    )
}

private fun measurePreviewHeaderHeightPx(
    title: String,
    artist: String,
    keySignature: String,
    showHeader: Boolean,
    titleTextStyle: TextStyle,
    subtitleTextStyle: TextStyle,
    textMeasurer: TextMeasurer,
    maxWidthPx: Int,
    density: androidx.compose.ui.unit.Density,
): Int {
    if (!showHeader || maxWidthPx <= 0) {
        return 0
    }

    val spacingPx = with(density) { 8.dp.roundToPx() }
    var totalHeightPx = measurePreviewTextHeightPx(
        text = title.ifBlank { "Untitled song" },
        style = titleTextStyle,
        maxWidthPx = maxWidthPx,
        textMeasurer = textMeasurer,
    )

    val subtitle = listOfNotNull(
        artist.takeIf { it.isNotBlank() },
        keySignature.takeIf { it.isNotBlank() }?.let { "Key $it" },
    ).joinToString(" - ")

    if (subtitle.isNotBlank()) {
        totalHeightPx += spacingPx
        totalHeightPx += measurePreviewTextHeightPx(
            text = subtitle,
            style = subtitleTextStyle,
            maxWidthPx = maxWidthPx,
            textMeasurer = textMeasurer,
        )
    }

    totalHeightPx += spacingPx
    return totalHeightPx
}

private fun measurePreviewLineHeightsPx(
    lines: List<PreviewLine>,
    columnWidthPx: Int,
    chartTextStyle: TextStyle,
    sectionTextStyle: TextStyle,
    tempoTextStyle: TextStyle,
    notationScale: Float,
    textMeasurer: TextMeasurer,
    density: androidx.compose.ui.unit.Density,
): List<Int> {
    val sectionStyle = sectionTextStyle.copy(fontWeight = FontWeight.SemiBold)
    val chordStyle = chartTextStyle.copy(fontFamily = FontFamily.Monospace)
    val lyricStyle = chartTextStyle.copy(fontFamily = FontFamily.Monospace)
    val lyricCueStyle = chartTextStyle.copy(fontStyle = FontStyle.Italic)
    val melodyErrorStyle = chartTextStyle.copy(fontFamily = FontFamily.Monospace)

    return lines.map { line ->
        when (line.type) {
            PreviewLineType.Section -> measurePreviewTextHeightPx(
                text = line.text,
                style = sectionStyle,
                maxWidthPx = columnWidthPx,
                textMeasurer = textMeasurer,
            )
            PreviewLineType.Chord -> measurePreviewTextHeightPx(
                text = protectSlashChordBreaks(
                    text = line.text,
                    accentSpans = line.accentSpans,
                ).text,
                style = chordStyle,
                maxWidthPx = columnWidthPx,
                textMeasurer = textMeasurer,
            )
            PreviewLineType.Lyric -> measurePreviewTextHeightPx(
                text = line.text,
                style = lyricStyle,
                maxWidthPx = columnWidthPx,
                textMeasurer = textMeasurer,
            )
            PreviewLineType.LyricCue -> measurePreviewTextHeightPx(
                text = line.text,
                style = lyricCueStyle,
                maxWidthPx = columnWidthPx,
                textMeasurer = textMeasurer,
            )
            PreviewLineType.Melody -> line.melodyNotation?.let { notation ->
                estimateMelodyPreviewHeightPx(
                    notation = notation,
                    columnWidthPx = columnWidthPx,
                    tempoTextStyle = tempoTextStyle,
                    notationScale = notationScale,
                    textMeasurer = textMeasurer,
                    density = density,
                )
            } ?: measurePreviewTextHeightPx(
                text = line.text,
                style = melodyErrorStyle,
                maxWidthPx = columnWidthPx,
                textMeasurer = textMeasurer,
            )
            PreviewLineType.MelodyError -> measurePreviewTextHeightPx(
                text = line.text,
                style = melodyErrorStyle,
                maxWidthPx = columnWidthPx,
                textMeasurer = textMeasurer,
            )
            PreviewLineType.Empty -> measurePreviewTextHeightPx(
                text = " ",
                style = lyricStyle,
                maxWidthPx = columnWidthPx,
                textMeasurer = textMeasurer,
            )
        }
    }
}

private fun measurePreviewTextHeightPx(
    text: String,
    style: TextStyle,
    maxWidthPx: Int,
    textMeasurer: TextMeasurer,
): Int {
    if (maxWidthPx <= 0) {
        return 0
    }

    return textMeasurer.measure(
        text = AnnotatedString(text),
        style = style,
        constraints = Constraints(maxWidth = maxWidthPx),
    ).size.height
}

private fun estimateMelodyPreviewHeightPx(
    notation: MelodyNotation,
    columnWidthPx: Int,
    tempoTextStyle: TextStyle,
    notationScale: Float,
    textMeasurer: TextMeasurer,
    density: androidx.compose.ui.unit.Density,
): Int {
    val innerWidthPx = (
        columnWidthPx -
            ((with(density) { 12.dp.toPx() } * notationScale * 2f).roundToInt())
        ).coerceAtLeast(0)
    val innerWidthDp = with(density) { innerWidthPx.toDp().value }
    val rowCount = layoutStaffRows(
        notation = notation,
        availableWidthDp = innerWidthDp,
        scale = notationScale,
    ).size.coerceAtLeast(1)
    val lineSpacingPx = with(density) { 12.dp.toPx() } * notationScale
    val rowGapPx = with(density) { 2.dp.toPx() } * notationScale
    val defaultStaffRowHeightPx = lineSpacingPx * 6.2f
    val canvasHeightPx = (defaultStaffRowHeightPx * rowCount) +
        (rowGapPx * (rowCount - 1).coerceAtLeast(0))
    val verticalPaddingPx = with(density) { 10.dp.toPx() } * notationScale * 2f
    val tempoHeightPx = notation.tempoBpm?.let { bpm ->
        measurePreviewTextHeightPx(
            text = "$bpm BPM",
            style = tempoTextStyle,
            maxWidthPx = columnWidthPx,
            textMeasurer = textMeasurer,
        ) + (with(density) { 6.dp.toPx() } * notationScale)
    } ?: 0f

    return (verticalPaddingPx + canvasHeightPx + tempoHeightPx).roundToInt()
}

internal fun measurePreviewColumnHeightPx(
    lineHeightsPx: List<Int>,
    lineSpacingPx: Int,
): Int {
    if (lineHeightsPx.isEmpty()) {
        return 0
    }
    return lineHeightsPx.sum() + (lineSpacingPx * (lineHeightsPx.size - 1))
}

internal fun splitPreviewLinesForHeightConstrainedColumns(
    lines: List<PreviewLine>,
    lineHeightsPx: List<Int>,
    maxFirstColumnHeightPx: Int,
    lineSpacingPx: Int,
): List<List<PreviewLine>> {
    if (lines.isEmpty()) {
        return listOf(emptyList())
    }
    require(lines.size == lineHeightsPx.size) {
        "Line heights must match preview lines."
    }

    var firstColumnHeightPx = 0
    var splitIndex = lines.size

    for (index in lines.indices) {
        val nextLineHeightPx = lineHeightsPx[index]
        val nextHeightPx = firstColumnHeightPx +
            nextLineHeightPx +
            if (index > 0) lineSpacingPx else 0

        if (index > 0 && nextHeightPx > maxFirstColumnHeightPx) {
            splitIndex = index
            break
        }

        firstColumnHeightPx = nextHeightPx
    }

    if (splitIndex >= lines.size) {
        return listOf(compactPreviewBlankLines(lines))
    }

    val leftColumn = compactPreviewBlankLines(lines.take(splitIndex))
    val rightColumn = compactPreviewBlankLines(lines.drop(splitIndex))
    return listOf(leftColumn, rightColumn).filter { it.isNotEmpty() }
}

private fun compactPreviewBlankLines(lines: List<PreviewLine>): List<PreviewLine> {
    val compacted = mutableListOf<PreviewLine>()
    var previousWasBlank = true

    lines.forEach { line ->
        if (line.type == PreviewLineType.Empty) {
            if (!previousWasBlank) {
                compacted += line
            }
            previousWasBlank = true
        } else {
            compacted += line
            previousWasBlank = false
        }
    }

    while (compacted.lastOrNull()?.type == PreviewLineType.Empty) {
        compacted.removeAt(compacted.lastIndex)
    }

    return compacted
}

internal fun protectSlashChordBreaks(
    text: String,
    accentSpans: List<PreviewSpan> = emptyList(),
): ProtectedChordText {
    if ('/' !in text) {
        return ProtectedChordText(
            text = text,
            accentSpans = accentSpans,
        )
    }

    val transformed = StringBuilder(text.length)
    val boundaryMap = IntArray(text.length + 1)

    text.forEachIndexed { index, character ->
        boundaryMap[index] = transformed.length
        val previous = text.getOrNull(index - 1)
        val next = text.getOrNull(index + 1)
        val isSlashChordBoundary = character == '/' &&
            previous != null &&
            next != null &&
            !previous.isWhitespace() &&
            !next.isWhitespace()

        if (isSlashChordBoundary) {
            transformed.append(WordJoiner)
            transformed.append(character)
            transformed.append(WordJoiner)
        } else {
            transformed.append(character)
        }
    }
    boundaryMap[text.length] = transformed.length

    return ProtectedChordText(
        text = transformed.toString(),
        accentSpans = accentSpans.map { span ->
            PreviewSpan(
                start = boundaryMap[span.start],
                endExclusive = boundaryMap[span.endExclusive],
            )
        },
    )
}

private fun buildSectionHeadingColors(
    previewLines: List<PreviewLine>,
    enabled: Boolean,
    defaultColor: Color,
): Map<String, Color> {
    if (!enabled) {
        return emptyMap()
    }

    val colorsByGroup = linkedMapOf<String, Color>()
    previewLines.forEach { line ->
        val group = line.sectionColorGroup ?: return@forEach
        if (group !in colorsByGroup) {
            colorsByGroup[group] = KnownSectionHeadingColors[group]
                ?: FallbackSectionHeadingPalette.getOrElse(
                    group.hashCode().mod(FallbackSectionHeadingPalette.size),
                ) { defaultColor }
        }
    }
    return colorsByGroup
}
