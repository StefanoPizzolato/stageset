package com.codex.stageset.ui.common

import com.codex.stageset.chart.ChordPlacement
import com.codex.stageset.chart.MelodyNotation
import com.codex.stageset.chart.MelodyParseResult
import com.codex.stageset.chart.canonicalSectionLine
import com.codex.stageset.chart.looksLikeChordLine
import com.codex.stageset.chart.parseChordPlacements
import com.codex.stageset.chart.parseMelodyNotation
import com.codex.stageset.chart.sectionColorGroup
import kotlin.math.abs
import kotlin.math.roundToInt

data class PreviewRenderOptions(
    val showLyrics: Boolean = true,
    val showLyricsCue: Boolean = false,
    val showChords: Boolean = true,
    val showNotation: Boolean = true,
    val hideRepeatedSectionChords: Boolean = false,
    val compressChords: Boolean = false,
    val colorizeSectionHeadings: Boolean = false,
    val twoColumns: Boolean = false,
)

data class PreviewSpan(
    val start: Int,
    val endExclusive: Int,
)

data class PreviewLine(
    val text: String,
    val type: PreviewLineType,
    val accentSpans: List<PreviewSpan> = emptyList(),
    val sectionColorGroup: String? = null,
    val melodyNotation: MelodyNotation? = null,
)

enum class PreviewLineType {
    Section,
    Chord,
    Lyric,
    LyricCue,
    Melody,
    MelodyError,
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
    val compressChordOnly = options.compressChords && options.showChords && !options.showLyrics
    val shouldCollapseRepeatedSections = options.hideRepeatedSectionChords || compressChordOnly
    val seenChordSignatures = mutableMapOf<String, MutableSet<String>>()
    val renderedSections = sections.mapNotNull { section ->
        val shouldHideChords = shouldCollapseRepeatedSections &&
            section.repeatFamilyKey != null &&
            section.chordSignature.isNotBlank() &&
            !seenChordSignatures
                .getOrPut(section.repeatFamilyKey) { mutableSetOf() }
                .add(section.chordSignature)

        renderSection(
            section = section,
            showLyrics = options.showLyrics,
            showLyricsCue = options.showLyricsCue,
            showChords = options.showChords,
            showNotation = options.showNotation,
            hideChordLines = shouldHideChords,
            compressChords = compressChordOnly,
            denseChordSpacing = options.twoColumns,
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
    val repeatFamilyKey: String?,
    val sectionColorGroup: String?,
    val entries: List<ChartSectionEntry>,
    val chordSignature: String,
)

private sealed interface ChartSectionEntry {
    data class TextLine(val text: String) : ChartSectionEntry
    data class MelodyBlock(val source: String) : ChartSectionEntry
}

private data class ChordSlotLayout(
    val slotStarts: List<Int>,
)

private data class StyledPreviewText(
    val text: String,
    val accentSpans: List<PreviewSpan> = emptyList(),
)

private data class CompressedChordToken(
    val visualWidth: Int,
    val styledText: StyledPreviewText,
)

private data class RepeatMatch(
    val motifLength: Int,
    val count: Int,
)

private fun parseSections(chart: String): List<ChartSection> {
    val sections = mutableListOf<ChartSection>()
    var currentHeader: String? = null
    var currentEntries = mutableListOf<ChartSectionEntry>()
    var inMelodyBlock = false
    var melodyBlockLines = mutableListOf<String>()

    fun commitSection() {
        if (currentHeader == null && currentEntries.isEmpty()) {
            return
        }

        val normalizedHeader = currentHeader?.removePrefix("[")?.removeSuffix("]")?.lowercase()
        val chordSignature = buildChordSignature(currentEntries)
        val sectionFamilyKey = currentHeader?.let(::sectionColorGroup) ?: normalizedHeader

        sections += ChartSection(
            header = currentHeader,
            normalizedHeader = normalizedHeader,
            repeatFamilyKey = sectionFamilyKey,
            sectionColorGroup = currentHeader?.let(::sectionColorGroup),
            entries = currentEntries.toList(),
            chordSignature = chordSignature,
        )

        currentEntries = mutableListOf()
    }

    fun commitMelodyBlock() {
        currentEntries += ChartSectionEntry.MelodyBlock(
            source = melodyBlockLines.joinToString("\n").trim(),
        )
        melodyBlockLines = mutableListOf()
        inMelodyBlock = false
    }

    chart.lines().forEach { rawLine ->
        val line = rawLine.trimEnd()
        val trimmed = line.trim()

        if (inMelodyBlock) {
            if (trimmed == "@") {
                commitMelodyBlock()
            } else {
                melodyBlockLines += line
            }
            return@forEach
        }

        inlineMelodyBlockSource(trimmed)?.let { melodySource ->
            currentEntries += ChartSectionEntry.MelodyBlock(melodySource)
            return@forEach
        }

        if (trimmed == "@") {
            inMelodyBlock = true
            melodyBlockLines = mutableListOf()
            return@forEach
        }

        val sectionLine = canonicalSectionLine(line)
        if (sectionLine != null) {
            commitSection()
            currentHeader = sectionLine
        } else {
            currentEntries += ChartSectionEntry.TextLine(line)
        }
    }

    if (inMelodyBlock) {
        commitMelodyBlock()
    }

    commitSection()
    return sections
}

private fun renderSection(
    section: ChartSection,
    showLyrics: Boolean,
    showLyricsCue: Boolean,
    showChords: Boolean,
    showNotation: Boolean,
    hideChordLines: Boolean,
    compressChords: Boolean,
    denseChordSpacing: Boolean,
): List<PreviewLine> {
    if (compressChords && showChords) {
        return renderCompressedChordSection(
            section = section,
            showLyricsCue = showLyricsCue,
            showNotation = showNotation,
            hideChordLines = hideChordLines,
            denseChordSpacing = denseChordSpacing,
        )
    }

    val chordSlotLayout = if (showChords && !showLyrics) {
        buildChordSlotLayout(
            section.entries.mapNotNull { entry ->
                (entry as? ChartSectionEntry.TextLine)?.text?.let(::parsePreviewChordPlacements)
            },
            denseSpacing = denseChordSpacing,
        )
    } else {
        null
    }

    val rendered = buildList {
        section.header?.let {
            add(
                PreviewLine(
                    text = it,
                    type = PreviewLineType.Section,
                    sectionColorGroup = section.sectionColorGroup,
                ),
            )
        }
        if (showLyricsCue) {
            buildLyricsCue(section.entries)?.let { cue ->
                add(PreviewLine(text = cue, type = PreviewLineType.LyricCue))
            }
        }

        section.entries.forEach { entry ->
            when (entry) {
                is ChartSectionEntry.MelodyBlock -> {
                    if (showNotation) {
                        add(buildMelodyPreviewLine(entry.source))
                    }
                }
                is ChartSectionEntry.TextLine -> {
                    val line = entry.text
                    val placements = parsePreviewChordPlacements(line)
                    when {
                        line.isBlank() -> add(PreviewLine(text = "", type = PreviewLineType.Empty))
                        placements != null && showChords && !hideChordLines -> {
                            add(
                                PreviewLine(
                                    text = when {
                                        showLyrics && denseChordSpacing -> renderCompactedChordLine(
                                            placements = placements,
                                            denseSpacing = true,
                                        )
                                        showLyrics -> line
                                        else -> renderColumnizedChordLine(
                                            placements = placements,
                                            slotLayout = chordSlotLayout,
                                        )
                                    },
                                    type = PreviewLineType.Chord,
                                    sectionColorGroup = section.sectionColorGroup,
                                ),
                            )
                        }
                        placements == null && showLyrics -> {
                            add(PreviewLine(text = line, type = PreviewLineType.Lyric))
                        }
                    }
                }
            }
        }
    }

    return compactBlankLines(rendered)
}

private fun renderCompressedChordSection(
    section: ChartSection,
    showLyricsCue: Boolean,
    showNotation: Boolean,
    hideChordLines: Boolean,
    denseChordSpacing: Boolean,
): List<PreviewLine> {
    val rendered = buildList {
        val pendingChordLines = mutableListOf<String>()

        fun flushPendingChordLines() {
            if (pendingChordLines.isEmpty()) {
                return
            }
            if (!hideChordLines) {
                addAll(
                    buildCompressedChordPreviewLines(
                        lines = pendingChordLines.toList(),
                        denseSpacing = denseChordSpacing,
                        sectionColorGroup = section.sectionColorGroup,
                    ),
                )
            }
            pendingChordLines.clear()
        }

        section.header?.let {
            add(
                PreviewLine(
                    text = it,
                    type = PreviewLineType.Section,
                    sectionColorGroup = section.sectionColorGroup,
                ),
            )
        }
        if (showLyricsCue) {
            buildLyricsCue(section.entries)?.let { cue ->
                add(PreviewLine(text = cue, type = PreviewLineType.LyricCue))
            }
        }

        section.entries.forEach { entry ->
            when (entry) {
                is ChartSectionEntry.MelodyBlock -> {
                    flushPendingChordLines()
                    if (showNotation) {
                        add(buildMelodyPreviewLine(entry.source))
                    }
                }

                is ChartSectionEntry.TextLine -> {
                    val placements = parsePreviewChordPlacements(entry.text)
                    if (placements != null) {
                        pendingChordLines += entry.text
                    }
                }
            }
        }

        flushPendingChordLines()
    }
    return compactBlankLines(rendered)
}

private fun buildChordSignature(entries: List<ChartSectionEntry>): String {
    return entries.asSequence()
        .mapNotNull { entry -> (entry as? ChartSectionEntry.TextLine)?.text }
        .mapNotNull(::parsePreviewChordPlacements)
        .flatMap { placements -> placements.asSequence().map { it.chord } }
        .joinToString(" ")
        .trim()
}

private fun buildLyricsCue(entries: List<ChartSectionEntry>): String? {
    val words = entries.asSequence()
        .mapNotNull { entry -> (entry as? ChartSectionEntry.TextLine)?.text }
        .filter { text ->
            text.isNotBlank() && parsePreviewChordPlacements(text) == null
        }
        .flatMap { text ->
            text.trim()
                .split(Regex("""\s+"""))
                .asSequence()
                .filter { it.isNotBlank() }
        }
        .take(4)
        .toList()

    return words.takeIf { it.isNotEmpty() }
        ?.joinToString(" ", postfix = "...")
}

private fun inlineMelodyBlockSource(line: String): String? {
    if (!line.startsWith("@") || !line.endsWith("@") || line.length <= 1) {
        return null
    }
    return line.removePrefix("@")
        .removeSuffix("@")
        .trim()
        .takeIf { it.isNotEmpty() }
}

private fun buildMelodyPreviewLine(source: String): PreviewLine {
    return when (val result = parseMelodyNotation(source)) {
        is MelodyParseResult.Success -> {
            PreviewLine(
                text = source,
                type = PreviewLineType.Melody,
                melodyNotation = result.notation,
            )
        }

        is MelodyParseResult.Error -> {
            PreviewLine(
                text = "Melody notation error: ${result.message}",
                type = PreviewLineType.MelodyError,
            )
        }
    }
}

private fun parsePreviewChordPlacements(line: String): List<ChordPlacement>? {
    if (line.isBlank() || !looksLikeChordLine(line)) {
        return null
    }
    return parseChordPlacements(line)?.takeIf { it.isNotEmpty() }
}

private fun buildChordSlotLayout(
    lines: List<List<ChordPlacement>>,
    denseSpacing: Boolean,
): ChordSlotLayout? {
    if (lines.isEmpty()) {
        return null
    }

    val slotCount = lines.maxOf { it.size }
    val slotStarts = MutableList(slotCount) { 0 }
    val maxChordLengths = MutableList(slotCount) { slotIndex ->
        lines.maxOfOrNull { placements -> placements.getOrNull(slotIndex)?.chord?.length ?: 0 } ?: 0
    }

    for (slotIndex in 1 until slotCount) {
        val rawGaps = lines.mapNotNull { placements ->
            val previous = placements.getOrNull(slotIndex - 1) ?: return@mapNotNull null
            val current = placements.getOrNull(slotIndex) ?: return@mapNotNull null
            (current.start - (previous.start + previous.chord.length)).coerceAtLeast(1)
        }
        val representativeGap = representativeGap(rawGaps, denseSpacing = denseSpacing)
        slotStarts[slotIndex] = slotStarts[slotIndex - 1] + maxChordLengths[slotIndex - 1] + representativeGap
    }

    return ChordSlotLayout(slotStarts = slotStarts)
}

private fun representativeGap(rawGaps: List<Int>, denseSpacing: Boolean): Int {
    if (rawGaps.isEmpty()) {
        return if (denseSpacing) 2 else 4
    }

    val sorted = rawGaps.sorted()
    val medianRawGap = if (sorted.size % 2 == 1) {
        sorted[sorted.lastIndex / 2]
    } else {
        ((sorted[sorted.size / 2] + sorted[(sorted.size / 2) - 1]) / 2f).roundToInt()
    }

    return compactChordGap(medianRawGap, denseSpacing = denseSpacing)
}

private fun renderColumnizedChordLine(
    placements: List<ChordPlacement>,
    slotLayout: ChordSlotLayout?,
): String {
    if (placements.isEmpty()) {
        return ""
    }
    if (slotLayout == null) {
        return placements.joinToString("  ") { it.chord }
    }

    val builder = StringBuilder()
    var renderedColumn = 0

    placements.forEachIndexed { index, placement ->
        val preferredSlotStart = slotLayout.slotStarts.getOrElse(index) { renderedColumn }
        val slotStart = if (index == 0) {
            preferredSlotStart
        } else {
            maxOf(preferredSlotStart, renderedColumn + 2)
        }
        renderedColumn = appendSpacingToColumn(
            builder = builder,
            currentColumn = renderedColumn,
            targetColumn = slotStart,
        )
        builder.append(placement.chord)
        renderedColumn = slotStart + placement.chord.length
    }

    return builder.toString().trimEnd()
}

private fun renderCompactedChordLine(
    placements: List<ChordPlacement>,
    denseSpacing: Boolean,
): String {
    if (placements.isEmpty()) {
        return ""
    }

    val builder = StringBuilder()
    builder.append(placements.first().chord)
    var renderedColumn = placements.first().chord.length

    for (index in 1 until placements.size) {
        val previous = placements[index - 1]
        val current = placements[index]
        val rawGap = (current.start - (previous.start + previous.chord.length)).coerceAtLeast(1)
        val nextColumn = renderedColumn + compactChordGap(rawGap, denseSpacing = denseSpacing)
        renderedColumn = appendSpacingToColumn(
            builder = builder,
            currentColumn = renderedColumn,
            targetColumn = nextColumn,
        )
        builder.append(current.chord)
        renderedColumn += current.chord.length
    }

    return builder.toString().trimEnd()
}

private fun buildCompressedChordPreviewLines(
    lines: List<String>,
    denseSpacing: Boolean,
    sectionColorGroup: String?,
): List<PreviewLine> {
    val chordTokens = collapseConsecutiveDuplicateChordSymbols(
        lines.asSequence()
            .mapNotNull(::parsePreviewChordPlacements)
            .flatMap { placements ->
                collapseConsecutiveDuplicateChords(placements)
                    .asSequence()
                    .map(ChordPlacement::chord)
            }
            .toList(),
    )

    if (chordTokens.isEmpty()) {
        return emptyList()
    }

    val compressedSegments = mutableListOf<CompressedChordToken>()
    var index = 0
    while (index < chordTokens.size) {
        val repeatMatch = findShortestRepeatMatch(chordTokens, startIndex = index)
        if (repeatMatch != null) {
            val motif = chordTokens.subList(index, index + repeatMatch.motifLength)
            val styledText = buildRepeatedChordToken(
                chordText = compressChordTextSequence(motif, denseSpacing = denseSpacing),
                count = repeatMatch.count,
            )
            compressedSegments += CompressedChordToken(
                visualWidth = visualWidth(styledText.text),
                styledText = styledText,
            )
            index += repeatMatch.motifLength * repeatMatch.count
            continue
        }

        val runStart = index
        index++
        while (index < chordTokens.size && findShortestRepeatMatch(chordTokens, startIndex = index) == null) {
            index++
        }

        val styledText = StyledPreviewText(
            text = compressChordTextSequence(
                chords = chordTokens.subList(runStart, index),
                denseSpacing = denseSpacing,
            ),
        )
        compressedSegments += CompressedChordToken(
            visualWidth = visualWidth(styledText.text),
            styledText = styledText,
        )
    }

    return packCompressedChordTokens(
        tokens = compressedSegments,
        denseSpacing = denseSpacing,
    ).map { styledText ->
        PreviewLine(
            text = styledText.text,
            type = PreviewLineType.Chord,
            accentSpans = styledText.accentSpans,
            sectionColorGroup = sectionColorGroup,
        )
    }
}

private fun compressChordSequence(placements: List<ChordPlacement>): String {
    return compressChordTextSequence(placements.map(ChordPlacement::chord))
}

private fun compressChordTextSequence(
    chords: List<String>,
    denseSpacing: Boolean = false,
): String {
    if (chords.isEmpty()) {
        return ""
    }

    val builder = StringBuilder()
    builder.append(chords.first())

    for (index in 1 until chords.size) {
        val previous = chords[index - 1]
        val current = chords[index]
        val separator = when {
            denseSpacing && maxOf(previous.length, current.length) >= 3 -> "  "
            denseSpacing -> " "
            maxOf(previous.length, current.length) >= 3 -> "    "
            else -> "  "
        }
        builder.append(separator)
        builder.append(current)
    }

    return builder.toString()
}

private fun collapseConsecutiveDuplicateChords(
    placements: List<ChordPlacement>,
): List<ChordPlacement> {
    if (placements.isEmpty()) {
        return emptyList()
    }

    val collapsed = mutableListOf<ChordPlacement>()
    placements.forEach { placement ->
        if (collapsed.lastOrNull()?.chord != placement.chord) {
            collapsed += placement
        }
    }
    return collapsed
}

private fun collapseConsecutiveDuplicateChordSymbols(chords: List<String>): List<String> {
    if (chords.isEmpty()) {
        return emptyList()
    }

    val collapsed = mutableListOf<String>()
    chords.forEach { chord ->
        if (collapsed.lastOrNull() != chord) {
            collapsed += chord
        }
    }
    return collapsed
}

private fun findShortestRepeatMatch(
    chords: List<String>,
    startIndex: Int,
): RepeatMatch? {
    val remaining = chords.size - startIndex
    if (remaining < 2) {
        return null
    }

    for (motifLength in 1..(remaining / 2)) {
        val motif = chords.subList(startIndex, startIndex + motifLength)
        var count = 1
        var probeIndex = startIndex + motifLength
        while (probeIndex + motifLength <= chords.size &&
            chords.subList(probeIndex, probeIndex + motifLength) == motif
        ) {
            count++
            probeIndex += motifLength
        }

        if (count >= 2) {
            return RepeatMatch(motifLength = motifLength, count = count)
        }
    }

    return null
}

private fun buildRepeatedChordToken(
    chordText: String,
    count: Int,
): StyledPreviewText {
    val prefix = ":"
    val suffix = ": x$count"
    val fullText = "$prefix$chordText$suffix"
    val closingColonIndex = prefix.length + chordText.length
    val multiplierStart = closingColonIndex + 2

    return StyledPreviewText(
        text = fullText,
        accentSpans = listOf(
            PreviewSpan(start = 0, endExclusive = 1),
            PreviewSpan(start = closingColonIndex, endExclusive = closingColonIndex + 1),
            PreviewSpan(start = multiplierStart, endExclusive = fullText.length),
        ),
    )
}

private fun packCompressedChordTokens(
    tokens: List<CompressedChordToken>,
    denseSpacing: Boolean,
): List<StyledPreviewText> {
    if (tokens.isEmpty()) {
        return emptyList()
    }

    val packedLines = mutableListOf<StyledPreviewText>()
    val separator = if (denseSpacing) "  " else "   "
    val separatorWidth = visualWidth(separator)
    val maxVisualWidth = if (denseSpacing) 28 else 64

    var currentText = StringBuilder()
    val currentSpans = mutableListOf<PreviewSpan>()
    var currentWidth = 0

    fun commitLine() {
        if (currentText.isNotEmpty()) {
            packedLines += StyledPreviewText(
                text = currentText.toString(),
                accentSpans = currentSpans.toList(),
            )
            currentText = StringBuilder()
            currentSpans.clear()
            currentWidth = 0
        }
    }

    tokens.forEach { token ->
        val needsSeparator = currentText.isNotEmpty()
        val projectedWidth = currentWidth + (if (needsSeparator) separatorWidth else 0) + token.visualWidth
        if (needsSeparator && projectedWidth > maxVisualWidth) {
            commitLine()
        }

        if (currentText.isNotEmpty()) {
            currentText.append(separator)
            currentWidth += separatorWidth
        }

        val offset = currentText.length
        currentText.append(token.styledText.text)
        currentSpans += token.styledText.accentSpans.map { span ->
            PreviewSpan(
                start = span.start + offset,
                endExclusive = span.endExclusive + offset,
            )
        }
        currentWidth += token.visualWidth
    }

    commitLine()
    return packedLines
}

private fun compactChordGap(rawGap: Int, denseSpacing: Boolean): Int = when {
    denseSpacing && rawGap <= 4 -> 2
    denseSpacing && rawGap <= 8 -> 4
    denseSpacing && rawGap <= 14 -> 5
    denseSpacing && rawGap <= 20 -> 6
    denseSpacing -> 8
    rawGap <= 1 -> 2
    rawGap <= 4 -> 4
    rawGap <= 8 -> 6
    rawGap <= 14 -> 8
    rawGap <= 20 -> 10
    else -> 12
}

private fun appendSpacingToColumn(
    builder: StringBuilder,
    currentColumn: Int,
    targetColumn: Int,
): Int {
    var column = currentColumn
    val safeTarget = targetColumn.coerceAtLeast(currentColumn)

    while (nextTabStop(column) <= safeTarget) {
        builder.append('\t')
        column = nextTabStop(column)
    }
    while (column < safeTarget) {
        builder.append(' ')
        column++
    }

    return column
}

private fun visualWidth(text: String): Int {
    var column = 0
    text.forEach { character ->
        column = if (character == '\t') nextTabStop(column) else column + 1
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

fun splitPreviewLinesForTwoColumns(lines: List<PreviewLine>): List<List<PreviewLine>> {
    if (lines.isEmpty()) {
        return listOf(emptyList())
    }

    val blocks = mutableListOf<List<PreviewLine>>()
    var currentBlock = mutableListOf<PreviewLine>()

    lines.forEach { line ->
        currentBlock += line
        if (line.type == PreviewLineType.Empty) {
            blocks += currentBlock.toList()
            currentBlock = mutableListOf()
        }
    }

    if (currentBlock.isNotEmpty()) {
        blocks += currentBlock.toList()
    }

    if (blocks.size <= 1) {
        return listOf(compactBlankLines(lines))
    }

    val blockWeights = blocks.map { block ->
        block.count { it.type != PreviewLineType.Empty }.coerceAtLeast(1)
    }
    val totalWeight = blockWeights.sum()
    var runningWeight = 0
    var bestSplitIndex = 1
    var bestDifference = Int.MAX_VALUE

    for (index in 1 until blocks.size) {
        runningWeight += blockWeights[index - 1]
        val difference = abs((totalWeight - runningWeight) - runningWeight)
        if (difference < bestDifference) {
            bestDifference = difference
            bestSplitIndex = index
        }
    }

    val leftColumn = compactBlankLines(blocks.take(bestSplitIndex).flatten())
    val rightColumn = compactBlankLines(blocks.drop(bestSplitIndex).flatten())
    return listOf(leftColumn, rightColumn).filter { it.isNotEmpty() }
}
