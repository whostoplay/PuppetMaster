package org.menagerie.puppet_master

/**
 * Defines a common API for saving and loading application settings in a multiplatform context.
 *
 * This `expect` class declares the public interface for a repository that handles the persistence of a [SettingsModel].
 * Platform-specific modules (`androidMain`, `jvmMain`, etc.) must provide `actual` implementations that handle
 * the concrete storage mechanism (e.g., SharedPreferences on Android, a properties file on JVM).
 *
 * @param context A generic placeholder for a platform-specific object that may be required for initialization.
 *                For example, on Android, this would be an instance of `android.content.Context`.
 */
expect class SettingsRepository(context: Any) {
    /**
     * Persists the given [SettingsModel] to the platform's storage.
     *
     * @param settings The settings object to save.
     */
    fun saveSettings(settings: SettingsModel)

    /**
     * Retrieves the persisted [SettingsModel] from the platform's storage.
     *
     * If no settings are found, an implementation should return a default [SettingsModel].
     *
     * @return The loaded [SettingsModel].
     */
    fun loadSettings(): SettingsModel
}
