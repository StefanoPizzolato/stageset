@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.codex.stageset.ui.setlists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codex.stageset.data.repository.PreviewSettingsRepository
import com.codex.stageset.data.repository.SetlistPreviewDetail
import com.codex.stageset.data.repository.SetlistPreviewSong
import com.codex.stageset.data.repository.SetlistRepository
import com.codex.stageset.ui.common.ChartPreview
import com.codex.stageset.ui.common.PreviewRenderOptions
import com.codex.stageset.ui.common.PreviewSettingsDialog
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

@Composable
fun SetlistPreviewRoute(
    setlistId: Long,
    previewSettingsRepository: PreviewSettingsRepository,
    setlistRepository: SetlistRepository,
    restoredSongIndex: Int? = null,
    onRestoreSongIndexConsumed: () -> Unit = {},
    onBack: () -> Unit,
    onEditSong: (Long, Int) -> Unit,
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
    val drawerState = androidx.compose.material3.rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var textSizeSp by rememberSaveable(setlistId) { mutableIntStateOf(20) }
    var currentSongIndex by rememberSaveable(setlistId) { mutableIntStateOf(0) }
    var showSettingsDialog by rememberSaveable(setlistId) { mutableStateOf(false) }
    var songSearchQuery by rememberSaveable(setlistId) { mutableStateOf("") }

    LaunchedEffect(setlist?.songs?.size) {
        val songCount = setlist?.songs?.size ?: 0
        currentSongIndex = when {
            songCount <= 0 -> 0
            currentSongIndex >= songCount -> songCount - 1
            else -> currentSongIndex
        }
    }

    val songs = setlist?.songs.orEmpty()

    LaunchedEffect(restoredSongIndex, songs.size) {
        val targetSongIndex = restoredSongIndex ?: return@LaunchedEffect
        if (songs.isEmpty()) {
            return@LaunchedEffect
        }

        currentSongIndex = targetSongIndex.coerceIn(0, songs.lastIndex)
        onRestoreSongIndexConsumed()
    }
    val currentSong = songs.getOrNull(currentSongIndex)
    val previousSong = songs.getOrNull(currentSongIndex - 1)
    val nextSong = songs.getOrNull(currentSongIndex + 1)
    val canGoBack = currentSongIndex > 0
    val canGoForward = currentSongIndex < songs.lastIndex
    val filteredSongs = remember(songs, songSearchQuery) {
        filterSetlistSongs(
            songs = songs,
            query = songSearchQuery,
        )
    }
    val compactTopBar = LocalConfiguration.current.screenWidthDp < 600
    val setlistTitle = setlist?.name?.ifBlank { "Play" } ?: "Play"
    val currentSongTitle = buildSongPlayTitle(
        preset = currentSong?.preset.orEmpty(),
        keySignature = currentSong?.keySignature.orEmpty(),
        songName = currentSong?.name.orEmpty(),
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = songs.isNotEmpty(),
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.widthIn(max = 360.dp),
            ) {
                SetlistSongPickerPanel(
                    setlistName = setlist?.name,
                    searchQuery = songSearchQuery,
                    onSearchQueryChange = { songSearchQuery = it },
                    songs = filteredSongs,
                    currentSongIndex = currentSongIndex,
                    onSongSelected = { selectedIndex ->
                        currentSongIndex = selectedIndex
                        songSearchQuery = ""
                        scope.launch {
                            drawerState.close()
                        }
                    },
                )
            }
        },
    ) {
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
                            onClick = {
                                scope.launch {
                                    if (drawerState.isClosed) {
                                        drawerState.open()
                                    } else {
                                        drawerState.close()
                                    }
                                }
                            },
                            enabled = songs.isNotEmpty(),
                        ) {
                            Icon(Icons.Outlined.Menu, contentDescription = "Setlist songs")
                        }
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
                        currentSong?.let {
                            if (compactTopBar) {
                                IconButton(onClick = { onEditSong(it.songId, currentSongIndex) }) {
                                    Icon(Icons.Outlined.Edit, contentDescription = "Edit song")
                                }
                            } else {
                                FilledTonalButton(onClick = { onEditSong(it.songId, currentSongIndex) }) {
                                    Icon(Icons.Outlined.Edit, contentDescription = "Edit song")
                                }
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
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            val previousSongTitle = buildSongPlayTitle(
                                preset = previousSong?.preset.orEmpty(),
                                keySignature = previousSong?.keySignature.orEmpty(),
                                songName = previousSong?.name.orEmpty(),
                            )
                            val nextSongTitle = buildSongPlayTitle(
                                preset = nextSong?.preset.orEmpty(),
                                keySignature = nextSong?.keySignature.orEmpty(),
                                songName = nextSong?.name.orEmpty(),
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                PlayTransportButton(
                                    onClick = { currentSongIndex-- },
                                    enabled = canGoBack,
                                    songTitle = previousSongTitle,
                                    iconBeforeTitle = true,
                                    modifier = Modifier
                                        .weight(1f),
                                ) {
                                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                                }
                                PlayTransportButton(
                                    onClick = { currentSongIndex++ },
                                    enabled = canGoForward,
                                    songTitle = nextSongTitle,
                                    iconBeforeTitle = false,
                                    modifier = Modifier
                                        .weight(1f),
                                ) {
                                    Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null)
                                }
                            }
                            Text(
                                text = currentSongTitle,
                                modifier = Modifier.fillMaxWidth(),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "${currentSongIndex + 1} / ${songs.size}",
                                modifier = Modifier.fillMaxWidth(),
                                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )

                            ChartPreview(
                                title = currentSong?.name.orEmpty(),
                                artist = currentSong?.artist.orEmpty(),
                                keySignature = currentSong?.keySignature.orEmpty(),
                                chart = currentSong?.chart.orEmpty(),
                                compressedChart = currentSong?.compressedChart,
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
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                            )
                        }
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

@Composable
private fun PlayTransportButton(
    onClick: () -> Unit,
    enabled: Boolean,
    songTitle: String,
    iconBeforeTitle: Boolean,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(80.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (iconBeforeTitle) {
                icon()
                Text(
                    text = songTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .weight(1f),
                )
            } else {
                Text(
                    text = songTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .weight(1f),
                )
                icon()
            }
        }
    }
}

private fun buildSongPlayTitle(
    preset: String,
    keySignature: String,
    songName: String,
): String {
    val resolvedSongName = songName.ifBlank { "Song" }
    val lead = listOfNotNull(
        preset.takeIf { it.isNotBlank() },
        keySignature.takeIf { it.isNotBlank() },
    ).joinToString(separator = " - ")

    return if (lead.isBlank()) {
        resolvedSongName
    } else {
        "$lead | $resolvedSongName"
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

@Composable
private fun SetlistSongPickerPanel(
    setlistName: String?,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    songs: List<IndexedValue<SetlistPreviewSong>>,
    currentSongIndex: Int,
    onSongSelected: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Setlist Songs",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        setlistName?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Search songs") },
            placeholder = { Text("Title, artist, preset, or number") },
            singleLine = true,
        )
        if (songs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (searchQuery.isBlank()) {
                        "No songs in this setlist."
                    } else {
                        "No songs match that search."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(
                    items = songs,
                    key = { indexedSong -> indexedSong.value.entryId },
                ) { indexedSong ->
                    val song = indexedSong.value
                    val isCurrent = indexedSong.index == currentSongIndex
                    val details = listOfNotNull(
                        song.artist.takeIf { it.isNotBlank() },
                        song.preset.takeIf { it.isNotBlank() },
                        song.keySignature.takeIf { it.isNotBlank() }?.let { "Key $it" },
                    ).joinToString(" - ")

                    Surface(
                        color = if (isCurrent) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                        },
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSongSelected(indexedSong.index) }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = "${indexedSong.index + 1}. ${song.name.ifBlank { "Untitled song" }}",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isCurrent) {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (details.isNotBlank()) {
                                Text(
                                    text = details,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isCurrent) {
                                        MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun filterSetlistSongs(
    songs: List<SetlistPreviewSong>,
    query: String,
): List<IndexedValue<SetlistPreviewSong>> {
    val indexedSongs = songs.withIndex().toList()
    val tokens = query.trim()
        .lowercase()
        .split(Regex("""\s+"""))
        .filter { it.isNotBlank() }

    if (tokens.isEmpty()) {
        return indexedSongs
    }

    return indexedSongs.filter { indexedSong ->
        val searchableText = buildString {
            append(indexedSong.index + 1)
            append(' ')
            append(indexedSong.value.name)
            append(' ')
            append(indexedSong.value.artist)
            append(' ')
            append(indexedSong.value.preset)
            append(' ')
            append(indexedSong.value.keySignature)
        }.lowercase()

        tokens.all(searchableText::contains)
    }
}
