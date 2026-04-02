package com.codex.stageset.ui.common

import android.content.ContentResolver
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun ContentResolver.readUtf8Text(uri: Uri): String = withContext(Dispatchers.IO) {
    openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { reader ->
        reader.readText()
    } ?: throw IllegalStateException("Couldn't open that archive file.")
}

suspend fun ContentResolver.writeUtf8Text(
    uri: Uri,
    text: String,
) = withContext(Dispatchers.IO) {
    openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { writer ->
        writer.write(text)
        writer.flush()
    } ?: throw IllegalStateException("Couldn't write that archive file.")
}
