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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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

@Composable
fun ChartPreview(
    title: String,
    artist: String,
    keySignature: String,
    chart: String,
    showHeader: Boolean = true,
    textSizeSp: Int = 16,
    previewOptions: PreviewRenderOptions = PreviewRenderOptions(),
    modifier: Modifier = Modifier,
) {
    val previewLines = remember(chart, previewOptions) { buildPreviewLines(chart, previewOptions) }
    val scrollState = rememberScrollState()
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
    val sectionTextStyle = MaterialTheme.typography.titleMedium.copy(
        fontSize = (textSizeSp * 0.9f).sp,
        lineHeight = (textSizeSp * 1.2f).sp,
    )

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
                val lineColumns = remember(previewLines, useTwoColumns) {
                    if (useTwoColumns) {
                        splitPreviewLinesForTwoColumns(previewLines)
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
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (artist.isNotBlank() || keySignature.isNotBlank()) {
                            Text(
                                text = listOfNotNull(
                                    artist.takeIf { it.isNotBlank() },
                                    keySignature.takeIf { it.isNotBlank() }?.let { "Key $it" },
                                ).joinToString(" - "),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (chart.isBlank()) {
                        Text(
                            text = "Your lyrics and chord chart will preview here.",
                            style = chartTextStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else if (previewLines.isEmpty()) {
                        Text(
                            text = "No section markers or chord lines are visible with the current preview settings.",
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
                    Text(
                        text = buildAnnotatedString {
                            append(line.text)
                            line.accentSpans.forEach { span ->
                                addStyle(
                                    SpanStyle(
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontWeight = FontWeight.SemiBold,
                                    ),
                                    start = span.start,
                                    end = span.endExclusive,
                                )
                            }
                        },
                        style = chartTextStyle,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary,
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
