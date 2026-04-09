@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.codex.stageset.ui.songs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.codex.stageset.data.repository.SongRepository
import com.codex.stageset.data.repository.SongSummary
import com.codex.stageset.ui.common.StageSetTopAppBar

@Composable
fun SongsRoute(
    songRepository: SongRepository,
    onCreateSong: () -> Unit,
    onOpenSong: (Long) -> Unit,
) {
    val songs by songRepository.observeSongSummaries().collectAsState(initial = emptyList())
    var query by rememberSaveable { mutableStateOf("") }
    val filteredSongs = remember(songs, query) {
        songs.filter { song ->
            query.isBlank() || buildString {
                append(song.name)
                append(' ')
                append(song.artist)
                append(' ')
                append(song.preset)
                append(' ')
                append(song.keySignature)
            }.contains(query.trim(), ignoreCase = true)
        }
    }
    val compactTopBar = LocalConfiguration.current.screenWidthDp < 600

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0f),
        topBar = {
            StageSetTopAppBar(
                title = {
                    Text(
                        text = "Songs",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                actions = {
                    if (compactTopBar) {
                        androidx.compose.material3.IconButton(onClick = onCreateSong) {
                            Icon(Icons.Outlined.Add, contentDescription = "New song")
                        }
                    } else {
                        FilledTonalButton(onClick = onCreateSong) {
                            Icon(Icons.Outlined.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("New Song")
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            SongLibraryPane(
                songs = filteredSongs,
                query = query,
                onQueryChange = { query = it },
                onOpenSong = onOpenSong,
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 860.dp),
            )
        }
    }
}

@Composable
private fun SongLibraryPane(
    songs: List<SongSummary>,
    query: String,
    onQueryChange: (String) -> Unit,
    onOpenSong: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Search songs") },
            singleLine = true,
        )

        AssistChip(
            onClick = {},
            label = { Text("${songs.size} songs") },
            leadingIcon = {
                Icon(Icons.Outlined.MusicNote, contentDescription = null)
            },
        )

        if (songs.isEmpty()) {
            EmptyState(
                title = "No songs yet",
                body = "Create a song manually or import one from Ultimate Guitar inside the editor.",
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(
                    items = songs,
                    key = { it.id },
                ) { song ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onOpenSong(song.id) },
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                text = song.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = listOfNotNull(
                                    song.artist.takeIf { it.isNotBlank() },
                                    song.preset.takeIf { it.isNotBlank() },
                                    song.keySignature.takeIf { it.isNotBlank() }?.let { "Key $it" },
                                ).joinToString(" - ").ifBlank { "Unknown artist" },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun EmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
