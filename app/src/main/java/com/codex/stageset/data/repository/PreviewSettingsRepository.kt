package com.codex.stageset.data.repository

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PreviewSettings(
    val showLyrics: Boolean = true,
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
        updateSettings { it.copy(showLyrics = showLyrics) }
    }

    fun setHideRepeatedSectionChords(hideRepeatedSectionChords: Boolean) {
        updateSettings { it.copy(hideRepeatedSectionChords = hideRepeatedSectionChords) }
    }

    fun setCompressChords(compressChords: Boolean) {
        updateSettings { it.copy(compressChords = compressChords) }
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
            hideRepeatedSectionChords = preferences.getBoolean(KeyHideRepeatedSectionChords, false),
            compressChords = preferences.getBoolean(KeyCompressChords, false),
            colorizeSectionHeadings = preferences.getBoolean(KeyColorizeSectionHeadings, false),
            twoColumns = preferences.getBoolean(KeyTwoColumns, false),
        )
    }

    private companion object {
        const val KeyShowLyrics = "show_lyrics"
        const val KeyHideRepeatedSectionChords = "hide_repeated_section_chords"
        const val KeyCompressChords = "compress_chords"
        const val KeyColorizeSectionHeadings = "colorize_section_headings"
        const val KeyTwoColumns = "two_columns"
    }
}
