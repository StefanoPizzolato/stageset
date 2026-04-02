package com.codex.stageset.data.remote

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

object UltimateGuitarRequestProfile {
    const val DesktopUserAgent =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

    fun browserHeaders(url: String): Map<String, String> {
        val referer = refererFor(url)
        return linkedMapOf(
            "User-Agent" to DesktopUserAgent,
            "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8",
            "Accept-Language" to "en-US,en;q=0.9",
            "Cache-Control" to "max-age=0",
            "Pragma" to "no-cache",
            "Upgrade-Insecure-Requests" to "1",
            "Referer" to referer,
        )
    }

    fun refererFor(url: String): String {
        val target = url.toHttpUrlOrNull()
        return target?.newBuilder()
            ?.encodedPath("/")
            ?.query(null)
            ?.fragment(null)
            ?.build()
            ?.toString()
            ?: "https://www.ultimate-guitar.com/"
    }
}
