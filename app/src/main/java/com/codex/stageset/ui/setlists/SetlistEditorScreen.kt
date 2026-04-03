@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.codex.stageset.ui.setlists

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.codex.stageset.data.repository.SetlistArchiveDocument
import com.codex.stageset.data.repository.SetlistDetail
import com.codex.stageset.data.repository.SetlistDraft
import com.codex.stageset.data.repository.SetlistRepository
import com.codex.stageset.data.repository.Song
import com.codex.stageset.data.repository.SongRepository
import com.codex.stageset.ui.common.ConfirmActionDialog
import com.codex.stageset.ui.common.writeUtf8Text
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetlistEditorRoute(
    setlistId: Long,
    songRepository: SongRepository,
    setlistRepository: SetlistRepository,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val allSongs by songRepository.observeSongs().collectAsState(initial = emptyList())
    val existingSetlistFlow = remember(setlistId, setlistRepository) {
        if (setlistId > 0) {
            setlistRepository.observeSetlist(setlistId)
        } else {
            flowOf<SetlistDetail?>(null)
        }
    }
    val existingSetlist by existingSetlistFlow.collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val queuedSongIds = remember(setlistId) { mutableStateListOf<Long>() }
    var pendingArchiveDocument by remember { mutableStateOf<SetlistArchiveDocument?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var pendingRemovalIndex by remember { mutableStateOf<Int?>(null) }

    var name by rememberSaveable(setlistId) { mutableStateOf("") }
    var notes by rememberSaveable(setlistId) { mutableStateOf("") }
    var search by rememberSaveable(setlistId) { mutableStateOf("") }
    var hasHydrated by rememberSaveable(setlistId) { mutableStateOf(setlistId <= 0) }
    val exportArchiveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        val archiveDocument = pendingArchiveDocument
        pendingArchiveDocument = null

        if (uri == null || archiveDocument == null) {
            return@rememberLauncherForActivityResult
        }

        scope.launch {
            runCatching {
                context.contentResolver.writeUtf8Text(uri, archiveDocument.json)
            }.onSuccess {
                snackbarHostState.showSnackbar("Archive saved.")
            }.onFailure { throwable ->
                snackbarHostState.showSnackbar(
                    throwable.message ?: "Couldn't save that archive.",
                )
            }
        }
    }

    LaunchedEffect(existingSetlist?.id) {
        if (!hasHydrated && existingSetlist != null) {
            name = existingSetlist?.name.orEmpty()
            notes = existingSetlist?.notes.orEmpty()
            queuedSongIds.clear()
            queuedSongIds.addAll(existingSetlist?.songs.orEmpty().sortedBy { it.position }.map { it.songId })
            hasHydrated = true
        }
    }

    val filteredSongPool = remember(allSongs, search) {
        allSongs.filter { song ->
            search.isBlank() || buildString {
                append(song.name)
                append(' ')
                append(song.artist)
                append(' ')
                append(song.preset)
                append(' ')
                append(song.keySignature)
            }.contains(search.trim(), ignoreCase = true)
        }
    }
    val songMap = remember(allSongs) { allSongs.associateBy { it.id } }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val wideLayout = maxWidth >= 760.dp
        val songPoolWidth = (maxWidth * 0.4f).coerceIn(320.dp, 460.dp)

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0f),
            snackbarHost = {
                SnackbarHost(snackbarHostState)
            },
            topBar = {
                TopAppBar(
                    title = { Text(if (setlistId > 0) "Edit Setlist" else "New Setlist") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (setlistId > 0) {
                            TextButton(
                                onClick = {
                                    scope.launch {
                                        runCatching {
                                            setlistRepository.exportSetlistArchive(setlistId).getOrThrow()
                                        }.onSuccess { archiveDocument ->
                                            pendingArchiveDocument = archiveDocument
                                            exportArchiveLauncher.launch(archiveDocument.suggestedFileName)
                                        }.onFailure { throwable ->
                                            snackbarHostState.showSnackbar(
                                                throwable.message ?: "Couldn't save that archive.",
                                            )
                                        }
                                    }
                                },
                            ) {
                                Text("Export Setlist")
                            }
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
                                    setlistRepository.saveSetlist(
                                        setlistId = setlistId.takeIf { it > 0 },
                                        draft = SetlistDraft(
                                            name = name,
                                            notes = notes,
                                            songIds = queuedSongIds.toList(),
                                        ),
                                    )
                                    onBack()
                                }
                            },
                            enabled = name.isNotBlank(),
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
                    SongPoolPane(
                        songs = filteredSongPool,
                        search = search,
                        onSearchChange = { search = it },
                        onAddSong = { queuedSongIds.add(it.id) },
                        modifier = Modifier
                            .width(songPoolWidth)
                            .fillMaxHeight(),
                    )
                    SetlistQueuePane(
                        name = name,
                        notes = notes,
                        queuedSongs = queuedSongIds.mapNotNull { songMap[it] },
                        onNameChange = { name = it },
                        onNotesChange = { notes = it },
                        onMoveUp = { index ->
                            if (index > 0) {
                                val item = queuedSongIds.removeAt(index)
                                queuedSongIds.add(index - 1, item)
                            }
                        },
                        onMoveDown = { index ->
                            if (index < queuedSongIds.lastIndex) {
                                val item = queuedSongIds.removeAt(index)
                                queuedSongIds.add(index + 1, item)
                            }
                        },
                        onRemove = { index -> pendingRemovalIndex = index },
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
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    SongPoolPane(
                        songs = filteredSongPool,
                        search = search,
                        onSearchChange = { search = it },
                        onAddSong = { queuedSongIds.add(it.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    )
                    SetlistQueuePane(
                        name = name,
                        notes = notes,
                        queuedSongs = queuedSongIds.mapNotNull { songMap[it] },
                        onNameChange = { name = it },
                        onNotesChange = { notes = it },
                        onMoveUp = { index ->
                            if (index > 0) {
                                val item = queuedSongIds.removeAt(index)
                                queuedSongIds.add(index - 1, item)
                            }
                        },
                        onMoveDown = { index ->
                            if (index < queuedSongIds.lastIndex) {
                                val item = queuedSongIds.removeAt(index)
                                queuedSongIds.add(index + 1, item)
                            }
                        },
                        onRemove = { index -> pendingRemovalIndex = index },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1.15f),
                    )
                }
            }
        }
    }

    if (showDeleteConfirmation) {
        ConfirmActionDialog(
            title = "Delete setlist?",
            message = "This will permanently remove this setlist from the library.",
            confirmLabel = "Delete",
            onConfirm = {
                showDeleteConfirmation = false
                scope.launch {
                    setlistRepository.deleteSetlist(setlistId)
                    onBack()
                }
            },
            onDismiss = { showDeleteConfirmation = false },
        )
    }

    pendingRemovalIndex?.let { index ->
        val songName = queuedSongIds.getOrNull(index)
            ?.let(songMap::get)
            ?.name
            .orEmpty()
            .ifBlank { "this song" }
        ConfirmActionDialog(
            title = "Remove song from setlist?",
            message = "This will remove $songName from the current running order.",
            confirmLabel = "Remove",
            onConfirm = {
                val confirmedIndex = pendingRemovalIndex
                pendingRemovalIndex = null
                if (confirmedIndex != null && confirmedIndex in queuedSongIds.indices) {
                    queuedSongIds.removeAt(confirmedIndex)
                }
            },
            onDismiss = { pendingRemovalIndex = null },
        )
    }
}

@Composable
private fun SongPoolPane(
    songs: List<Song>,
    search: String,
    onSearchChange: (String) -> Unit,
    onAddSong: (Song) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "Song pool",
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = "Pick songs from the main library and append them to the set order.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = search,
                onValueChange = onSearchChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search library") },
                singleLine = true,
            )
            AssistChip(
                onClick = {},
                label = { Text("${songs.size} available songs") },
                leadingIcon = { Icon(Icons.Outlined.LibraryMusic, contentDescription = null) },
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                itemsIndexed(
                    items = songs,
                    key = { _, song -> song.id },
                ) { _, song ->
                    Card {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
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
                                    ).joinToString(" - "),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Button(onClick = { onAddSong(song) }) {
                                Icon(Icons.Outlined.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Add")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SetlistQueuePane(
    name: String,
    notes: String,
    queuedSongs: List<Song>,
    onNameChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Set order",
                style = MaterialTheme.typography.headlineMedium,
            )
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Setlist name") },
                singleLine = true,
            )
            OutlinedTextField(
                value = notes,
                onValueChange = onNotesChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Show notes") },
                minLines = 2,
            )
            AssistChip(
                onClick = {},
                label = { Text("${queuedSongs.size} songs in order") },
            )
            if (queuedSongs.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "Your running order is empty.",
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            text = "Add songs from the library, then move them up or down for the live flow.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    itemsIndexed(
                        items = queuedSongs,
                        key = { index, song -> "${song.id}-$index" },
                    ) { index, song ->
                        Card {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(
                                        text = "${index + 1}. ${song.name}",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        text = listOfNotNull(
                                            song.artist.takeIf { it.isNotBlank() },
                                            song.preset.takeIf { it.isNotBlank() },
                                            song.keySignature.takeIf { it.isNotBlank() }?.let { "Key $it" },
                                        ).joinToString(" - "),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    IconButton(
                                        onClick = { onMoveUp(index) },
                                        enabled = index > 0,
                                    ) {
                                        Icon(Icons.Outlined.ArrowUpward, contentDescription = "Move up")
                                    }
                                    IconButton(
                                        onClick = { onMoveDown(index) },
                                        enabled = index < queuedSongs.lastIndex,
                                    ) {
                                        Icon(Icons.Outlined.ArrowDownward, contentDescription = "Move down")
                                    }
                                    IconButton(onClick = { onRemove(index) }) {
                                        Icon(
                                            Icons.Outlined.RemoveCircleOutline,
                                            contentDescription = "Remove song",
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
