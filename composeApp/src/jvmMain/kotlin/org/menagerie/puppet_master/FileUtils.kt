package org.menagerie.puppet_master

import java.io.File

actual fun getUploadsDir(context: Any): String {
    val uploadsDir = File(System.getProperty("user.dir"), "uploads")
    if (!uploadsDir.exists()) {
        uploadsDir.mkdirs()
    }
    return uploadsDir.absolutePath
}
