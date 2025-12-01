package org.menagerie.puppet_master

/**
 * A repository for saving and loading application settings.
 * This is a platform-specific class.
 */
expect class SettingsRepository(context: Any) {
    /**
     * Saves the server IP address.
     */
    fun saveIp(ip: String)

    /**
     * Loads the server IP address.
     */
    fun loadIp(): String
}
