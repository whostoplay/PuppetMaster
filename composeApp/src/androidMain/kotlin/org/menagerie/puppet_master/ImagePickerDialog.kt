package org.menagerie.puppet_master

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Android implementation of an image picker dialog.
 * This function launches the system's file picker to select one or more PNG images.
 *
 * @param show Whether to show the dialog.
 * @param title The title of the dialog. This is not used on Android.
 * @param multiSelect Whether to allow multiple image selection.
 * @param initialDirectory The initial directory to open. This is not used on Android.
 * @param onCancel Called when the dialog is cancelled.
 * @param onResult Called with the selected image(s) as a list of pairs of [ByteArray] and file name.
 * @param onFolderSelected Called when a folder is selected. This is not used on Android.
 */
@Composable
actual fun ImagePickerDialog(
    show: Boolean,
    title: String,
    multiSelect: Boolean,
    initialDirectory: String?,
    onCancel: () -> Unit,
    onResult: (List<Pair<ByteArray, String>>) -> Unit,
    onFolderSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uris = result.data?.clipData?.let { clipData ->
                (0 until clipData.itemCount).map { clipData.getItemAt(it).uri }
            } ?: listOfNotNull(result.data?.data)

            val images = uris.mapNotNull { uri ->
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val bytes = inputStream.readBytes()
                    val fileName = uri.lastPathSegment ?: "image.png"
                    bytes to fileName
                }
            }
            onResult(images)
        } else {
            onCancel()
        }
    }

    if (show) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/png"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, multiSelect)
        }
        launcher.launch(intent)
    }
}
