package com.codex.stageset.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
