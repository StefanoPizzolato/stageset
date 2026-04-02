package com.codex.stageset.data.repository

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PreviewSettings(
    val showLyrics: Boolean = true,
    val hideRepeatedSectionChords: Boolean = false,
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

    private fun updateSettings(transform: (PreviewSettings) -> PreviewSettings) {
        val updated = transform(_settings.value)
        preferences.edit()
            .putBoolean(KeyShowLyrics, updated.showLyrics)
            .putBoolean(KeyHideRepeatedSectionChords, updated.hideRepeatedSectionChords)
            .apply()
        _settings.value = updated
    }

    private fun readSettings(): PreviewSettings {
        return PreviewSettings(
            showLyrics = preferences.getBoolean(KeyShowLyrics, true),
            hideRepeatedSectionChords = preferences.getBoolean(KeyHideRepeatedSectionChords, false),
        )
    }

    private companion object {
        const val KeyShowLyrics = "show_lyrics"
        const val KeyHideRepeatedSectionChords = "hide_repeated_section_chords"
    }
}
