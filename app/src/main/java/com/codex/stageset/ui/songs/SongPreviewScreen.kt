@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.codex.stageset.ui.songs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.codex.stageset.data.repository.PreviewSettingsRepository
import com.codex.stageset.data.repository.Song
import com.codex.stageset.data.repository.SongRepository
import com.codex.stageset.ui.common.ChartPreview
import com.codex.stageset.ui.common.PreviewSettingsDialog
import com.codex.stageset.ui.common.PreviewRenderOptions
import com.codex.stageset.ui.common.StageSetTopAppBar
import com.codex.stageset.ui.common.buildPreviewTitle
import kotlinx.coroutines.flow.flowOf

@Composable
fun SongPreviewRoute(
    songId: Long,
    previewSettingsRepository: PreviewSettingsRepository,
    songRepository: SongRepository,
    onBack: () -> Unit,
    onEditSong: (Long) -> Unit,
) {
    val songFlow = remember(songId, songRepository) {
        if (songId > 0) {
            songRepository.observeSong(songId)
        } else {
            flowOf<Song?>(null)
        }
    }
    val song by songFlow.collectAsState(initial = null)
    val previewSettings by previewSettingsRepository.settings.collectAsState()
    var textSizeSp by rememberSaveable { mutableIntStateOf(20) }
    var showSettingsDialog by rememberSaveable { mutableStateOf(false) }
    val compactTopBar = LocalConfiguration.current.screenWidthDp < 600

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0f),
        topBar = {
            StageSetTopAppBar(
                title = {
                    Text(
                        text = song?.let { buildPreviewTitle(it.preset, it.keySignature, it.name) }
                            ?: buildPreviewTitle("", "", "Song Preview"),
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
                        enabled = song != null && textSizeSp > 12,
                    ) {
                        Icon(Icons.Outlined.Remove, contentDescription = "Smaller text")
                    }
                    IconButton(
                        onClick = { textSizeSp = (textSizeSp + 2).coerceAtMost(34) },
                        enabled = song != null && textSizeSp < 34,
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = "Larger text")
                    }
                    IconButton(
                        onClick = { showSettingsDialog = true },
                        enabled = song != null,
                    ) {
                        Icon(Icons.Outlined.Tune, contentDescription = "Preview settings")
                    }
                    song?.let {
                        if (compactTopBar) {
                            IconButton(onClick = { onEditSong(it.id) }) {
                                Icon(Icons.Outlined.Edit, contentDescription = "Edit song")
                            }
                        } else {
                            FilledTonalButton(onClick = { onEditSong(it.id) }) {
                                Icon(Icons.Outlined.Edit, contentDescription = "Edit song")
                            }
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        if (song == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(0.8f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Song unavailable",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        text = "That song could not be loaded from the library.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
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
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    ChartPreview(
                        title = song?.name.orEmpty(),
                        artist = song?.artist.orEmpty(),
                        keySignature = song?.keySignature.orEmpty(),
                        chart = song?.chart.orEmpty(),
                        compressedChart = song?.compressedChart,
                        showHeader = false,
                        textSizeSp = textSizeSp,
                        previewOptions = PreviewRenderOptions(
                            showLyrics = previewSettings.showLyrics,
                            showLyricsCue = previewSettings.showLyricsCue,
                            showChords = previewSettings.showChords,
                            showNotation = previewSettings.showNotation,
                            hideRepeatedSectionChords = previewSettings.hideRepeatedSectionChords,
                            compressChords = previewSettings.compressChords,
                            colorizeSectionHeadings = previewSettings.colorizeSectionHeadings,
                            twoColumns = previewSettings.twoColumns,
                        ),
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }

    if (showSettingsDialog) {
        PreviewSettingsDialog(
            settings = previewSettings,
            title = "Select your song viewing options",
            onDismiss = { showSettingsDialog = false },
            onShowLyricsChange = previewSettingsRepository::setShowLyrics,
            onShowLyricsCueChange = previewSettingsRepository::setShowLyricsCue,
            onShowChordsChange = previewSettingsRepository::setShowChords,
            onShowNotationChange = previewSettingsRepository::setShowNotation,
            onHideRepeatedSectionChordsChange = previewSettingsRepository::setHideRepeatedSectionChords,
            onCompressChordsChange = previewSettingsRepository::setCompressChords,
            onColorizeSectionHeadingsChange = previewSettingsRepository::setColorizeSectionHeadings,
            onTwoColumnsChange = previewSettingsRepository::setTwoColumns,
        )
    }
}
