package org.menagerie.puppet_master.controls

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable

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