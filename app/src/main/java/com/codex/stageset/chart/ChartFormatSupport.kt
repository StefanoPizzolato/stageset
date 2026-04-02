package com.codex.stageset.chart

private val SectionLabelMap = linkedMapOf(
    "pre chorus" to "Pre-Chorus",
    "post chorus" to "Post-Chorus",
    "instrumental" to "Instrumental",
    "interlude" to "Interlude",
    "turnaround" to "Turnaround",
    "refrain" to "Refrain",
    "chorus" to "Chorus",
    "bridge" to "Bridge",
    "verse" to "Verse",
    "intro" to "Intro",
    "outro" to "Outro",
    "ending" to "Ending",
    "hook" to "Hook",
    "solo" to "Solo",
    "tag" to "Tag",
)

data class ChordPlacement(
    val chord: String,
    val start: Int,
)

private val ChordSymbolRegex = Regex(
    pattern = """
        ^(?:N\.C\.|NC|[A-G](?:#|b)?(?:maj|min|m|sus|dim|aug|add|no|omit|M)?(?:
            \d+|
            [#b]\d+|
            sus\d*|
            add\d+|
            maj\d+|
            m\d+|
            dim\d*|
            aug\d*|
            no\d+|
            omit\d+
        )*(?:\([^)]*\))*(?:/[A-G](?:#|b)?)?)$
    """.trimIndent().replace(Regex("\\s+"), ""),
)

fun canonicalSectionLine(rawLine: String): String? {
    val trimmed = rawLine.trim()
    if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
        return null
    }

    val label = trimmed.removePrefix("[").removeSuffix("]")
    return canonicalSectionLabel(label)?.let { "[$it]" }
}

fun sectionColorGroup(rawSectionLine: String): String? {
    val trimmed = rawSectionLine.trim()
    if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
        return null
    }

    val label = trimmed.removePrefix("[").removeSuffix("]")
    return resolveSectionEntry(label)?.key
}

fun canonicalSectionLabel(rawLabel: String): String? {
    val entry = resolveSectionEntry(rawLabel) ?: return null
    val normalized = normalizeSectionLabel(rawLabel)

    val suffix = normalized.removePrefix(entry.key).trim()
    return if (suffix.isBlank()) {
        entry.value
    } else {
        "${entry.value} ${formatSectionSuffix(suffix)}"
    }
}

private fun resolveSectionEntry(rawLabel: String): Map.Entry<String, String>? {
    val normalized = normalizeSectionLabel(rawLabel)
    if (normalized.isBlank()) {
        return null
    }

    return SectionLabelMap.entries.firstOrNull { (candidate, _) ->
        normalized == candidate || normalized.startsWith("$candidate ")
    }
}

private fun normalizeSectionLabel(rawLabel: String): String {
    return rawLabel.trim()
        .removeSuffix(":")
        .replace('_', ' ')
        .replace('-', ' ')
        .replace(Regex("""\s+"""), " ")
        .trim()
        .lowercase()
}

fun looksLikeChordLine(line: String): Boolean {
    return parseChordPlacements(line)?.isNotEmpty() == true
}

fun parseChordPlacements(line: String): List<ChordPlacement>? {
    val placements = mutableListOf<ChordPlacement>()
    var index = 0

    while (index < line.length) {
        while (index < line.length && line[index].isWhitespace()) {
            index++
        }

        if (index >= line.length) {
            break
        }

        val runStart = index
        while (index < line.length && !line[index].isWhitespace()) {
            index++
        }

        val run = line.substring(runStart, index)
        val segmented = segmentChordRun(run) ?: return null
        placements += segmented.map { (chord, relativeStart) ->
            ChordPlacement(
                chord = chord,
                start = runStart + relativeStart,
            )
        }
    }

    return placements
}

private fun segmentChordRun(run: String): List<ChordPlacement>? {
    if (run.isBlank()) {
        return emptyList()
    }
    if (run.matches(ChordSymbolRegex)) {
        return listOf(ChordPlacement(chord = run, start = 0))
    }

    val memo = mutableMapOf<Int, List<ChordPlacement>?>()

    fun segmentFrom(index: Int): List<ChordPlacement>? {
        if (index == run.length) {
            return emptyList()
        }

        memo[index]?.let { return it }

        var best: List<ChordPlacement>? = null
        for (end in (index + 1)..run.length) {
            val candidate = run.substring(index, end)
            if (!candidate.matches(ChordSymbolRegex)) {
                continue
            }

            val remainder = segmentFrom(end) ?: continue
            val split = listOf(ChordPlacement(chord = candidate, start = index)) + remainder
            best = when {
                best == null -> split
                split.size < best.size -> split
                split.size == best.size && split.first().chord.length > best.first().chord.length -> split
                else -> best
            }
        }

        memo[index] = best
        return best
    }

    return segmentFrom(0)
}

private fun formatSectionSuffix(suffix: String): String {
    return suffix.split(' ')
        .filter { it.isNotBlank() }
        .joinToString(" ") { token ->
            when {
                token.all(Char::isDigit) -> token
                token.length <= 3 && token.all(Char::isLetter) -> token.uppercase()
                else -> token.replaceFirstChar { character ->
                    if (character.isLowerCase()) character.titlecase() else character.toString()
                }
            }
        }
}
