package com.codex.stageset.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.codex.stageset.data.repository.PreviewSettings

fun buildPreviewTitle(
    preset: String,
    keySignature: String,
    name: String,
): AnnotatedString {
    val resolvedName = name.ifBlank { "Song Preview" }
    val leadingParts = listOfNotNull(
        preset.takeIf { it.isNotBlank() },
        keySignature.takeIf { it.isNotBlank() },
    )

    if (leadingParts.isEmpty()) {
        return AnnotatedString(resolvedName)
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
        append(resolvedName)
    }
}

@Composable
fun PreviewSettingsDialog(
    settings: PreviewSettings,
    onDismiss: () -> Unit,
    onShowLyricsChange: (Boolean) -> Unit,
    onShowLyricsCueChange: (Boolean) -> Unit,
    onShowChordsChange: (Boolean) -> Unit,
    onShowNotationChange: (Boolean) -> Unit,
    onHideRepeatedSectionChordsChange: (Boolean) -> Unit,
    onCompressChordsChange: (Boolean) -> Unit,
    onColorizeSectionHeadingsChange: (Boolean) -> Unit,
    onTwoColumnsChange: (Boolean) -> Unit,
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
                    title = "Show lyrics cue",
                    description = "Show the first four lyric words from each section as a short reminder line followed by ellipsis.",
                    checked = settings.showLyricsCue,
                    onCheckedChange = onShowLyricsCueChange,
                )
                PreviewSettingsRow(
                    title = "Show chords",
                    description = "Turn this off to hide chord lines while keeping section titles, lyrics, and notation that are still enabled.",
                    checked = settings.showChords,
                    onCheckedChange = onShowChordsChange,
                )
                PreviewSettingsRow(
                    title = "Show notation",
                    description = "Turn this off to hide staff notation blocks written with @ ... @ in the chart.",
                    checked = settings.showNotation,
                    onCheckedChange = onShowNotationChange,
                )
                PreviewSettingsRow(
                    title = "Hide repeated section chords",
                    description = "If later verses or choruses reuse the same chord pattern, only the first matching section keeps its chord lines.",
                    checked = settings.hideRepeatedSectionChords,
                    onCheckedChange = onHideRepeatedSectionChordsChange,
                )
                PreviewSettingsRow(
                    title = "Compress",
                    description = "In chord-only preview, condense repeating chord runs into compact symbolic lines.",
                    checked = settings.compressChords,
                    onCheckedChange = onCompressChordsChange,
                )
                PreviewSettingsRow(
                    title = "Two columns",
                    description = "Split the chart into two reading columns and tighten chord spacing so more of the song fits on screen.",
                    checked = settings.twoColumns,
                    onCheckedChange = onTwoColumnsChange,
                )
                PreviewSettingsRow(
                    title = "Color similar sections",
                    description = "Give matching section families like Verse, Chorus, and Bridge their own shared heading color.",
                    checked = settings.colorizeSectionHeadings,
                    onCheckedChange = onColorizeSectionHeadingsChange,
                )
            }
        },
    )
}

@Composable
fun ConfirmActionDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = {
            Text(title)
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
    var showHelp by remember(title) { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Box {
                IconButton(
                    onClick = { showHelp = true },
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "About $title",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                DropdownMenu(
                    expanded = showHelp,
                    onDismissRequest = { showHelp = false },
                ) {
                    Text(
                        text = description,
                        modifier = Modifier
                            .widthIn(max = 280.dp)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}
