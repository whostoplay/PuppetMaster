package org.menagerie.puppet_master

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

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
