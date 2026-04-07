package com.codex.stageset.data.repository

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PreviewSettings(
    val showLyrics: Boolean = true,
    val showLyricsCue: Boolean = false,
    val showChords: Boolean = true,
    val showNotation: Boolean = true,
    val hideRepeatedSectionChords: Boolean = false,
    val compressChords: Boolean = false,
    val colorizeSectionHeadings: Boolean = false,
    val twoColumns: Boolean = false,
)

class PreviewSettingsRepository(context: Context) {
    private val preferences = context.getSharedPreferences("preview-settings", Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(readSettings())

    val settings: StateFlow<PreviewSettings> = _settings.asStateFlow()

    fun setShowLyrics(showLyrics: Boolean) {
        updateSettings {
            it.copy(
                showLyrics = showLyrics,
                compressChords = if (showLyrics) false else it.compressChords,
            )
        }
    }

    fun setShowLyricsCue(showLyricsCue: Boolean) {
        updateSettings { it.copy(showLyricsCue = showLyricsCue) }
    }

    fun setShowChords(showChords: Boolean) {
        updateSettings { it.copy(showChords = showChords) }
    }

    fun setShowNotation(showNotation: Boolean) {
        updateSettings { it.copy(showNotation = showNotation) }
    }

    fun setHideRepeatedSectionChords(hideRepeatedSectionChords: Boolean) {
        updateSettings { it.copy(hideRepeatedSectionChords = hideRepeatedSectionChords) }
    }

    fun setCompressChords(compressChords: Boolean) {
        updateSettings {
            it.copy(
                compressChords = compressChords,
                showLyrics = if (compressChords) false else it.showLyrics,
            )
        }
    }

    fun setColorizeSectionHeadings(colorizeSectionHeadings: Boolean) {
        updateSettings { it.copy(colorizeSectionHeadings = colorizeSectionHeadings) }
    }

    fun setTwoColumns(twoColumns: Boolean) {
        updateSettings { it.copy(twoColumns = twoColumns) }
    }

    private fun updateSettings(transform: (PreviewSettings) -> PreviewSettings) {
        val updated = transform(_settings.value)
        preferences.edit()
            .putBoolean(KeyShowLyrics, updated.showLyrics)
            .putBoolean(KeyShowLyricsCue, updated.showLyricsCue)
            .putBoolean(KeyShowChords, updated.showChords)
            .putBoolean(KeyShowNotation, updated.showNotation)
            .putBoolean(KeyHideRepeatedSectionChords, updated.hideRepeatedSectionChords)
            .putBoolean(KeyCompressChords, updated.compressChords)
            .putBoolean(KeyColorizeSectionHeadings, updated.colorizeSectionHeadings)
            .putBoolean(KeyTwoColumns, updated.twoColumns)
            .apply()
        _settings.value = updated
    }

    private fun readSettings(): PreviewSettings {
        return PreviewSettings(
            showLyrics = preferences.getBoolean(KeyShowLyrics, true),
            showLyricsCue = preferences.getBoolean(KeyShowLyricsCue, false),
            showChords = preferences.getBoolean(KeyShowChords, true),
            showNotation = preferences.getBoolean(KeyShowNotation, true),
            hideRepeatedSectionChords = preferences.getBoolean(KeyHideRepeatedSectionChords, false),
            compressChords = preferences.getBoolean(KeyCompressChords, false),
            colorizeSectionHeadings = preferences.getBoolean(KeyColorizeSectionHeadings, false),
            twoColumns = preferences.getBoolean(KeyTwoColumns, false),
        )
    }

    private companion object {
        const val KeyShowLyrics = "show_lyrics"
        const val KeyShowLyricsCue = "show_lyrics_cue"
        const val KeyShowChords = "show_chords"
        const val KeyShowNotation = "show_notation"
        const val KeyHideRepeatedSectionChords = "hide_repeated_section_chords"
        const val KeyCompressChords = "compress_chords"
        const val KeyColorizeSectionHeadings = "colorize_section_headings"
        const val KeyTwoColumns = "two_columns"
    }
}
