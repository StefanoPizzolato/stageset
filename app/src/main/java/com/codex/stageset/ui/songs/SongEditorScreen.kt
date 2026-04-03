@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.codex.stageset.ui.songs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.codex.stageset.chart.TransposeDirection
import com.codex.stageset.chart.transposeChart
import com.codex.stageset.chart.transposeKeySignature
import com.codex.stageset.data.remote.UltimateGuitarBlockedException
import com.codex.stageset.data.repository.Song
import com.codex.stageset.data.repository.ImportedSongDraft
import com.codex.stageset.data.repository.SongDraft
import com.codex.stageset.data.repository.SongRepository
import com.codex.stageset.ui.common.ChartPreview
import com.codex.stageset.ui.common.ConfirmActionDialog
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongEditorRoute(
    songId: Long,
    songRepository: SongRepository,
    onBack: () -> Unit,
) {
    val existingSongFlow = remember(songId, songRepository) {
        if (songId > 0) {
            songRepository.observeSong(songId)
        } else {
            flowOf<Song?>(null)
        }
    }
    val existingSong by existingSongFlow.collectAsState(initial = null)
    val scope = rememberCoroutineScope()

    var name by rememberSaveable(songId) { mutableStateOf("") }
    var artist by rememberSaveable(songId) { mutableStateOf("") }
    var preset by rememberSaveable(songId) { mutableStateOf("") }
    var keySignature by rememberSaveable(songId) { mutableStateOf("") }
    var chart by rememberSaveable(songId) { mutableStateOf("") }
    var importUrl by rememberSaveable(songId) { mutableStateOf("") }
    var hasHydrated by rememberSaveable(songId) { mutableStateOf(songId <= 0) }
    var isSaving by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var browserImportUrl by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    fun applyImportedSong(imported: ImportedSongDraft, message: String) {
        name = imported.name.ifBlank { name }
        artist = imported.artist.ifBlank { artist }
        preset = imported.preset.ifBlank { preset }
        keySignature = imported.keySignature.ifBlank { keySignature }
        chart = imported.chart
        feedbackMessage = message
    }

    fun transposeSong(direction: TransposeDirection) {
        keySignature = transposeKeySignature(keySignature, direction)
        chart = transposeChart(chart, direction)
        feedbackMessage = if (direction == TransposeDirection.Up) {
            "Transposed up a semitone."
        } else {
            "Transposed down a semitone."
        }
    }

    fun launchDirectImport() {
        scope.launch {
            val targetUrl = importUrl.trim()
            isImporting = true
            feedbackMessage = null
            songRepository.importFromUltimateGuitar(targetUrl)
                .onSuccess { imported ->
                    applyImportedSong(imported, "Imported chart from Ultimate Guitar.")
                }
                .onFailure { throwable ->
                    if (shouldOfferBrowserFallback(targetUrl, throwable)) {
                        feedbackMessage = browserFallbackMessage(throwable)
                        browserImportUrl = targetUrl
                    } else {
                        feedbackMessage = throwable.message ?: "Import failed."
                    }
                }
            isImporting = false
        }
    }

    LaunchedEffect(existingSong?.id) {
        if (!hasHydrated && existingSong != null) {
            name = existingSong?.name.orEmpty()
            artist = existingSong?.artist.orEmpty()
            preset = existingSong?.preset.orEmpty()
            keySignature = existingSong?.keySignature.orEmpty()
            chart = existingSong?.chart.orEmpty()
            hasHydrated = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val wideLayout = maxWidth >= 1000.dp
        val formPaneWidth = maxWidth * 0.42f
        val formScroll = rememberScrollState()

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0f),
            topBar = {
                TopAppBar(
                    title = { Text(if (songId > 0) "Edit Song" else "New Song") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (songId > 0) {
                            TextButton(
                                onClick = { showDeleteConfirmation = true },
                            ) {
                                Icon(Icons.Outlined.Delete, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Delete")
                            }
                        }
                        FilledTonalButton(
                            onClick = {
                                scope.launch {
                                    isSaving = true
                                    feedbackMessage = null
                                    songRepository.saveSong(
                                        songId = songId.takeIf { it > 0 },
                                        draft = SongDraft(
                                            name = name,
                                            artist = artist,
                                            preset = preset,
                                            keySignature = keySignature,
                                            chart = chart,
                                        ),
                                    )
                                    isSaving = false
                                    onBack()
                                }
                            },
                            enabled = name.isNotBlank() && !isSaving,
                        ) {
                            Icon(Icons.Outlined.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save")
                        }
                    },
                )
            },
        ) { innerPadding ->
            if (wideLayout) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .width(formPaneWidth)
                            .fillMaxHeight()
                            .verticalScroll(formScroll),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        SongForm(
                            name = name,
                            artist = artist,
                            preset = preset,
                            keySignature = keySignature,
                            chart = chart,
                            importUrl = importUrl,
                            isImporting = isImporting,
                            feedbackMessage = feedbackMessage,
                            onNameChange = { name = it },
                            onArtistChange = { artist = it },
                            onPresetChange = { preset = it },
                            onKeyChange = { keySignature = it },
                            onChartChange = { chart = it },
                            onTransposeUp = { transposeSong(TransposeDirection.Up) },
                            onTransposeDown = { transposeSong(TransposeDirection.Down) },
                            onImportUrlChange = { importUrl = it },
                            onImportClick = ::launchDirectImport,
                        )
                    }

                    ChartPreview(
                        title = name,
                        artist = artist,
                        keySignature = keySignature,
                        chart = chart,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(formScroll)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    SongForm(
                        name = name,
                        artist = artist,
                        preset = preset,
                        keySignature = keySignature,
                        chart = chart,
                        importUrl = importUrl,
                        isImporting = isImporting,
                        feedbackMessage = feedbackMessage,
                        onNameChange = { name = it },
                        onArtistChange = { artist = it },
                        onPresetChange = { preset = it },
                        onKeyChange = { keySignature = it },
                        onChartChange = { chart = it },
                        onTransposeUp = { transposeSong(TransposeDirection.Up) },
                        onTransposeDown = { transposeSong(TransposeDirection.Down) },
                        onImportUrlChange = { importUrl = it },
                        onImportClick = ::launchDirectImport,
                    )
                    ChartPreview(
                        title = name,
                        artist = artist,
                        keySignature = keySignature,
                        chart = chart,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(520.dp),
                    )
                }
            }
        }
    }

    browserImportUrl?.let { targetUrl ->
        UltimateGuitarBrowserImportDialog(
            url = targetUrl,
            onDismiss = { browserImportUrl = null },
            onImportSuccess = { imported ->
                browserImportUrl = null
                applyImportedSong(
                    imported = imported,
                    message = "Imported chart from Ultimate Guitar browser fallback.",
                )
            },
        )
    }

    if (showDeleteConfirmation) {
        ConfirmActionDialog(
            title = "Delete song?",
            message = "This will permanently remove this song from the library.",
            confirmLabel = "Delete",
            onConfirm = {
                showDeleteConfirmation = false
                scope.launch {
                    songRepository.deleteSong(songId)
                    onBack()
                }
            },
            onDismiss = { showDeleteConfirmation = false },
        )
    }
}

private fun shouldOfferBrowserFallback(
    targetUrl: String,
    throwable: Throwable,
): Boolean {
    if (!targetUrl.contains("ultimate-guitar.com", ignoreCase = true)) {
        return false
    }

    if (throwable is IllegalArgumentException) {
        return false
    }

    if (throwable is UltimateGuitarBlockedException) {
        return true
    }

    val message = throwable.message.orEmpty()
    return message.contains("http 401", ignoreCase = true) ||
        message.contains("http 403", ignoreCase = true) ||
        message.contains("couldn't extract a chord chart", ignoreCase = true) ||
        message.contains("page may need sign-in", ignoreCase = true) ||
        message.contains("blocked", ignoreCase = true)
}

private fun browserFallbackMessage(throwable: Throwable): String {
    if (throwable is UltimateGuitarBlockedException) {
        return "Ultimate Guitar blocked the direct import. Opening browser fallback."
    }

    return "Direct import couldn't read that Ultimate Guitar page. Opening browser fallback."
}

@Composable
private fun SongForm(
    name: String,
    artist: String,
    preset: String,
    keySignature: String,
    chart: String,
    importUrl: String,
    isImporting: Boolean,
    feedbackMessage: String?,
    onNameChange: (String) -> Unit,
    onArtistChange: (String) -> Unit,
    onPresetChange: (String) -> Unit,
    onKeyChange: (String) -> Unit,
    onChartChange: (String) -> Unit,
    onTransposeUp: () -> Unit,
    onTransposeDown: () -> Unit,
    onImportUrlChange: (String) -> Unit,
    onImportClick: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Song details",
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = "Store the live-ready version of the song exactly how you want to read it on stage.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Song name") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
        )
        OutlinedTextField(
            value = artist,
            onValueChange = onArtistChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Artist") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
        )
        OutlinedTextField(
            value = preset,
            onValueChange = onPresetChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Preset") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
        )
        OutlinedTextField(
            value = keySignature,
            onValueChange = onKeyChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Key") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FilledTonalButton(onClick = onTransposeDown) {
                Text("Transpose -")
            }
            FilledTonalButton(onClick = onTransposeUp) {
                Text("Transpose +")
            }
        }

        Text(
            text = "Ultimate Guitar import",
            style = MaterialTheme.typography.titleLarge,
        )
        OutlinedTextField(
            value = importUrl,
            onValueChange = onImportUrlChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Ultimate Guitar URL") },
            placeholder = { Text("https://tabs.ultimate-guitar.com/...") },
            singleLine = true,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = onImportClick,
                enabled = importUrl.isNotBlank() && !isImporting,
            ) {
                Icon(Icons.Outlined.CloudDownload, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Import Tab")
            }
        }
        if (isImporting) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        feedbackMessage?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Text(
            text = "Lyrics and chord chart",
            style = MaterialTheme.typography.titleLarge,
        )
        OutlinedTextField(
            value = chart,
            onValueChange = onChartChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp),
            label = { Text("Chart") },
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
            supportingText = {
                Text("Use @ ... @ blocks for melody MML, for example: @ key=Ebm meter=4/4 cleff=treble o4 l4 c d e f @")
            },
        )
    }
}
