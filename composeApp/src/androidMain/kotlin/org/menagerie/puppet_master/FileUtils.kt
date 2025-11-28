package org.menagerie.puppet_master

import android.content.Context
import java.io.File

actual fun getUploadsDir(context: Any): String {
    val androidContext = context as Context
    val uploadsDir = File(androidContext.filesDir, "uploads")
    if (!uploadsDir.exists()) {
        uploadsDir.mkdirs()
    }
    return uploadsDir.absolutePath
}
