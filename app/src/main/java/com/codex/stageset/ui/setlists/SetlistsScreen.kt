@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.codex.stageset.ui.setlists

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.codex.stageset.data.repository.SetlistDetail
import com.codex.stageset.data.repository.SetlistRepository
import com.codex.stageset.data.repository.SetlistSummary
import com.codex.stageset.ui.common.StageSetTopAppBar
import com.codex.stageset.ui.common.readUtf8Text
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetlistsRoute(
    setlistRepository: SetlistRepository,
    onCreateSetlist: () -> Unit,
    onOpenSetlist: (Long) -> Unit,
    onEditSetlist: (Long) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val setlists by setlistRepository.observeSetlists().collectAsState(initial = emptyList())
    var selectedSetlistId by rememberSaveable { mutableLongStateOf(-1L) }
    val importArchiveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) {
            return@rememberLauncherForActivityResult
        }

        scope.launch {
            runCatching {
                val archiveJson = context.contentResolver.readUtf8Text(uri)
                setlistRepository.importSetlistArchive(archiveJson).getOrThrow()
            }.onSuccess { result ->
                selectedSetlistId = result.setlistId
                snackbarHostState.showSnackbar(
                    "Loaded ${result.songCount} songs into ${result.setlistName}.",
                )
            }.onFailure { throwable ->
                snackbarHostState.showSnackbar(
                    throwable.message ?: "Couldn't load that archive.",
                )
            }
        }
    }

    LaunchedEffect(setlists) {
        if (setlists.isNotEmpty() && setlists.none { it.id == selectedSetlistId }) {
            selectedSetlistId = setlists.first().id
        }
    }

    val selectedDetailFlow = remember(selectedSetlistId, setlistRepository) {
        if (selectedSetlistId > 0) {
            setlistRepository.observeSetlist(selectedSetlistId)
        } else {
            flowOf<SetlistDetail?>(null)
        }
    }
    val selectedDetail by selectedDetailFlow.collectAsState(initial = null)

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val compactTopBar = maxWidth < 600.dp
        val wideLayout = maxWidth >= 1000.dp

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0f),
            snackbarHost = {
                SnackbarHost(snackbarHostState)
            },
            topBar = {
                StageSetTopAppBar(
                    title = {
                        Text(
                            text = "Setlists",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    actions = {
                        if (compactTopBar) {
                            TextButton(
                                onClick = {
                                    importArchiveLauncher.launch(
                                        arrayOf("application/json", "text/plain", "*/*"),
                                    )
                                },
                            ) {
                                Text("Import setlist")
                            }
                            androidx.compose.material3.IconButton(onClick = onCreateSetlist) {
                                Icon(Icons.Outlined.Add, contentDescription = "New setlist")
                            }
                        } else {
                            TextButton(
                                onClick = {
                                    importArchiveLauncher.launch(
                                        arrayOf("application/json", "text/plain", "*/*"),
                                    )
                                },
                            ) {
                                Text("Import setlist")
                            }
                            FilledTonalButton(onClick = onCreateSetlist) {
                                Icon(Icons.Outlined.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("New Setlist")
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
                    SetlistLibraryPane(
                        setlists = setlists,
                        selectedSetlistId = selectedSetlistId,
                        onSelectSetlist = { selectedSetlistId = it },
                        onOpenSetlist = onOpenSetlist,
                        onEditSetlist = onEditSetlist,
                        modifier = Modifier
                            .width(400.dp)
                            .fillMaxHeight(),
                    )

                    if (selectedDetail != null) {
                        Card(
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = selectedDetail?.name.orEmpty(),
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                        Text(
                                            text = "${selectedDetail?.songs?.size ?: 0} songs",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        FilledTonalButton(onClick = { onOpenSetlist(selectedDetail?.id ?: -1L) }) {
                                            Icon(
                                                Icons.AutoMirrored.Outlined.PlaylistPlay,
                                                contentDescription = null,
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Preview")
                                        }
                                        FilledTonalButton(onClick = { onEditSetlist(selectedDetail?.id ?: -1L) }) {
                                            Icon(Icons.Outlined.EditNote, contentDescription = null)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Edit Setlist")
                                        }
                                    }
                                }
                                if (selectedDetail?.notes?.isNotBlank() == true) {
                                    Text(
                                        text = selectedDetail?.notes.orEmpty(),
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                }
                                HorizontalDivider()
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    items(
                                        items = selectedDetail?.songs.orEmpty(),
                                        key = { it.entryId },
                                    ) { song ->
                                        Card {
                                            Column(
                                                modifier = Modifier.padding(16.dp),
                                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                            ) {
                                                Text(
                                                    text = "${song.position + 1}. ${song.name}",
                                                    style = MaterialTheme.typography.titleLarge,
                                                )
                                                Text(
                                text = listOfNotNull(
                                    song.artist.takeIf { it.isNotBlank() },
                                    song.keySignature.takeIf { it.isNotBlank() }?.let { "Key $it" },
                                ).joinToString(" - "),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        SetlistEmptyState(
                            title = "Ready for rehearsal",
                            body = "Create a setlist and build the order from your saved song library.",
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            } else {
                SetlistLibraryPane(
                    setlists = setlists,
                    selectedSetlistId = selectedSetlistId,
                    onSelectSetlist = { selectedSetlistId = it },
                    onOpenSetlist = onOpenSetlist,
                    onEditSetlist = onEditSetlist,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun SetlistLibraryPane(
    setlists: List<SetlistSummary>,
    selectedSetlistId: Long,
    onSelectSetlist: (Long) -> Unit,
    onOpenSetlist: (Long) -> Unit,
    onEditSetlist: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AssistChip(
            onClick = {},
            label = { Text("${setlists.size} setlists") },
            leadingIcon = { Icon(Icons.AutoMirrored.Outlined.PlaylistPlay, contentDescription = null) },
        )

        if (setlists.isEmpty()) {
            SetlistEmptyState(
                title = "No setlists yet",
                body = "Build a running order from your song pool and reorder it for the live set.",
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(
                    items = setlists,
                    key = { it.id },
                ) { setlist ->
                    val selected = setlist.id == selectedSetlistId
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            onSelectSetlist(setlist.id)
                            onOpenSetlist(setlist.id)
                        },
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = setlist.name,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                    )
                                    Text(
                                        text = "${setlist.songCount} songs",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                FilledTonalButton(onClick = { onEditSetlist(setlist.id) }) {
                                    Icon(Icons.Outlined.EditNote, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Edit")
                                }
                            }
                            if (setlist.notes.isNotBlank()) {
                                Text(
                                    text = setlist.notes,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
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
private fun SetlistEmptyState(
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
