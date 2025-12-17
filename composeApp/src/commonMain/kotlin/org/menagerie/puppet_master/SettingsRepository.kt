package org.menagerie.puppet_master

/**
 * A repository for saving and loading application settings.
 * This is a platform-specific class.
 */
expect class SettingsRepository(context: Any) {
    /**
     * Saves the settings.
     */
    fun saveSettings(settings: SettingsModel)

    /**
     * Loads the settings.
     */
    fun loadSettings(): SettingsModel
}
