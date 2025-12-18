package org.menagerie.puppet_master

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.Properties

/**
 * JVM-specific implementation of [SettingsRepository] that stores settings in a `settings.properties` file
 * in the application's working directory.
 *
 * This class uses Java's [Properties] class to read and write a single "settings" key, where the value is a
 * JSON serialized string of the [SettingsModel].
 *
 * @param context This parameter is not used in the JVM implementation but is required by the `expect` declaration.
 */
actual class SettingsRepository actual constructor(context: Any) {
    private val propertiesFile = File("settings.properties")
    private val json = Json

    /**
     * Saves the provided [SettingsModel] to the `settings.properties` file.
     *
     * The settings are serialized to a JSON string and stored under the "settings" key.
     * If the file already exists, its contents will be updated.
     *
     * @param settings The [SettingsModel] to save.
     */
    actual fun saveSettings(settings: SettingsModel) {
        val properties = Properties()
        if (propertiesFile.exists()) {
            propertiesFile.reader().use { properties.load(it) }
        }
        val settingsJson = json.encodeToString(settings)
        properties.setProperty("settings", settingsJson)
        propertiesFile.writer().use { properties.store(it, "Puppet Master Settings") }
    }

    /**
     * Loads the [SettingsModel] from the `settings.properties` file.
     *
     * If the file does not exist, or if the "settings" key is not present, a default [SettingsModel] is returned.
     * If the loaded IP address is blank, it defaults to "127.0.0.1".
     *
     * @return The loaded [SettingsModel], or a default instance if not found or on error.
     */
    actual fun loadSettings(): SettingsModel {
        if (!propertiesFile.exists()) return SettingsModel(serverIpAddress = "127.0.0.1")

        val properties = Properties().apply {
            runCatching {
                propertiesFile.reader().use { load(it) }
            }
        }

        val settingsJson = properties.getProperty("settings")
        val settings = settingsJson?.let {
            try {
                json.decodeFromString<SettingsModel>(it)
            } catch (e: Exception) {
                // Handle potential deserialization errors, e.g., if the file is corrupt or format changed.
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
