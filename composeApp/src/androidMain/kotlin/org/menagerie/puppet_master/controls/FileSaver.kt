package org.menagerie.puppet_master.controls

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable

/**
 * Android implementation for saving files.
 *
 * This actual fun uses the [ActivityResultContracts.StartActivityForResult] to launch a system file picker
 * for creating a document.
 *
 * @param show Controls the visibility of the file saver. If true, the file saver is shown.
 * @param defaultFileName The default name for the file to be saved.
 * @param fileExtensions A list of file extensions that are allowed to be saved.
 * @param onFileSaved A callback function that is invoked when a file is saved.
 * It returns the Uri of the saved file as a string, or null if the operation was canceled.
 */
@Composable
actual fun FileSaver(
    show: Boolean,
    defaultFileName: String,
    fileExtensions: List<String>,
    onFileSaved: (String?) -> Unit
) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            onFileSaved(result.data?.data?.toString())
        } else {
            onFileSaved(null)
        }
    }

    if (show) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_TITLE, defaultFileName)
        }
        launcher.launch(intent)
    }
}