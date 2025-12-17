package org.menagerie.puppet_master

import java.io.File

/**
 * Returns the directory for storing uploaded files.
 * On JVM, this is a subdirectory of the current working directory.
 *
 * @param context The JVM context (unused).
 * @return The absolute path to the uploads directory.
 */
actual fun getUploadsDir(context: Any): String {
    val uploadsDir = File(System.getProperty("user.dir"), "uploads")
    if (!uploadsDir.exists()) {
        uploadsDir.mkdirs()
    }
    return uploadsDir.absolutePath
}
