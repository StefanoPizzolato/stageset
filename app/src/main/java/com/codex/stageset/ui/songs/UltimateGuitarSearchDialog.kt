@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.codex.stageset.ui.songs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.codex.stageset.data.remote.UltimateGuitarSearchResult
import kotlinx.coroutines.launch

@Composable
fun UltimateGuitarSearchDialog(
    initialQuery: String,
    onDismiss: () -> Unit,
    onSearch: suspend (String) -> Result<List<UltimateGuitarSearchResult>>,
    onResultSelected: (UltimateGuitarSearchResult) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var query by remember(initialQuery) { mutableStateOf(initialQuery) }
    var results by remember(initialQuery) { mutableStateOf(emptyList<UltimateGuitarSearchResult>()) }
    val duplicateKeys = remember(results) {
        results.groupingBy { result ->
            result.title.trim().lowercase() to result.artist.trim().lowercase()
        }.eachCount()
    }
    var statusMessage by remember(initialQuery) {
        mutableStateOf(
            if (initialQuery.isBlank()) {
                "Search for a song the same way you would on Ultimate Guitar."
            } else {
                "Searching Ultimate Guitar chord results."
            },
        )
    }
    var isSearching by remember(initialQuery) { mutableStateOf(initialQuery.isNotBlank()) }

    fun runSearch(targetQuery: String) {
        val trimmedQuery = targetQuery.trim()
        if (trimmedQuery.isBlank()) {
            statusMessage = "Type a song title or artist to search."
            results = emptyList()
            isSearching = false
            return
        }

        scope.launch {
            isSearching = true
            results = emptyList()
            statusMessage = "Searching Ultimate Guitar chord results."
            onSearch(trimmedQuery)
                .onSuccess { foundResults ->
                    results = foundResults
                    statusMessage = if (foundResults.isEmpty()) {
                        "No chord results were found for that search."
                    } else {
                        "Select the Ultimate Guitar result you want to import."
                    }
                }
                .onFailure { throwable ->
                    results = emptyList()
                    statusMessage = throwable.message ?: "Ultimate Guitar search failed."
                }
            isSearching = false
        }
    }

    LaunchedEffect(initialQuery) {
        if (initialQuery.isNotBlank()) {
            runSearch(initialQuery)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.86f),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Search Ultimate Guitar",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = "Only chord results are shown. Select one to import it into this song.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Search query") },
                        placeholder = { Text("Song title artist") },
                        singleLine = true,
                    )
                    Button(
                        onClick = { runSearch(query) },
                        enabled = query.isNotBlank() && !isSearching,
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Search")
                    }
                }
                if (isSearching) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Box(
                    modifier = Modifier.weight(1f),
                ) {
                    if (results.isEmpty()) {
                        Text(
                            text = if (isSearching) "" else "No chord results to show yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(
                                items = results,
                                key = { result ->
                                    if (result.tabId > 0) {
                                        result.tabId
                                    } else {
                                        result.url
                                    }
                                },
                            ) { result ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onResultSelected(result) },
                                    shape = MaterialTheme.shapes.large,
                                    tonalElevation = 2.dp,
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Text(
                                            text = result.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                        val duplicateCount = duplicateKeys[
                                            result.title.trim().lowercase() to result.artist.trim().lowercase()
                                        ] ?: 0
                                        val versionLabel = result.version
                                            ?.takeIf { duplicateCount > 1 }
                                            ?.let { "Ver $it" }
                                        val subtitle = listOfNotNull(
                                            result.artist.takeIf { it.isNotBlank() },
                                            versionLabel,
                                            result.typeLabel.takeIf { it.isNotBlank() },
                                        ).joinToString(" | ")
                                        if (subtitle.isNotBlank()) {
                                            Text(
                                                text = subtitle,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }
                }
            }
        }
    }
}
