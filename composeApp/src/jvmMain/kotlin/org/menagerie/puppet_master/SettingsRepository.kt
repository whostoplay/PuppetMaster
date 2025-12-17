package org.menagerie.puppet_master

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.Properties

/**
 * A JVM-specific implementation of the [SettingsRepository] that uses a properties file for storage.
 */
actual class SettingsRepository actual constructor(context: Any) {
    private val propertiesFile = File("settings.properties")
    private val json = Json

    /**
     * Saves the settings to the properties file.
     */
    actual fun saveSettings(settings: SettingsModel) {
        val properties = Properties()
        if (propertiesFile.exists()) {
            properties.load(propertiesFile.reader())
        }
        val settingsJson = json.encodeToString(settings)
        properties.setProperty("settings", settingsJson)
        properties.store(propertiesFile.writer(), null)
    }

    /**
     * Loads the settings from the properties file.
     */
    actual fun loadSettings(): SettingsModel {
        val properties = Properties()
        if (propertiesFile.exists()) {
            properties.load(propertiesFile.reader())
        }
        val settingsJson = properties.getProperty("settings")
        val settings = if (settingsJson != null) {
            json.decodeFromString(settingsJson)
        } else {
            SettingsModel()
        }
        return if (settings.serverIpAddress.isBlank()) {
            settings.copy(serverIpAddress = "127.0.0.1")
        } else {
            settings
        }
    }
}
