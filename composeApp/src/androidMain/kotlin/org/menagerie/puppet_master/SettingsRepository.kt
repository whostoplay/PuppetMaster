package org.menagerie.puppet_master

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Android-specific implementation of [SettingsRepository] that stores settings using [SharedPreferences].
 *
 * This class serializes the [SettingsModel] to a JSON string and saves it under a single key
 * in a private SharedPreferences file named "app_settings".
 *
 * @param context The Android [Context] required to access SharedPreferences.
 */
actual class SettingsRepository actual constructor(context: Any) {
    private val prefs: SharedPreferences = (context as Context).getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    private val json = Json

    /**
     * Saves the provided [SettingsModel] to SharedPreferences.
     *
     * The settings are serialized to a JSON string and stored under the "settings" key.
     * The `apply()` method is used for asynchronous saving.
     *
     * @param settings The [SettingsModel] to save.
     */
    actual fun saveSettings(settings: SettingsModel) {
        val settingsJson = json.encodeToString(settings)
        prefs.edit {
            putString("settings", settingsJson)
        }
    }

    /**
     * Loads the [SettingsModel] from SharedPreferences.
     *
     * If no settings are found, a default [SettingsModel] is returned.
     * If the loaded IP address is blank, it defaults to "127.0.0.1".
     * This function also handles potential JSON deserialization errors gracefully.
     *
     * @return The loaded [SettingsModel], or a default instance if not found or on error.
     */
    actual fun loadSettings(): SettingsModel {
        val settingsJson = prefs.getString("settings", null)
        val settings = settingsJson?.let {
            try {
                json.decodeFromString<SettingsModel>(it)
            } catch (e: Exception) {
                // Handle potential deserialization errors if the stored format is invalid.
                e.printStackTrace()
                SettingsModel()
            }
        } ?: SettingsModel()

        // Ensure a default IP is present if the loaded one is blank.
        return if (settings.serverIpAddress.isBlank()) {
            settings.copy(serverIpAddress = "127.0.0.1")
        } else {
            settings
        }
    }
}
