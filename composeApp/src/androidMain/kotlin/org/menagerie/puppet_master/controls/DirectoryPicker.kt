package org.menagerie.puppet_master.controls

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable

@Composable
actual fun DirectoryPicker(
    show: Boolean,
    onDirectorySelected: (String?) -> Unit
) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            onDirectorySelected(result.data?.data?.toString())
        } else {
            onDirectorySelected(null)
        }
    }

    if (show) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        launcher.launch(intent)
    }
}