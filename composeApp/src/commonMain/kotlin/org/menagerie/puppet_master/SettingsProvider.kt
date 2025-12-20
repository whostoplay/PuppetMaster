package org.menagerie.puppet_master


/**
 * A singleton object to provide global access to the SettingsRepository instance.
 *
 * This uses a 'late-initialization' pattern. The instance must be set
 * by the platform-specific application entry point (e.g., in PuppetDataManager or MainActivity).
 *
 * The @ThreadLocal annotation is used for Kotlin/Native to ensure compatibility,
 * although for Android/JVM it's not strictly necessary.
 */
object SettingsProvider {
    private var instance: SettingsRepository? = null

    /**
     * Initializes the provider with a platform-specific instance of SettingsRepository.
     * This should only be called once at application startup.
     */
    fun initialize(repository: SettingsRepository) {
        if (instance == null) {
            instance = repository
        }
    }

    /**
     * Provides the global instance of the SettingsRepository.
     * Throws an exception if `initialize` has not been called first.
     */
    fun get(): SettingsRepository {
        return instance ?: throw IllegalStateException("SettingsProvider has not been initialized. Call initialize() first.")
    }
}
