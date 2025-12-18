package org.menagerie.puppet_master

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.TargetDataLine
import kotlin.math.sqrt

/**
 * Processes audio from the microphone to detect volume levels.
 * This is the JVM implementation.
 *
 * @param context The context is not used on JVM but is required by the expect class.
 */
actual class AudioProcessor actual constructor(context: Any) {

    private val audioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var audioJob: Job? = null

    /**
     * Starts listening to the microphone and reporting audio levels.
     * The audio processing is done in a background coroutine.
     *
     * @param onLevelChange A callback that receives the audio level as a Float between 0.0 and 1.0.
     */
    actual fun start(onLevelChange: (Float) -> Unit) {
        audioJob?.cancel()
        audioJob = audioScope.launch {
            var dataLine: TargetDataLine? = null
            try {
                val format = AudioFormat(SAMPLE_RATE, SAMPLE_SIZE_IN_BITS, CHANNELS, IS_SIGNED, IS_BIG_ENDIAN)
                val info = DataLine.Info(TargetDataLine::class.java, format)
                dataLine = AudioSystem.getLine(info) as TargetDataLine
                dataLine.open(format)
                dataLine.start()

                val buffer = ByteArray(BUFFER_SIZE)

                while (isActive) {
                    val bytesRead = dataLine.read(buffer, 0, buffer.size)
                    if (bytesRead > 0) {
                        val level = calculateAudioLevel(buffer, bytesRead)
                        onLevelChange(level)
                    }
                }
            } catch (e: Exception) {
                // TODO: Implement a proper error handling strategy, e.g., using a callback.
                e.printStackTrace()
            } finally {
                dataLine?.stop()
                dataLine?.close()
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

    /**
     * Calculates the audio level from a byte array of audio data.
     * The level is the normalized Root Mean Square (RMS) of the audio samples.
     * This implementation is optimized to avoid intermediate array allocation.
     *
     * @param audioData The byte array containing raw audio data.
     * @param bytesRead The number of bytes read into the buffer.
     * @return The audio level, a float value between 0.0 and 1.0.
     */
    private fun calculateAudioLevel(audioData: ByteArray, bytesRead: Int): Float {
        val numSamples = bytesRead / 2
        if (numSamples == 0) return 0f

        val sumOfSquares = (0 until numSamples).sumOf { i ->
            val byteIndex = i * 2
            // Little-endian conversion from 2 bytes to a short
            val sample = ((audioData[byteIndex + 1].toInt() shl 8) or (audioData[byteIndex].toInt() and 0xFF)).toShort()
            val sampleAsDouble = sample.toDouble()
            sampleAsDouble * sampleAsDouble
        }

        val rms = sqrt(sumOfSquares / numSamples)
        val normalizedRms = (rms / MAX_AMPLITUDE).toFloat()

        return normalizedRms.coerceIn(0f, 1f)
    }

    /**
     * Cancels the audio processing coroutine scope. This should be called when the AudioProcessor is no longer needed.
     */
    fun release() {
        audioScope.cancel()
    }

    companion object {
        private const val SAMPLE_RATE = 16000f
        private const val SAMPLE_SIZE_IN_BITS = 16
        private const val CHANNELS = 1
        private const val IS_SIGNED = true
        private const val IS_BIG_ENDIAN = false // For WAV format, data is usually little-endian
        private const val BUFFER_SIZE = 2048
        private const val MAX_AMPLITUDE = 32767.0 // Max value for 16-bit signed audio
    }
}
