package com.codex.stageset.data.remote

import com.codex.stageset.chart.canonicalSectionLine
import com.codex.stageset.data.repository.ImportedSongDraft
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.parser.Parser

object UltimateGuitarParser {
    fun parse(html: String, sourceUrl: String = ""): ImportedSongDraft {
        val document = Jsoup.parse(html, sourceUrl)
        val structuredImport = findStructuredImport(document)
        if (structuredImport != null) {
            return structuredImport
        }

        val preformattedChart = document.selectFirst("pre")?.text()?.trim().orEmpty()
        if (preformattedChart.isNotBlank()) {
            val fallbackMetadata = fallbackMetadata(document)
            return ImportedSongDraft(
                name = fallbackMetadata.first,
                artist = fallbackMetadata.second,
                keySignature = "",
                chart = preformattedChart,
            )
        }

        error(
            "Couldn't extract a chord chart from that page. The page may need sign-in, or Ultimate Guitar may have changed its markup.",
        )
    }

    fun formatMarkup(raw: String): String {
        val normalized = Parser.unescapeEntities(raw, false)
            .replace("\r\n", "\n")
            .replace("[tab]", "")
            .replace("[/tab]", "")
            .replace("[br]", "\n")
            .replace("[/br]", "\n")
        val separatedSections = separateSectionMarkers(normalized)

        val lines = separatedSections.lines()
            .flatMap(::expandChordMarkupLine)
            .map(::stripResidualTags)
            .map { it.replace('\u00A0', ' ').trimEnd() }
            .normalizeSectionSpacing()
            .dropTrailingBlankLines()

        return lines.joinToString("\n")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
    }

    private fun findStructuredImport(document: Document): ImportedSongDraft? {
        val jsonCandidates = collectJsonCandidates(document)

        for (candidate in jsonCandidates) {
            val json = parseJsonCandidate(candidate) ?: continue
            val content = findChartContent(json)?.takeIf { it.isNotBlank() } ?: continue
            val formattedChart = formatMarkup(content)
            if (formattedChart.isBlank()) continue

            val fallbackMetadata = fallbackMetadata(document)
            return ImportedSongDraft(
                name = findFirstString(json, setOf("song_name", "songName", "title"))
                    ?.cleanupMetadata()
                    .orEmpty()
                    .ifBlank { fallbackMetadata.first },
                artist = findFirstString(json, setOf("artist_name", "artistName", "artist"))
                    ?.cleanupMetadata()
                    .orEmpty()
                    .ifBlank { fallbackMetadata.second },
                keySignature = findFirstString(json, setOf("tonality_name", "key", "original_key"))
                    ?.cleanupMetadata()
                    .orEmpty(),
                chart = formattedChart,
            )
        }

        return null
    }

    private fun collectJsonCandidates(document: Document): List<String> {
        val candidates = buildList {
            document.select("script").forEach { script ->
                script.data().takeIf { it.isNotBlank() }?.let(::add)
                script.attr("data-content").takeIf { it.isNotBlank() }?.let(::add)
            }
            document.select("[data-content]").forEach { element ->
                element.attr("data-content").takeIf { it.isNotBlank() }?.let(::add)
            }
        }
        return candidates.distinct()
    }

    private fun parseJsonCandidate(candidate: String): JSONObject? {
        val direct = candidate.trim()
        if (direct.startsWith("{") && direct.endsWith("}")) {
            return runCatching { JSONObject(direct) }.getOrNull()
        }

        val possibleAssignments = listOf(
            "window.UGAPP.store.page",
            "window.__PRELOADED_STATE__",
            "window.__INITIAL_STATE__",
            "__NEXT_DATA__",
        )

        for (marker in possibleAssignments) {
            val markerIndex = candidate.indexOf(marker)
            if (markerIndex < 0) continue
            val jsonStart = candidate.indexOf('{', markerIndex)
            if (jsonStart < 0) continue
            extractBalancedJson(candidate, jsonStart)?.let { jsonText ->
                return runCatching { JSONObject(jsonText) }.getOrNull()
            }
        }

        return null
    }

    private fun extractBalancedJson(text: String, startIndex: Int): String? {
        var braceDepth = 0
        var inString = false
        var isEscaped = false

        for (index in startIndex until text.length) {
            val character = text[index]
            when {
                isEscaped -> isEscaped = false
                character == '\\' -> isEscaped = true
                character == '"' -> inString = !inString
                !inString && character == '{' -> braceDepth++
                !inString && character == '}' -> {
                    braceDepth--
                    if (braceDepth == 0) {
                        return text.substring(startIndex, index + 1)
                    }
                }
            }
        }

        return null
    }

    private fun findChartContent(node: Any?): String? {
        return when (node) {
            is JSONObject -> {
                val wikiTab = node.opt("wiki_tab")
                when (wikiTab) {
                    is String -> wikiTab
                    is JSONObject -> wikiTab.optString("content").takeIf { it.isNotBlank() }
                    else -> null
                }?.let { return it }

                val content = node.optString("content")
                val type = node.optString("type")
                if (content.isNotBlank() && (type.contains("chord", true) || type.contains("tab", true))) {
                    return content
                }

                node.keys().asSequence().forEach { key ->
                    findChartContent(node.opt(key))?.let { return it }
                }
                null
            }

            is JSONArray -> {
                for (index in 0 until node.length()) {
                    findChartContent(node.opt(index))?.let { return it }
                }
                null
            }

            else -> null
        }
    }

    private fun findFirstString(node: Any?, keys: Set<String>): String? = when (node) {
        is JSONObject -> {
            keys.firstNotNullOfOrNull { key ->
                node.optString(key).takeIf { it.isNotBlank() }
            } ?: node.keys().asSequence().firstNotNullOfOrNull { key ->
                findFirstString(node.opt(key), keys)
            }
        }

        is JSONArray -> {
            for (index in 0 until node.length()) {
                findFirstString(node.opt(index), keys)?.let { return it }
            }
            null
        }

        else -> null
    }

    private fun fallbackMetadata(document: Document): Pair<String, String> {
        val metaTitle = document.selectFirst("meta[property=og:title]")?.attr("content").orEmpty()
        val titleSource = metaTitle.ifBlank { document.title() }
        if (titleSource.isBlank()) {
            return "Imported song" to ""
        }

        val explicitPattern = Regex(
            pattern = """^(.*?)\s+(?:CHORDS|CHORD|TAB|TABS|UKULELE CHORDS)\s+by\s+(.*?)\s+@""",
            option = RegexOption.IGNORE_CASE,
        )
        val explicitMatch = explicitPattern.find(titleSource)
        if (explicitMatch != null) {
            return explicitMatch.groupValues[1].cleanupMetadata() to explicitMatch.groupValues[2].cleanupMetadata()
        }

        val splitByHyphen = titleSource.substringBefore(" @ ").split(" - ")
        return when {
            splitByHyphen.size >= 2 -> splitByHyphen[0].cleanupMetadata() to splitByHyphen[1].cleanupMetadata()
            else -> titleSource.cleanupMetadata() to ""
        }
    }

    private fun expandChordMarkupLine(rawLine: String): List<String> {
        if (!rawLine.contains("[ch]")) {
            return listOf(rawLine)
        }

        val chordLine = StringBuilder()
        val lyricLine = StringBuilder()
        var index = 0

        while (index < rawLine.length) {
            when {
                rawLine.startsWith("[ch]", index) -> {
                    val closeIndex = rawLine.indexOf("[/ch]", startIndex = index)
                    if (closeIndex == -1) {
                        index = rawLine.length
                    } else {
                        val chord = rawLine.substring(index + 4, closeIndex).cleanupMetadata()
                        while (chordLine.length < lyricLine.length) {
                            chordLine.append(' ')
                        }
                        chordLine.append(chord)
                        index = closeIndex + 5
                    }
                }

                rawLine[index] == '[' -> {
                    val closeIndex = rawLine.indexOf(']', startIndex = index)
                    if (closeIndex == -1) {
                        lyricLine.append(rawLine[index])
                        index++
                    } else {
                        index = closeIndex + 1
                    }
                }

                else -> {
                    lyricLine.append(rawLine[index])
                    if (chordLine.length < lyricLine.length) {
                        chordLine.append(' ')
                    }
                    index++
                }
            }
        }

        val cleanedChordLine = chordLine.toString().trimEnd()
        val cleanedLyricLine = lyricLine.toString().trimEnd()

        return when {
            cleanedChordLine.isBlank() -> listOf(cleanedLyricLine)
            cleanedLyricLine.isBlank() -> listOf(cleanedChordLine)
            else -> listOf(cleanedChordLine, cleanedLyricLine)
        }
    }

    private fun separateSectionMarkers(text: String): String {
        return Regex("""\[[^\[\]]+\]""").replace(text) { matchResult ->
            val sectionLine = canonicalSectionLine(matchResult.value) ?: return@replace matchResult.value
            val prefix = if (matchResult.range.first > 0 && text[matchResult.range.first - 1] != '\n') "\n" else ""
            val suffix = if (matchResult.range.last < text.lastIndex && text[matchResult.range.last + 1] != '\n') "\n" else ""
            "$prefix$sectionLine$suffix"
        }
    }

    private fun stripResidualTags(line: String): String {
        canonicalSectionLine(line)?.let { return it }
        return line.replace(Regex("""\[(?:/?[a-zA-Z][^\]]*)]"""), "")
    }

    private fun List<String>.normalizeSectionSpacing(): List<String> {
        if (isEmpty()) return this

        val normalized = mutableListOf<String>()
        forEach { line ->
            if (canonicalSectionLine(line) != null && normalized.lastOrNull()?.isNotBlank() == true) {
                normalized += ""
            }
            normalized += line
        }
        return normalized
    }

    private fun String.cleanupMetadata(): String = trim()
        .replace('\u00A0', ' ')
        .replace(Regex("""\s+"""), " ")
        .removeSuffix("@ Ultimate-Guitar.Com")
        .trim()

    private fun List<String>.dropTrailingBlankLines(): List<String> {
        var lastIndex = size - 1
        while (lastIndex >= 0 && this[lastIndex].isBlank()) {
            lastIndex--
        }
        return if (lastIndex < 0) emptyList() else subList(0, lastIndex + 1)
    }
}
