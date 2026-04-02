@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.codex.stageset.ui.setlists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codex.stageset.data.repository.PreviewSettingsRepository
import com.codex.stageset.data.repository.SetlistPreviewDetail
import com.codex.stageset.data.repository.SetlistRepository
import com.codex.stageset.ui.common.ChartPreview
import com.codex.stageset.ui.common.PreviewRenderOptions
import com.codex.stageset.ui.common.PreviewSettingsDialog
import com.codex.stageset.ui.common.buildPreviewTitle
import kotlinx.coroutines.flow.flowOf

@Composable
fun SetlistPreviewRoute(
    setlistId: Long,
    previewSettingsRepository: PreviewSettingsRepository,
    setlistRepository: SetlistRepository,
    onBack: () -> Unit,
    onEditSetlist: (Long) -> Unit,
) {
    val setlistFlow = remember(setlistId, setlistRepository) {
        if (setlistId > 0) {
            setlistRepository.observeSetlistPreview(setlistId)
        } else {
            flowOf<SetlistPreviewDetail?>(null)
        }
    }
    val setlist by setlistFlow.collectAsState(initial = null)
    val previewSettings by previewSettingsRepository.settings.collectAsState()
    var textSizeSp by rememberSaveable(setlistId) { mutableIntStateOf(20) }
    var currentSongIndex by rememberSaveable(setlistId) { mutableIntStateOf(0) }
    var showSettingsDialog by rememberSaveable(setlistId) { mutableStateOf(false) }

    LaunchedEffect(setlist?.songs?.size) {
        val songCount = setlist?.songs?.size ?: 0
        currentSongIndex = when {
            songCount <= 0 -> 0
            currentSongIndex >= songCount -> songCount - 1
            else -> currentSongIndex
        }
    }

    val songs = setlist?.songs.orEmpty()
    val currentSong = songs.getOrNull(currentSongIndex)
    val previousSong = songs.getOrNull(currentSongIndex - 1)
    val nextSong = songs.getOrNull(currentSongIndex + 1)
    val canGoBack = currentSongIndex > 0
    val canGoForward = currentSongIndex < songs.lastIndex
    val setlistTitle = buildSetlistPlayTitle(
        setlistName = setlist?.name,
        preset = currentSong?.preset.orEmpty(),
        keySignature = currentSong?.keySignature.orEmpty(),
        songName = currentSong?.name.orEmpty(),
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0f),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = setlistTitle,
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
                    IconButton(
                        onClick = { textSizeSp = (textSizeSp - 2).coerceAtLeast(12) },
                        enabled = currentSong != null && textSizeSp > 12,
                    ) {
                        Icon(Icons.Outlined.Remove, contentDescription = "Smaller text")
                    }
                    IconButton(
                        onClick = { textSizeSp = (textSizeSp + 2).coerceAtMost(34) },
                        enabled = currentSong != null && textSizeSp < 34,
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = "Larger text")
                    }
                    IconButton(
                        onClick = { showSettingsDialog = true },
                        enabled = currentSong != null,
                    ) {
                        Icon(Icons.Outlined.Tune, contentDescription = "Preview settings")
                    }
                    setlist?.let {
                        FilledTonalButton(onClick = { onEditSetlist(it.id) }) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Edit setlist")
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        when {
            setlist == null -> {
                SetlistPreviewMessage(
                    title = "Setlist unavailable",
                    body = "That setlist could not be loaded.",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                )
            }

            songs.isEmpty() -> {
                SetlistPreviewMessage(
                    title = "This setlist is empty",
                    body = "Add songs in the setlist editor to step through them in live view.",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                )
            }

            else -> {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    val sidePadding = if (maxWidth >= 1000.dp) 40.dp else 16.dp

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = sidePadding, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            PlayTransportButton(
                                onClick = { currentSongIndex-- },
                                enabled = canGoBack,
                                modifier = Modifier
                                    .weight(1f),
                            ) {
                                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                                Text(text = "Back", modifier = Modifier.padding(start = 10.dp))
                            }
                            PlayTransportButton(
                                onClick = { currentSongIndex++ },
                                enabled = canGoForward,
                                modifier = Modifier
                                    .weight(1f),
                            ) {
                                Text(text = "Forward", modifier = Modifier.padding(end = 10.dp))
                                Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null)
                            }
                        }
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = "${currentSongIndex + 1} / ${songs.size}",
                                modifier = Modifier.align(Alignment.Center),
                                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = previousSong?.let {
                                        buildPreviewTitle(it.preset, it.keySignature, it.name)
                                    } ?: AnnotatedString(""),
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = nextSong?.let {
                                        buildPreviewTitle(it.preset, it.keySignature, it.name)
                                    } ?: AnnotatedString(""),
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }

                        ChartPreview(
                            title = currentSong?.name.orEmpty(),
                            artist = currentSong?.artist.orEmpty(),
                            keySignature = currentSong?.keySignature.orEmpty(),
                            chart = currentSong?.chart.orEmpty(),
                            showHeader = false,
                            textSizeSp = textSizeSp,
                            previewOptions = PreviewRenderOptions(
                                showLyrics = previewSettings.showLyrics,
                                hideRepeatedSectionChords = previewSettings.hideRepeatedSectionChords,
                                compressChords = previewSettings.compressChords,
                                colorizeSectionHeadings = previewSettings.colorizeSectionHeadings,
                                twoColumns = previewSettings.twoColumns,
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }

    if (showSettingsDialog) {
        PreviewSettingsDialog(
            settings = previewSettings,
            onDismiss = { showSettingsDialog = false },
            onShowLyricsChange = previewSettingsRepository::setShowLyrics,
            onHideRepeatedSectionChordsChange = previewSettingsRepository::setHideRepeatedSectionChords,
            onCompressChordsChange = previewSettingsRepository::setCompressChords,
            onColorizeSectionHeadingsChange = previewSettingsRepository::setColorizeSectionHeadings,
            onTwoColumnsChange = previewSettingsRepository::setTwoColumns,
        )
    }
}

@Composable
private fun PlayTransportButton(
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(72.dp),
    ) {
        content()
    }
}

private fun buildSetlistPlayTitle(
    setlistName: String?,
    preset: String,
    keySignature: String,
    songName: String,
): AnnotatedString {
    val resolvedSetlistName = setlistName?.ifBlank { "Play" } ?: "Play"
    val songTitle = buildPreviewTitle(
        preset = preset,
        keySignature = keySignature,
        name = songName.ifBlank { "Song" },
    )
    return buildAnnotatedString {
        append(resolvedSetlistName)
        append(" - ")
        append(songTitle)
    }
}

@Composable
private fun SetlistPreviewMessage(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(0.8f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
