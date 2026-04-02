@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.codex.stageset.ui.songs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.codex.stageset.data.repository.PreviewSettings
import com.codex.stageset.data.repository.PreviewSettingsRepository
import com.codex.stageset.data.repository.Song
import com.codex.stageset.data.repository.SongRepository
import com.codex.stageset.ui.common.ChartPreview
import com.codex.stageset.ui.common.PreviewRenderOptions
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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0f),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = song?.let(::buildPreviewTitle) ?: AnnotatedString("Song Preview"),
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
                        FilledTonalButton(onClick = { onEditSong(it.id) }) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Edit song")
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
                        showHeader = false,
                        textSizeSp = textSizeSp,
                        previewOptions = PreviewRenderOptions(
                            showLyrics = previewSettings.showLyrics,
                            hideRepeatedSectionChords = previewSettings.hideRepeatedSectionChords,
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
            onDismiss = { showSettingsDialog = false },
            onShowLyricsChange = previewSettingsRepository::setShowLyrics,
            onHideRepeatedSectionChordsChange = previewSettingsRepository::setHideRepeatedSectionChords,
        )
    }
}

private fun buildPreviewTitle(song: Song): AnnotatedString {
    val name = song.name.ifBlank { "Song Preview" }
    val leadingParts = listOfNotNull(
        song.preset.takeIf { it.isNotBlank() },
        song.keySignature.takeIf { it.isNotBlank() },
    )

    if (leadingParts.isEmpty()) {
        return AnnotatedString(name)
    }

    return buildAnnotatedString {
        leadingParts.forEachIndexed { index, part ->
            if (index > 0) {
                append(" - ")
            }
            pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
            append(part)
            pop()
        }
        append(" | ")
        append(name)
    }
}

@Composable
private fun PreviewSettingsDialog(
    settings: PreviewSettings,
    onDismiss: () -> Unit,
    onShowLyricsChange: (Boolean) -> Unit,
    onHideRepeatedSectionChordsChange: (Boolean) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        },
        title = {
            Text("Preview settings")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Text(
                    text = "These switches apply to every song preview on this device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                PreviewSettingsRow(
                    title = "Show lyrics",
                    description = "Turn this off to keep only section labels and chord lines in the live view.",
                    checked = settings.showLyrics,
                    onCheckedChange = onShowLyricsChange,
                )
                PreviewSettingsRow(
                    title = "Hide repeated section chords",
                    description = "If later verses or choruses reuse the same chord pattern, only the first matching section keeps its chord lines.",
                    checked = settings.hideRepeatedSectionChords,
                    onCheckedChange = onHideRepeatedSectionChordsChange,
                )
            }
        },
    )
}

@Composable
private fun PreviewSettingsRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}
