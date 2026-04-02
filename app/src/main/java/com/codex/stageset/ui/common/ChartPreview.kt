package com.codex.stageset.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
                } else {
                    previewLines.forEach { line ->
                        when (line.type) {
                            PreviewLineType.Section -> {
                                Text(
                                    text = line.text,
                                    style = sectionTextStyle,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.secondary,
                                )
                            }

                            PreviewLineType.Chord -> {
                                Text(
                                    text = line.text,
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
        }
    }
}
