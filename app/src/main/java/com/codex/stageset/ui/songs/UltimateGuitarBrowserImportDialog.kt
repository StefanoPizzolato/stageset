@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.codex.stageset.ui.songs

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.codex.stageset.data.remote.UltimateGuitarParser
import com.codex.stageset.data.remote.UltimateGuitarRequestProfile
import com.codex.stageset.data.repository.ImportedSongDraft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun UltimateGuitarBrowserImportDialog(
    url: String,
    onDismiss: () -> Unit,
    onImportSuccess: (ImportedSongDraft) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var webView by remember(url) { mutableStateOf<WebView?>(null) }
    var currentPageUrl by remember(url) { mutableStateOf(url) }
    var isPageLoading by remember(url) { mutableStateOf(true) }
    var isImportingPage by remember(url) { mutableStateOf(false) }
    var statusMessage by remember(url) {
        mutableStateOf("Loading the Ultimate Guitar page in browser mode.")
    }
    var hasAttemptedAutoImport by remember(url) { mutableStateOf(false) }

    fun importCurrentPage() {
        val activeWebView = webView ?: return
        if (isImportingPage) {
            return
        }

        isImportingPage = true
        statusMessage = "Reading the current browser page."
        activeWebView.evaluateJavascript(
            "(function(){return document.documentElement.outerHTML;})()",
        ) { htmlPayload ->
            val html = decodeJavascriptString(htmlPayload)
            if (html.isNullOrBlank()) {
                isImportingPage = false
                statusMessage = "Couldn't read the current page yet. If a prompt is visible, finish it and try again."
                return@evaluateJavascript
            }

            scope.launch {
                val result = withContext(Dispatchers.Default) {
                    runCatching {
                        UltimateGuitarParser.parse(
                            html = html,
                            sourceUrl = currentPageUrl,
                        )
                    }
                }

                result.onSuccess { imported ->
                    statusMessage = "Imported chart from the browser page."
                    onImportSuccess(imported)
                }.onFailure { throwable ->
                    statusMessage = throwable.message
                        ?: "That page still isn't readable. If Ultimate Guitar is showing a check or sign-in prompt, complete it and try again."
                }
                isImportingPage = false
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.94f),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Browser Import Fallback",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = "Ultimate Guitar blocked the direct request, so this loads the page through Android WebView. If the site shows a prompt, finish it here and then import the current page.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (isPageLoading || isImportingPage) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Box(modifier = Modifier.weight(1f)) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { context ->
                            WebView(context).apply {
                                webView = this
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.loadsImagesAutomatically = true
                                settings.userAgentString = UltimateGuitarRequestProfile.DesktopUserAgent
                                CookieManager.getInstance().setAcceptCookie(true)
                                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                                webChromeClient = WebChromeClient()
                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView, finishedUrl: String?) {
                                        currentPageUrl = finishedUrl ?: currentPageUrl
                                        isPageLoading = false
                                        statusMessage = "Page loaded. Importing automatically now."
                                        if (!hasAttemptedAutoImport) {
                                            hasAttemptedAutoImport = true
                                            importCurrentPage()
                                        } else {
                                            statusMessage = "Page loaded. Tap Import Current Page if you want to retry."
                                        }
                                    }

                                    override fun onReceivedError(
                                        view: WebView?,
                                        request: WebResourceRequest?,
                                        error: WebResourceError?,
                                    ) {
                                        if (request?.isForMainFrame == true) {
                                            isPageLoading = false
                                            statusMessage = error?.description?.toString()
                                                ?: "The browser fallback couldn't load the page."
                                        }
                                    }
                                }
                                loadUrl(url, UltimateGuitarRequestProfile.browserHeaders(url))
                            }
                        },
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }
                    Button(
                        onClick = ::importCurrentPage,
                        enabled = !isPageLoading && !isImportingPage,
                    ) {
                        Text("Import Current Page")
                    }
                }
            }
        }
    }

    DisposableEffect(url) {
        onDispose {
            webView?.stopLoading()
            webView?.destroy()
            webView = null
        }
    }
}

private fun decodeJavascriptString(payload: String?): String? {
    val value = payload?.takeIf { it.isNotBlank() && it != "null" } ?: return null
    return runCatching {
        JSONArray("[$value]").getString(0)
    }.getOrNull()
}
