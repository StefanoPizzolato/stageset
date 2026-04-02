package com.codex.stageset.ui.common

import com.codex.stageset.chart.canonicalSectionLine
import com.codex.stageset.chart.looksLikeChordLine
import com.codex.stageset.chart.parseChordPlacements

data class PreviewRenderOptions(
    val showLyrics: Boolean = true,
    val hideRepeatedSectionChords: Boolean = false,
)

data class PreviewLine(
    val text: String,
    val type: PreviewLineType,
)

enum class PreviewLineType {
    Section,
    Chord,
    Lyric,
    Empty,
}

fun buildPreviewLines(
    chart: String,
    options: PreviewRenderOptions = PreviewRenderOptions(),
): List<PreviewLine> {
    if (chart.isBlank()) {
        return emptyList()
    }

    val sections = parseSections(chart)
    val seenChordSignatures = mutableMapOf<String, MutableSet<String>>()
    val renderedSections = sections.mapNotNull { section ->
        val shouldHideChords = options.hideRepeatedSectionChords &&
            section.normalizedHeader != null &&
            section.chordSignature.isNotBlank() &&
            !seenChordSignatures
                .getOrPut(section.normalizedHeader) { mutableSetOf() }
                .add(section.chordSignature)

        renderSection(
            section = section,
            showLyrics = options.showLyrics,
            hideChordLines = shouldHideChords,
        ).takeIf { it.isNotEmpty() }
    }

    val rendered = mutableListOf<PreviewLine>()
    renderedSections.forEachIndexed { index, lines ->
        if (index > 0 && rendered.lastOrNull()?.type != PreviewLineType.Empty) {
            rendered += PreviewLine(text = "", type = PreviewLineType.Empty)
        }
        rendered += lines
    }

    return compactBlankLines(rendered)
}

private data class ChartSection(
    val header: String?,
    val normalizedHeader: String?,
    val lines: List<String>,
    val chordSignature: String,
)

private fun parseSections(chart: String): List<ChartSection> {
    val sections = mutableListOf<ChartSection>()
    var currentHeader: String? = null
    var currentLines = mutableListOf<String>()

    fun commitSection() {
        if (currentHeader == null && currentLines.isEmpty()) {
            return
        }

        val normalizedHeader = currentHeader?.removePrefix("[")?.removeSuffix("]")?.lowercase()
        val chordSignature = buildChordSignature(currentLines)

        sections += ChartSection(
            header = currentHeader,
            normalizedHeader = normalizedHeader,
            lines = currentLines.toList(),
            chordSignature = chordSignature,
        )

        currentLines = mutableListOf()
    }

    chart.lines().forEach { rawLine ->
        val line = rawLine.trimEnd()
        val sectionLine = canonicalSectionLine(line)
        if (sectionLine != null) {
            commitSection()
            currentHeader = sectionLine
        } else {
            currentLines += line
        }
    }

    commitSection()
    return sections
}

private fun renderSection(
    section: ChartSection,
    showLyrics: Boolean,
    hideChordLines: Boolean,
): List<PreviewLine> {
    val rendered = buildList {
        section.header?.let { add(PreviewLine(text = it, type = PreviewLineType.Section)) }

        section.lines.forEach { line ->
            when {
                line.isBlank() -> add(PreviewLine(text = "", type = PreviewLineType.Empty))
                looksLikeChordLine(line) && !hideChordLines -> {
                    add(
                        PreviewLine(
                            text = if (showLyrics) line else compactChordLine(line),
                            type = PreviewLineType.Chord,
                        ),
                    )
                }
                !looksLikeChordLine(line) && showLyrics -> {
                    add(PreviewLine(text = line, type = PreviewLineType.Lyric))
                }
            }
        }
    }

    return compactBlankLines(rendered)
}

private fun buildChordSignature(lines: List<String>): String {
    return lines.asSequence()
        .filter(::looksLikeChordLine)
        .flatMap { line ->
            parseChordPlacements(line).orEmpty().asSequence().map { placement -> placement.chord }
        }
        .joinToString(" ")
        .trim()
}

private fun compactChordLine(line: String): String {
    val placements = parseChordPlacements(line).orEmpty()
    if (placements.isEmpty()) {
        return ""
    }
    if (placements.size == 1) {
        return placements.first().chord
    }

    val builder = StringBuilder()
    builder.append(placements.first().chord)
    var renderedColumn = placements.first().chord.length

    for (index in 1 until placements.size) {
        val previous = placements[index - 1]
        val current = placements[index]
        val rawGap = (current.start - (previous.start + previous.chord.length)).coerceAtLeast(1)
        val compactGap = compactChordGap(rawGap)
        renderedColumn = appendCompactSpacing(
            builder = builder,
            currentColumn = renderedColumn,
            targetColumn = renderedColumn + compactGap,
        )
        builder.append(current.chord)
        renderedColumn += current.chord.length
    }

    return builder.toString().trimEnd()
}

private fun compactChordGap(rawGap: Int): Int = when {
    rawGap <= 1 -> 2
    rawGap <= 4 -> 4
    rawGap <= 8 -> 6
    rawGap <= 14 -> 8
    rawGap <= 20 -> 10
    else -> 12
}

private fun appendCompactSpacing(
    builder: StringBuilder,
    currentColumn: Int,
    targetColumn: Int,
): Int {
    var column = currentColumn

    while (nextTabStop(column) <= targetColumn) {
        builder.append('\t')
        column = nextTabStop(column)
    }
    while (column < targetColumn) {
        builder.append(' ')
        column++
    }

    return column
}

private fun nextTabStop(column: Int): Int {
    val tabWidth = 8
    return ((column / tabWidth) + 1) * tabWidth
}

private fun compactBlankLines(lines: List<PreviewLine>): List<PreviewLine> {
    val compacted = mutableListOf<PreviewLine>()
    var previousWasBlank = true

    lines.forEach { line ->
        if (line.type == PreviewLineType.Empty) {
            if (!previousWasBlank) {
                compacted += PreviewLine(text = "", type = PreviewLineType.Empty)
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
