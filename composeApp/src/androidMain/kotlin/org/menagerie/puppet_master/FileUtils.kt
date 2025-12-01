package org.menagerie.puppet_master

import android.content.Context
import java.io.File

/**
 * Returns the directory for storing uploaded files.
 * On Android, this is a subdirectory of the app's internal storage.
 *
 * @param context The Android context.
 * @return The absolute path to the uploads directory.
 */
actual fun getUploadsDir(context: Any): String {
    val androidContext = context as Context
    val uploadsDir = File(androidContext.filesDir, "uploads")
    if (!uploadsDir.exists()) {
        uploadsDir.mkdirs()
    }
    return uploadsDir.absolutePath
}
