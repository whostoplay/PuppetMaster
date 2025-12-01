package org.menagerie.puppet_master

import java.io.File
import java.util.Properties

/**
 * A JVM-specific implementation of the [SettingsRepository] that uses a properties file for storage.
 */
actual class SettingsRepository actual constructor(context: Any) {
    private val propertiesFile = File("settings.properties")

    /**
     * Saves the server IP address to the properties file.
     */
    actual fun saveIp(ip: String) {
        val properties = Properties()
        if (propertiesFile.exists()) {
            properties.load(propertiesFile.reader())
        }
        properties.setProperty("server_ip", ip)
        properties.store(propertiesFile.writer(), null)
    }

    /**
     * Loads the server IP address from the properties file.
     */
    actual fun loadIp(): String {
        val properties = Properties()
        if (propertiesFile.exists()) {
            properties.load(propertiesFile.reader())
        }
        return properties.getProperty("server_ip", DEFAULT_SERVER_HOST)
    }
}
