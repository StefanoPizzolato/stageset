package com.codex.stageset.data.remote

import com.codex.stageset.data.repository.ImportedSongDraft
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

class UltimateGuitarImporter(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(InMemoryCookieJar())
        .followRedirects(true)
        .followSslRedirects(true)
        .build(),
) {
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
