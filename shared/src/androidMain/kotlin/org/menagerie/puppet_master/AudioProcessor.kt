package org.menagerie.puppet_master

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.Visualizer
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.math.sqrt

/**
 * Processes audio from the microphone to detect volume levels and frequency data.
 * This is the Android implementation.
 *
 * @param context The Android [Context] used to check for audio permissions.
 */
actual class AudioProcessor actual constructor(private val context: Any) {

    private val audioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var audioJob: Job? = null
    private var smoothedLevel: Float = 0f
    private var visualizer: Visualizer? = null

    /**
     * Starts listening to the microphone and reporting audio levels and frequency data.
     * The audio processing is done in a background coroutine.
     *
     * If the `RECORD_AUDIO` permission is not granted, this function will not start listening.
     *
     * @param onLevelChange A callback that receives the audio level as a Float between 0.0 and 1.0.
     * @param onFrequencyData An optional callback that receives the frequency spectrum as a FloatArray.
     */
    @SuppressLint("MissingPermission")
    actual fun start(
        onLevelChange: (Float) -> Unit,
        onFrequencyData: ((FloatArray) -> Unit)?,
    ) {
        audioJob?.cancel()

        val androidContext = context as Context
        if (ContextCompat.checkSelfPermission(androidContext, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            println("RECORD_AUDIO permission not granted.")
            return
        }

        audioJob = audioScope.launch {
            var audioRecord: AudioRecord? = null
            try {
                val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)

                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    minBufferSize
                )

                if (onFrequencyData != null) {
                    try {
                        visualizer = Visualizer(audioRecord.audioSessionId).apply {
                            captureSize = Visualizer.getCaptureSizeRange()[1]
                            setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                                override fun onWaveFormDataCapture(visualizer: Visualizer?, waveform: ByteArray?, samplingRate: Int) {}

                                override fun onFftDataCapture(visualizer: Visualizer?, fft: ByteArray?, samplingRate: Int) {
                                    if (fft != null) {
                                        onFrequencyData(processFftData(fft))
                                    }
                                }
                            }, Visualizer.getMaxCaptureRate() / 2, false, true)
                            enabled = true
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        visualizer = null
                    }
                }

                audioRecord.startRecording()

                val buffer = ShortArray(minBufferSize)

                while (isActive) {
                    val readSize = audioRecord.read(buffer, 0, buffer.size)
                    if (readSize > 0) {
                        val level = calculateAudioLevel(buffer, readSize)
                        onLevelChange(level)
                    }
                }
            } catch (e: Exception) {
                onError("Error initializing audio: ${e.message}")
                e.printStackTrace()
            } finally {
                audioRecord?.stop()
                audioRecord?.release()
                visualizer?.release()
                visualizer = null
            }
        }
    }

    /**
     * Stops listening to the microphone and releases audio resources.
     */
    actual fun stop() {
        audioJob?.cancel()
        // The coroutine's finally block will handle resource cleanup.
    }

    private fun processFftData(fft: ByteArray): FloatArray {
        val magnitudes = FloatArray(fft.size / 2)
        for (i in 0 until fft.size / 2) {
            val real = fft[i * 2].toFloat()
            val imag = fft[i * 2 + 1].toFloat()
            magnitudes[i] = hypot(real, imag)
        }
        return magnitudes
    }

    /**
     * Calculates the audio level from a short array of audio data.
     * The level is the normalized Root Mean Square (RMS) of the audio samples.
     *
     * @param audioData The short array containing raw audio data.
     * @param readSize The number of samples read into the buffer.
     * @return The audio level, a float value between 0.0 and 1.0.
     */
    private fun calculateAudioLevel(audioData: ShortArray, readSize: Int): Float {
        if (readSize == 0) return 0f

        val sumOfSquares = (0 until readSize).sumOf { i ->
            val sample = audioData[i].toDouble()
            sample * sample
        }

        val rms = sqrt(sumOfSquares / readSize)
        val normalizedRms = (rms / MAX_AMPLITUDE).toFloat()

        // Apply smoothing
        smoothedLevel += (normalizedRms - smoothedLevel) * SMOOTHING_FACTOR

        return smoothedLevel
    }

    /**
     * Cancels the audio processing coroutine scope. This should be called when the AudioProcessor is no longer needed.
     */
    fun release() {
        audioScope.cancel()
    }

    companion object {
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val MAX_AMPLITUDE = 32767.0 // Max value for 16-bit signed audio

        private const val SMOOTHING_FACTOR = 0.1f // Increase for faster response, decrease for more smoothing

        const val SOURCE_MIC = "MIC"
        const val SOURCE_MEDIA = "MEDIA"
        const val SOURCE_GAME = "GAME"

        fun getAvailableInputs(): List<String> {
            val inputs = mutableListOf(SOURCE_MIC)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                inputs.add(SOURCE_MEDIA)
                inputs.add(SOURCE_GAME)
            }
            return inputs
        }

    }
}
