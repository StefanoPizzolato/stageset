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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.codex.stageset.chart.MelodyParseResult
import com.codex.stageset.chart.TransposeDirection
import com.codex.stageset.chart.parseMelodyNotation
import com.codex.stageset.chart.transposeChart
import com.codex.stageset.chart.transposeKeySignature
import com.codex.stageset.data.remote.UltimateGuitarBlockedException
import com.codex.stageset.data.repository.Song
import com.codex.stageset.data.repository.ImportedSongDraft
import com.codex.stageset.data.repository.SongDraft
import com.codex.stageset.data.repository.SongRepository
import com.codex.stageset.data.repository.UltimateGuitarConsentRepository
import com.codex.stageset.ui.common.ChartPreview
import com.codex.stageset.ui.common.ConfirmActionDialog
import com.codex.stageset.ui.common.MelodyStaffPreview
import com.codex.stageset.ui.common.PreviewRenderOptions
import com.codex.stageset.ui.common.StageSetTopAppBar
import com.codex.stageset.ui.common.buildCompressedChartText
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongEditorRoute(
    songId: Long,
    ultimateGuitarConsentRepository: UltimateGuitarConsentRepository,
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
    var compressedChartOverride by rememberSaveable(songId) { mutableStateOf<String?>(null) }
    var importQuery by rememberSaveable(songId) { mutableStateOf("") }
    var hasHydrated by rememberSaveable(songId) { mutableStateOf(songId <= 0) }
    var isSaving by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var browserImportUrl by remember { mutableStateOf<String?>(null) }
    var showUltimateGuitarSearch by remember { mutableStateOf(false) }
    var hasAcceptedUltimateGuitarSearchDisclaimer by remember {
        mutableStateOf(ultimateGuitarConsentRepository.hasAcceptedSearchDisclaimer())
    }
    var showUltimateGuitarSearchDisclaimer by remember { mutableStateOf(false) }
    var showCompressedVersion by rememberSaveable(songId) { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val autoCompressedChart = remember(chart) { buildCompressedChartText(chart) }
    val displayedChart = if (showCompressedVersion) {
        compressedChartOverride ?: autoCompressedChart
    } else {
        chart
    }
    val previewChart = displayedChart
    val previewOptions = if (showCompressedVersion) {
        PreviewRenderOptions(
            showLyrics = false,
            showChords = true,
            showNotation = true,
            hideRepeatedSectionChords = false,
            compressChords = false,
        )
    } else {
        PreviewRenderOptions()
    }

    fun applyImportedSong(imported: ImportedSongDraft, message: String) {
        name = imported.name.ifBlank { name }
        artist = imported.artist.ifBlank { artist }
        preset = imported.preset.ifBlank { preset }
        keySignature = imported.keySignature.ifBlank { keySignature }
        chart = imported.chart
        compressedChartOverride = null
        feedbackMessage = message
    }

    fun transposeSong(direction: TransposeDirection) {
        keySignature = transposeKeySignature(keySignature, direction)
        chart = transposeChart(chart, direction)
        compressedChartOverride = compressedChartOverride?.let { transposeChart(it, direction) }
        feedbackMessage = if (direction == TransposeDirection.Up) {
            "Transposed up a semitone."
        } else {
            "Transposed down a semitone."
        }
    }

    fun launchDirectImport(targetUrl: String) {
        scope.launch {
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

    fun launchSearchResultImport(result: com.codex.stageset.data.remote.UltimateGuitarSearchResult) {
        scope.launch {
            isImporting = true
            feedbackMessage = null
            songRepository.importFromUltimateGuitarTab(
                tabId = result.tabId,
                tabAccessType = result.tabAccessType,
            )
                .onSuccess { imported ->
                    applyImportedSong(imported, "Imported chart from Ultimate Guitar.")
                }
                .onFailure { throwable ->
                    feedbackMessage = throwable.message ?: "Import failed."
                }
            isImporting = false
        }
    }

    fun openUltimateGuitarSearch() {
        if (hasAcceptedUltimateGuitarSearchDisclaimer) {
            showUltimateGuitarSearch = true
        } else {
            showUltimateGuitarSearchDisclaimer = true
        }
    }

    fun updateDisplayedChart(updated: String) {
        if (showCompressedVersion) {
            compressedChartOverride = updated.takeUnless { it == autoCompressedChart }
        } else {
            chart = updated
        }
    }

    LaunchedEffect(existingSong?.id) {
        if (!hasHydrated && existingSong != null) {
            name = existingSong?.name.orEmpty()
            artist = existingSong?.artist.orEmpty()
            preset = existingSong?.preset.orEmpty()
            keySignature = existingSong?.keySignature.orEmpty()
            chart = existingSong?.chart.orEmpty()
            compressedChartOverride = existingSong?.compressedChart
            hasHydrated = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val compactTopBar = maxWidth < 600.dp
        val wideLayout = maxWidth >= 1000.dp
        val formPaneWidth = maxWidth * 0.42f
        val formScroll = rememberScrollState()

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0f),
            topBar = {
                StageSetTopAppBar(
                    title = {
                        Text(
                            text = if (songId > 0) "Edit Song" else "New Song",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (songId > 0) {
                            if (compactTopBar) {
                                IconButton(onClick = { showDeleteConfirmation = true }) {
                                    Icon(Icons.Outlined.Delete, contentDescription = "Delete song")
                                }
                            } else {
                                TextButton(
                                    onClick = { showDeleteConfirmation = true },
                                ) {
                                    Icon(Icons.Outlined.Delete, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Delete")
                                }
                            }
                        }
                        if (compactTopBar) {
                            IconButton(
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
                                                compressedChart = compressedChartOverride,
                                            ),
                                        )
                                        isSaving = false
                                        onBack()
                                    }
                                },
                                enabled = name.isNotBlank() && !isSaving,
                            ) {
                                Icon(Icons.Outlined.Save, contentDescription = "Save song")
                            }
                        } else {
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
                                                compressedChart = compressedChartOverride,
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
                            chart = displayedChart,
                            importQuery = importQuery,
                            showCompressedVersion = showCompressedVersion,
                            isImporting = isImporting,
                            feedbackMessage = feedbackMessage,
                            onNameChange = { name = it },
                            onArtistChange = { artist = it },
                            onPresetChange = { preset = it },
                            onKeyChange = { keySignature = it },
                            onChartChange = ::updateDisplayedChart,
                            onTransposeUp = { transposeSong(TransposeDirection.Up) },
                            onTransposeDown = { transposeSong(TransposeDirection.Down) },
                            onImportQueryChange = { importQuery = it },
                            onImportClick = ::openUltimateGuitarSearch,
                            onShowCompressedVersionChange = { showCompressedVersion = it },
                        )
                    }

                    ChartPreview(
                        title = name,
                        artist = artist,
                        keySignature = keySignature,
                        chart = previewChart,
                        previewOptions = previewOptions,
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
                        chart = displayedChart,
                        importQuery = importQuery,
                        showCompressedVersion = showCompressedVersion,
                        isImporting = isImporting,
                        feedbackMessage = feedbackMessage,
                        onNameChange = { name = it },
                        onArtistChange = { artist = it },
                        onPresetChange = { preset = it },
                        onKeyChange = { keySignature = it },
                        onChartChange = ::updateDisplayedChart,
                        onTransposeUp = { transposeSong(TransposeDirection.Up) },
                        onTransposeDown = { transposeSong(TransposeDirection.Down) },
                        onImportQueryChange = { importQuery = it },
                        onImportClick = ::openUltimateGuitarSearch,
                        onShowCompressedVersionChange = { showCompressedVersion = it },
                    )
                    ChartPreview(
                        title = name,
                        artist = artist,
                        keySignature = keySignature,
                        chart = previewChart,
                        previewOptions = previewOptions,
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

    if (showUltimateGuitarSearch) {
        UltimateGuitarSearchDialog(
            initialQuery = importQuery.ifBlank {
                listOfNotNull(
                    name.takeIf { it.isNotBlank() },
                    artist.takeIf { it.isNotBlank() },
                ).joinToString(" ")
            },
            onDismiss = { showUltimateGuitarSearch = false },
            onSearch = songRepository::searchUltimateGuitar,
            onResultSelected = { result ->
                showUltimateGuitarSearch = false
                importQuery = listOfNotNull(
                    result.title.takeIf { it.isNotBlank() },
                    result.artist.takeIf { it.isNotBlank() },
                ).joinToString(" ")
                launchSearchResultImport(result)
            },
        )
    }

    if (showUltimateGuitarSearchDisclaimer) {
        UltimateGuitarSearchDisclaimerDialog(
            onDismiss = { showUltimateGuitarSearchDisclaimer = false },
            onContinue = {
                ultimateGuitarConsentRepository.acceptSearchDisclaimer()
                hasAcceptedUltimateGuitarSearchDisclaimer = true
                showUltimateGuitarSearchDisclaimer = false
                showUltimateGuitarSearch = true
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

@Composable
private fun UltimateGuitarSearchDisclaimerDialog(
    onDismiss: () -> Unit,
    onContinue: () -> Unit,
) {
    var hasAcceptedTerms by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onContinue,
                enabled = hasAcceptedTerms,
            ) {
                Text("Continue")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = {
            Text("Ultimate Guitar disclaimer")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "This search works like searching directly on the Ultimate Guitar website. We are not affiliated with, endorsed by, or sponsored by Ultimate Guitar.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "By continuing, you confirm that you are responsible for how you use any results or imported charts, and you agree to indemnify this app and its developers for any improper or unlawful use.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Checkbox(
                        checked = hasAcceptedTerms,
                        onCheckedChange = { hasAcceptedTerms = it },
                    )
                    Text(
                        text = "I understand and accept this disclaimer.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
        },
    )
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
    importQuery: String,
    showCompressedVersion: Boolean,
    isImporting: Boolean,
    feedbackMessage: String?,
    onNameChange: (String) -> Unit,
    onArtistChange: (String) -> Unit,
    onPresetChange: (String) -> Unit,
    onKeyChange: (String) -> Unit,
    onChartChange: (String) -> Unit,
    onTransposeUp: () -> Unit,
    onTransposeDown: () -> Unit,
    onImportQueryChange: (String) -> Unit,
    onImportClick: () -> Unit,
    onShowCompressedVersionChange: (Boolean) -> Unit,
) {
    var showNotationHelp by remember { mutableStateOf(false) }
    var showCompressedVersionHelp by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
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

        OutlinedTextField(
            value = importQuery,
            onValueChange = onImportQueryChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Search Ultimate Guitar") },
            placeholder = { Text("Song title artist") },
            singleLine = true,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = onImportClick,
                enabled = importQuery.isNotBlank() && !isImporting,
            ) {
                Icon(Icons.Outlined.Search, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Search Tabs")
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Switch(
                checked = showCompressedVersion,
                onCheckedChange = onShowCompressedVersionChange,
            )
            Text(
                text = "Edit compressed chart",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { showCompressedVersionHelp = true }) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = "Compressed version help",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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

        OutlinedTextField(
            value = chart,
            onValueChange = onChartChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp),
            label = {
                Text(
                    if (showCompressedVersion) {
                        "Compressed chart"
                    } else {
                        "Chart"
                    },
                )
            },
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
            supportingText = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "To write notation use @ ... @ blocks with MML notation",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    IconButton(
                        onClick = { showNotationHelp = true },
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = "View MML glossary",
                            modifier = Modifier.size(22.dp),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            },
        )

        if (showNotationHelp) {
            NotationHelpDialog(
                onDismiss = { showNotationHelp = false },
            )
        }

        if (showCompressedVersionHelp) {
            CompressedVersionHelpDialog(
                onDismiss = { showCompressedVersionHelp = false },
            )
        }
    }
}

@Composable
private fun CompressedVersionHelpDialog(
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        },
        title = {
            Text("Compressed version")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "This shows the same chord-only compressed layout used in live view when Compress is on.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "If you edit this version and save, StageSet stores it separately from the full chart.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "That saved compressed version will be used in play mode whenever Compress is enabled, instead of rebuilding one automatically from the main chart.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}

@Composable
private fun NotationHelpDialog(
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        },
        title = {
            Text("MML glossary")
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                GlossaryLine(
                    text = "Write notation inside @ ... @ blocks. Each example below shows the rendered notation first, then the MML that creates it.",
                )
                NotationGlossarySection(
                    title = "Notes",
                    details = listOf(
                        "A-G notes",
                        "# sharp, b flat, n natural",
                        "r rest",
                    ),
                    snippet = "o4 l4 c d e r f# g a bb",
                )
                NotationGlossarySection(
                    title = "Rhythm",
                    details = listOf(
                        "2 half notes, 4 quarter notes, 8 eighth notes, and so on",
                        "& ties two notes of the same pitch",
                        "Note duration follows the last set duration",
                    ),
                    snippet = "o4 c4 e8 f g16 a bb a&a4",
                )
                NotationGlossarySection(
                    title = "Octaves",
                    details = listOf(
                        "o4 middle octave, o5 fifth octave, and so on",
                        "> and < go up and down an octave",
                    ),
                    snippet = "o4 a16 b o5 c d e d c < b a2",
                )
                NotationGlossarySection(
                    title = "Score settings",
                    details = listOf(
                        "key=Ebm meter=4/4 cleff=treble",
                        "If not set it is not visible",
                    ),
                    snippet = "key=Ebm meter=4/4 cleff=treble o4 l4 c d eb f",
                )
            }
        },
    )
}

@Composable
private fun GlossaryLine(
    text: String,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun NotationGlossarySection(
    title: String,
    details: List<String>,
    snippet: String,
) {
    val parsedNotation = remember(snippet) { parseMelodyNotation(snippet) }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
        )
        details.forEach { detail ->
            Text(
                text = detail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        when (parsedNotation) {
            is MelodyParseResult.Success -> {
                MelodyStaffPreview(
                    notation = parsedNotation.notation,
                    scale = 0.82f,
                )
            }

            is MelodyParseResult.Error -> {
                Text(
                    text = parsedNotation.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        Text(
            text = snippet,
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
