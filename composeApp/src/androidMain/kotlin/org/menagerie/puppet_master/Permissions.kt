package org.menagerie.puppet_master

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

/**
 * Checks if the app has audio recording permission.
 * @param context The Android context.
 * @return True if the permission is granted, false otherwise.
 */
actual fun hasAudioPermission(context: Any): Boolean {
    return (context as Context).checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
}

/**
 * A composable that requests audio recording permission.
 * @param onResult Callback that is invoked with the result of the permission request.
 */
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
