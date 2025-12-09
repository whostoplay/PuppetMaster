package org.menagerie.puppet_master

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * An Android-specific implementation of the [SettingsRepository] that uses [SharedPreferences] for storage.
 */
actual class SettingsRepository actual constructor(context: Any) {
    private val prefs: SharedPreferences = (context as Context).getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    private val json = Json

    /**
     * Saves the settings to [SharedPreferences].
     */
    actual fun saveSettings(settings: SettingsModel) {
        val settingsJson = json.encodeToString(settings)
        prefs.edit { putString("settings", settingsJson) }
    }

    /**
     * Loads the settings from [SharedPreferences].
     */
    actual fun loadSettings(): SettingsModel {
        val settingsJson = prefs.getString("settings", null)
        return if (settingsJson != null) {
            json.decodeFromString(settingsJson)
        } else {
            SettingsModel()
        }
    }
}
