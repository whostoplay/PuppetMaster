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
import kotlin.math.sqrt

actual class AudioProcessor actual constructor(private val context: Any) {

    private var audioJob: Job? = null

    @SuppressLint("MissingPermission")
    actual fun start(onVoiceActivity: (Boolean) -> Unit) {
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
                var isSpeaking = false

                while (true) {
                    val readSize = audioRecord.read(buffer, 0, buffer.size)
                    if (readSize > 0) {
                        var sum = 0.0
                        for (i in 0 until readSize) {
                            sum += buffer[i] * buffer[i]
                        }
                        val rms = sqrt(sum / readSize)
                        
                        val currentlySpeaking = rms > 300 // Basic threshold

                        if (currentlySpeaking != isSpeaking) {
                            isSpeaking = currentlySpeaking
                            onVoiceActivity(isSpeaking)
                        }
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