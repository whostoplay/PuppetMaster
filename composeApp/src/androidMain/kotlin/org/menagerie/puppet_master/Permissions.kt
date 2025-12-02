package org.menagerie.puppet_master

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

actual fun hasAudioPermission(context: Any): Boolean {
    return (context as Context).checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
}

@Composable
actual fun RequestAudioPermission(onResult: (granted: Boolean) -> Unit) {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        onResult(isGranted)
    }

    LaunchedEffect(Unit) {
        launcher.launch(Manifest.permission.RECORD_AUDIO)
    }
}
