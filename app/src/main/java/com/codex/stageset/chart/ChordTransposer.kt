package com.codex.stageset.chart

enum class TransposeDirection(
    val semitoneOffset: Int,
    private val noteNames: List<String>,
) {
    Up(
        semitoneOffset = 1,
        noteNames = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"),
    ),
    Down(
        semitoneOffset = -1,
        noteNames = listOf("C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B"),
    ),
    ;

    fun noteNameFor(semitone: Int): String = noteNames[semitone.mod(12)]
}

private val LeadingNoteRegex = Regex("""^([A-G](?:#|b)?)(.*)$""")
private val SlashBassRegex = Regex("""/([A-G](?:#|b)?)""")
private val NoteSemitones = mapOf(
    "C" to 0,
    "B#" to 0,
    "C#" to 1,
    "Db" to 1,
    "D" to 2,
    "D#" to 3,
    "Eb" to 3,
    "E" to 4,
    "Fb" to 4,
    "E#" to 5,
    "F" to 5,
    "F#" to 6,
    "Gb" to 6,
    "G" to 7,
    "G#" to 8,
    "Ab" to 8,
    "A" to 9,
    "A#" to 10,
    "Bb" to 10,
    "B" to 11,
    "Cb" to 11,
)

fun transposeChart(
    chart: String,
    direction: TransposeDirection,
): String {
    if (chart.isBlank()) {
        return chart
    }

    return chart.lines()
        .joinToString("\n") { line -> transposeChordLine(line, direction) }
}

fun transposeKeySignature(
    keySignature: String,
    direction: TransposeDirection,
): String {
    val trimmed = keySignature.trim()
    if (trimmed.isBlank()) {
        return keySignature
    }

    val transposed = transposeChordSymbol(trimmed, direction) ?: return keySignature
    return keySignature.replaceRange(
        startIndex = keySignature.indexOf(trimmed),
        endIndex = keySignature.indexOf(trimmed) + trimmed.length,
        replacement = transposed,
    )
}

fun transposeChordLine(
    line: String,
    direction: TransposeDirection,
): String {
    val placements = parseChordPlacements(line)?.takeIf { it.isNotEmpty() } ?: return line
    val transposedChords = placements.map { placement ->
        transposeChordSymbol(placement.chord, direction) ?: placement.chord
    }

    val builder = StringBuilder()
    var renderedColumn = 0

    placements.forEachIndexed { index, placement ->
        val originalColumn = visualWidth(line.take(placement.start))
        val targetColumn = if (index == 0) {
            originalColumn
        } else {
            maxOf(originalColumn, renderedColumn + 2)
        }
        renderedColumn = appendSpacingToColumn(
            builder = builder,
            currentColumn = renderedColumn,
            targetColumn = targetColumn,
        )

        val chordText = transposedChords[index]
        builder.append(chordText)
        renderedColumn = targetColumn + chordText.length
    }

    return builder.toString().trimEnd()
}

fun transposeChordSymbol(
    chord: String,
    direction: TransposeDirection,
): String? {
    if (chord.equals("N.C.", ignoreCase = true) || chord.equals("NC", ignoreCase = true)) {
        return chord
    }

    val match = LeadingNoteRegex.matchEntire(chord) ?: return null
    val root = match.groupValues[1]
    val suffix = match.groupValues[2]
    val transposedRoot = transposeNoteName(root, direction) ?: return null
    val transposedSuffix = SlashBassRegex.replace(suffix) { bassMatch ->
        val bassNote = bassMatch.groupValues[1]
        "/${transposeNoteName(bassNote, direction) ?: bassNote}"
    }
    return transposedRoot + transposedSuffix
}

private fun transposeNoteName(
    note: String,
    direction: TransposeDirection,
): String? {
    val semitone = NoteSemitones[note] ?: return null
    return direction.noteNameFor(semitone + direction.semitoneOffset)
}

private fun appendSpacingToColumn(
    builder: StringBuilder,
    currentColumn: Int,
    targetColumn: Int,
): Int {
    var column = currentColumn
    val safeTarget = targetColumn.coerceAtLeast(currentColumn)

    while (column < safeTarget) {
        builder.append(' ')
        column++
    }

    return column
}

private fun visualWidth(text: String): Int = text.length
