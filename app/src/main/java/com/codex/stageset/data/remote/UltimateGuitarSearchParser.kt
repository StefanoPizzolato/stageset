package com.codex.stageset.data.remote

import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

data class UltimateGuitarSearchResult(
    val tabId: Long = -1L,
    val title: String,
    val artist: String,
    val url: String,
    val typeLabel: String,
    val version: Int? = null,
    val tabAccessType: String = "public",
)

object UltimateGuitarSearchParser {
    fun parseChordResults(html: String, sourceUrl: String = ""): List<UltimateGuitarSearchResult> {
        val document = Jsoup.parse(html, sourceUrl)
        val structuredResults = findStructuredResults(document)
        if (structuredResults.isNotEmpty()) {
            return structuredResults
        }
        return findAnchorResults(document)
    }

    private fun findStructuredResults(document: Document): List<UltimateGuitarSearchResult> {
        val resultsByUrl = linkedMapOf<String, UltimateGuitarSearchResult>()
        collectJsonCandidates(document).forEach { candidate ->
            val json = parseJsonCandidate(candidate) ?: return@forEach
            collectStructuredResults(json, resultsByUrl)
        }
        return resultsByUrl.values.toList()
    }

    private fun collectStructuredResults(
        node: Any?,
        resultsByUrl: MutableMap<String, UltimateGuitarSearchResult>,
    ) {
        when (node) {
            is JSONObject -> {
                parseResultObject(node)?.let { result ->
                    resultsByUrl.putIfAbsent(result.url, result)
                }
                node.keys().asSequence().forEach { key ->
                    collectStructuredResults(node.opt(key), resultsByUrl)
                }
            }

            is JSONArray -> {
                for (index in 0 until node.length()) {
                    collectStructuredResults(node.opt(index), resultsByUrl)
                }
            }
        }
    }

    private fun parseResultObject(node: JSONObject): UltimateGuitarSearchResult? {
        val rawUrl = firstNonBlankString(
            node.optString("tab_url"),
            node.optString("url"),
            node.optString("tabUrl"),
            node.optString("share_link"),
            node.optString("shareLink"),
        ).orEmpty()
        val normalizedUrl = normalizeUltimateGuitarUrl(rawUrl) ?: return null

        val rawType = firstNonBlankString(
            node.optString("type"),
            node.optString("type_name"),
            node.optString("typeName"),
            node.optString("tab_type"),
            node.optString("tabType"),
        ).orEmpty()

        if (!isChordResult(url = normalizedUrl, typeLabel = rawType)) {
            return null
        }

        val title = firstNonBlankString(
            node.optString("song_name"),
            node.optString("songName"),
            node.optString("tab_name"),
            node.optString("tabName"),
            node.optString("title"),
            node.optString("name"),
        ).orEmpty().trim()

        val artistNode = node.opt("artist")
        val artist = firstNonBlankString(
            node.optString("artist_name"),
            node.optString("artistName"),
            node.optString("artist"),
            (artistNode as? JSONObject)?.optString("name").orEmpty(),
        ).orEmpty().trim()

        if (title.isBlank() && artist.isBlank()) {
            return null
        }

        return UltimateGuitarSearchResult(
            title = title.ifBlank { "Untitled" },
            artist = artist,
            url = normalizedUrl,
            typeLabel = rawType.ifBlank { "Chords" },
            version = node.optInt("version").takeIf { it > 0 },
        )
    }

    private fun findAnchorResults(document: Document): List<UltimateGuitarSearchResult> {
        val resultsByUrl = linkedMapOf<String, UltimateGuitarSearchResult>()
        document.select("a[href]").forEach { element ->
            val url = normalizeUltimateGuitarUrl(element.attr("abs:href").ifBlank { element.attr("href") })
                ?: return@forEach
            if (!isChordResult(url = url, typeLabel = element.text())) {
                return@forEach
            }

            val title = firstNonBlankString(
                element.text(),
                element.attr("title"),
                element.attr("aria-label"),
            ).orEmpty().trim()

            if (title.isBlank()) {
                return@forEach
            }

            resultsByUrl.putIfAbsent(
                url,
                UltimateGuitarSearchResult(
                    title = title,
                    artist = "",
                    url = url,
                    typeLabel = "Chords",
                ),
            )
        }
        return resultsByUrl.values.toList()
    }

    private fun isChordResult(url: String, typeLabel: String): Boolean {
        val normalizedType = typeLabel.trim().lowercase()
        if (normalizedType.contains("ukulele")) {
            return false
        }
        if (normalizedType.contains("chord")) {
            return true
        }
        return url.contains("chords", ignoreCase = true)
    }

    private fun normalizeUltimateGuitarUrl(rawUrl: String): String? {
        val trimmed = rawUrl.trim()
        if (trimmed.isBlank()) {
            return null
        }

        val absoluteUrl = when {
            trimmed.startsWith("//") -> "https:$trimmed"
            trimmed.startsWith("/") -> "https://www.ultimate-guitar.com$trimmed"
            else -> trimmed
        }

        if (!absoluteUrl.contains("ultimate-guitar.com", ignoreCase = true)) {
            return null
        }

        return absoluteUrl.substringBefore('?').substringBefore('#')
    }

    private fun firstNonBlankString(vararg candidates: String): String? {
        return candidates.firstOrNull { it.isNotBlank() }
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
}
