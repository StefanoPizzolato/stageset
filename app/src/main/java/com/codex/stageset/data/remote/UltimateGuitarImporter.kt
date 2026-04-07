package com.codex.stageset.data.remote

import com.codex.stageset.data.repository.ImportedSongDraft
import java.io.IOException
import java.security.MessageDigest
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class UltimateGuitarImporter(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(InMemoryCookieJar())
        .followRedirects(true)
        .followSslRedirects(true)
        .build(),
) {
    suspend fun search(query: String): List<UltimateGuitarSearchResult> = withContext(Dispatchers.IO) {
        require(query.isNotBlank()) { "Search query cannot be blank." }

        runCatching {
            searchViaApi(query)
        }.getOrElse {
            searchViaPage(query)
        }
    }

    suspend fun importTab(
        tabId: Long,
        tabAccessType: String = "public",
    ): ImportedSongDraft = withContext(Dispatchers.IO) {
        require(tabId > 0) { "Invalid Ultimate Guitar tab id." }

        val deviceId = generateMobileClientId()
        val url = "${UltimateGuitarApiEndpoint}/tab/info".toHttpUrl()
            .newBuilder()
            .addQueryParameter("tab_id", tabId.toString())
            .addQueryParameter("tab_access_type", tabAccessType.ifBlank { "public" })
            .build()
            .toString()

        val json = JSONObject(fetchApiJson(url = url, clientId = deviceId))
        val content = json.optString("content").trim()
        if (content.isBlank()) {
            val urlWeb = json.optString("urlWeb").trim()
            if (urlWeb.isNotBlank()) {
                return@withContext import(urlWeb)
            }
            error("Ultimate Guitar returned an empty tab.")
        }

        ImportedSongDraft(
            name = json.optString("song_name").trim(),
            artist = json.optString("artist_name").trim(),
            keySignature = json.optString("tonality_name").trim(),
            chart = UltimateGuitarParser.formatMarkup(content),
        )
    }

    suspend fun import(url: String): ImportedSongDraft = withContext(Dispatchers.IO) {
        require(url.contains("ultimate-guitar.com", ignoreCase = true)) {
            "Paste a full Ultimate Guitar page URL."
        }

        val html = runCatching { fetchHtml(url) }
            .recoverCatching { throwable ->
                if (throwable is UltimateGuitarBlockedException) {
                    warmSession(url)
                    fetchHtml(url)
                } else {
                    throw throwable
                }
            }
            .getOrElse { throw it }

        if (html.isBlank()) {
            error("Ultimate Guitar returned an empty page.")
        }

        UltimateGuitarParser.parse(html = html, sourceUrl = url)
    }

    private fun fetchHtml(url: String): String {
        val requestBuilder = Request.Builder().url(url)
        UltimateGuitarRequestProfile.browserHeaders(url).forEach { (name, value) ->
            requestBuilder.header(name, value)
        }

        client.newCall(requestBuilder.build()).execute().use { response ->
            when {
                response.code == 401 || response.code == 403 -> {
                    throw UltimateGuitarBlockedException(response.code)
                }

                !response.isSuccessful -> {
                    error("Ultimate Guitar returned HTTP ${response.code}.")
                }
            }

            return response.body?.string().orEmpty()
        }
    }

    private fun fetchApiJson(
        url: String,
        clientId: String,
    ): String {
        val requestBuilder = Request.Builder().url(url)
        ultimateGuitarApiHeaders(clientId).forEach { (name, value) ->
            requestBuilder.header(name, value)
        }

        client.newCall(requestBuilder.build()).execute().use { response ->
            if (!response.isSuccessful) {
                error("Ultimate Guitar search returned HTTP ${response.code}.")
            }

            return response.body?.string().orEmpty()
        }
    }

    private fun searchViaApi(query: String): List<UltimateGuitarSearchResult> {
        val deviceId = generateMobileClientId()
        val url = "${UltimateGuitarApiEndpoint}/tab/search".toHttpUrl()
            .newBuilder()
            .addQueryParameter("title", query.trim())
            .addQueryParameter("type[]", "300")
            .build()
            .toString()

        val json = JSONObject(fetchApiJson(url = url, clientId = deviceId))
        val tabs = json.optJSONArray("tabs") ?: return emptyList()

        return buildList {
            for (index in 0 until tabs.length()) {
                val tab = tabs.optJSONObject(index) ?: continue
                val type = tab.optString("type")
                if (!type.contains("chord", ignoreCase = true) || type.contains("ukulele", ignoreCase = true)) {
                    continue
                }

                add(
                    UltimateGuitarSearchResult(
                        tabId = tab.optLong("id"),
                        title = tab.optString("song_name").trim().ifBlank { "Untitled" },
                        artist = tab.optString("artist_name").trim(),
                        url = "",
                        typeLabel = type.ifBlank { "Chords" },
                        version = tab.optInt("version").takeIf { it > 0 },
                        tabAccessType = tab.optString("tab_access_type").ifBlank { "public" },
                    ),
                )
            }
        }
    }

    private fun searchViaPage(query: String): List<UltimateGuitarSearchResult> {
        val encodedQuery = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8.toString())
        val searchUrl = "https://www.ultimate-guitar.com/search.php?search_type=title&value=$encodedQuery"
        val html = fetchHtml(searchUrl)
        return UltimateGuitarSearchParser.parseChordResults(
            html = html,
            sourceUrl = searchUrl,
        )
    }

    private fun warmSession(url: String) {
        val warmRequestBuilder = Request.Builder()
            .url(UltimateGuitarRequestProfile.refererFor(url))

        UltimateGuitarRequestProfile.browserHeaders(url).forEach { (name, value) ->
            warmRequestBuilder.header(name, value)
        }

        runCatching {
            client.newCall(warmRequestBuilder.build()).execute().close()
        }
    }
}

private const val UltimateGuitarApiEndpoint = "https://api.ultimate-guitar.com/api/v1"

private fun ultimateGuitarApiHeaders(clientId: String): Map<String, String> {
    return linkedMapOf(
        "Accept-Charset" to "utf-8",
        "Accept" to "application/json",
        "User-Agent" to "UGT_ANDROID/4.11.1 (Pixel; 8.1.0)",
        "X-UG-CLIENT-ID" to clientId,
        "X-UG-API-KEY" to buildUltimateGuitarApiKey(clientId),
    )
}

private fun generateMobileClientId(): String {
    return UUID.randomUUID().toString().replace("-", "").take(16)
}

private fun buildUltimateGuitarApiKey(clientId: String): String {
    val now = ZonedDateTime.now(ZoneOffset.UTC)
    val signatureSource = buildString {
        append(clientId)
        append(DateTimeFormatter.ofPattern("yyyy-MM-dd").format(now))
        append(':')
        append(now.hour)
        append("createLog()")
    }
    return MessageDigest.getInstance("MD5")
        .digest(signatureSource.toByteArray())
        .joinToString(separator = "") { byte -> "%02x".format(byte) }
}

class UltimateGuitarBlockedException(
    val statusCode: Int,
) : IOException("Ultimate Guitar blocked the direct import (HTTP $statusCode). Try the browser import fallback.")

private class InMemoryCookieJar : CookieJar {
    private val store = linkedMapOf<String, MutableList<Cookie>>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        if (cookies.isEmpty()) {
            return
        }

        val hostCookies = store.getOrPut(url.host) { mutableListOf() }
        cookies.forEach { incoming ->
            hostCookies.removeAll { existing ->
                existing.name == incoming.name && existing.matches(url)
            }
            hostCookies += incoming
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val hostCookies = store[url.host].orEmpty()
        return hostCookies.filter { cookie ->
            !cookie.expiresAt.let { it != Long.MIN_VALUE && it < System.currentTimeMillis() } &&
                cookie.matches(url)
        }
    }
}
