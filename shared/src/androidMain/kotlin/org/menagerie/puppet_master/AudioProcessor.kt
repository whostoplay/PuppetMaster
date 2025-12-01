package org.menagerie.puppet_master

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

actual class AudioProcessor actual constructor(private val context: Any) {

    private var audioJob: Job? = null

    @SuppressLint("MissingPermission")
    actual fun start(onLevelChange: (Float) -> Unit) {
        val androidContext = context as Context
        if (ActivityCompat.checkSelfPermission(androidContext, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            println("RECORD_AUDIO permission not granted.")
            return
        }

        audioJob = GlobalScope.launch {
            try {
                val sampleRate = 16000
                val channelConfig = AudioFormat.CHANNEL_IN_MONO
                val audioFormat = AudioFormat.ENCODING_PCM_16BIT
                val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

                val audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat, minBufferSize)
                audioRecord.startRecording()

                val buffer = ShortArray(minBufferSize)

                while (true) {
                    val readSize = audioRecord.read(buffer, 0, buffer.size)
                    if (readSize > 0) {
                        val level = calculateAudioLevel(buffer, readSize)
                        onLevelChange(level)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    actual fun stop() {
        audioJob?.cancel()
    }
}