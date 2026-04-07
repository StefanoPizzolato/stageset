package com.codex.stageset.data.repository

import android.content.Context

class UltimateGuitarConsentRepository(context: Context) {
    private val preferences = context.getSharedPreferences("ultimate-guitar-consent", Context.MODE_PRIVATE)

    fun hasAcceptedSearchDisclaimer(): Boolean {
        return preferences.getBoolean(KeySearchDisclaimerAccepted, false)
    }

    fun acceptSearchDisclaimer() {
        preferences.edit()
            .putBoolean(KeySearchDisclaimerAccepted, true)
            .apply()
    }

    private companion object {
        const val KeySearchDisclaimerAccepted = "search_disclaimer_accepted"
    }
}
