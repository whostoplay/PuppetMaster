package org.menagerie.puppet_master

import android.content.Context
import android.content.SharedPreferences

/**
 * An Android-specific implementation of the [SettingsRepository] that uses [SharedPreferences] for storage.
 */
actual class SettingsRepository actual constructor(context: Any) {
    private val prefs: SharedPreferences = (context as Context).getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    /**
     * Saves the server IP address to [SharedPreferences].
     */
    actual fun saveIp(ip: String) {
        prefs.edit().putString("server_ip", ip).apply()
    }

    /**
     * Loads the server IP address from [SharedPreferences].
     */
    actual fun loadIp(): String {
        return prefs.getString("server_ip", DEFAULT_SERVER_HOST) ?: DEFAULT_SERVER_HOST
    }
}
