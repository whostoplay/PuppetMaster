package org.menagerie.puppet_master.controls

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import org.menagerie.puppet_master.getContext

/**
 * Android implementation for picking files.
 *
 * This actual fun uses the [ActivityResultContracts.StartActivityForResult] to launch a system file picker
 * for opening a document.
 *
 * @param show Controls the visibility of the file picker. If true, the file picker is shown.
 * @param fileExtensions A list of file extensions to filter by.
 * @param onFileSelected A callback function that is invoked when a file is selected.
 * It returns the Uri of the selected file as a string, or null if the operation was canceled.
 */
@Composable
actual fun FilePicker(
    show: Boolean,
    fileExtensions: List<String>,
    onFileSelected: (String?) -> Unit
) {
    val context = getContext() as Context
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            onFileSelected(result.data?.data?.toString())
        } else {
            onFileSelected(null)
        }
    }

    if (show) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, fileExtensions.map { "application/$it" }.toTypedArray())
        }
        launcher.launch(intent)
    }
}