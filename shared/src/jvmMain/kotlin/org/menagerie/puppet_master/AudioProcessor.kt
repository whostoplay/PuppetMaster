package org.menagerie.puppet_master

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.apache.commons.math3.transform.DftNormalization
import org.apache.commons.math3.transform.FastFourierTransformer
import org.apache.commons.math3.transform.TransformType
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.TargetDataLine
import javax.sound.sampled.Mixer
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
    private var smoothedLevel: Float = 0f

    /**
     * Starts listening to the microphone and reporting audio levels.
     * The audio processing is done in a background coroutine.
     *
     * @param onLevelChange A callback that receives the audio level as a Float between 0.0 and 1.0.
     * @param onFrequencyData An optional callback that receives the frequency spectrum as a FloatArray.
     */
    actual fun start(
        onLevelChange: (Float) -> Unit,
        onFrequencyData: ((FloatArray) -> Unit)?,
        mixerName: String?,
        onError: (String) -> Unit,
    ) {
        audioJob?.cancel()
        audioJob = audioScope.launch {
            var dataLine: TargetDataLine? = null
            try {
                val format = AudioFormat(SAMPLE_RATE, SAMPLE_SIZE_IN_BITS, CHANNELS, IS_SIGNED, IS_BIG_ENDIAN)
                val info = DataLine.Info(TargetDataLine::class.java, format)


                val dataLine = if (mixerName != null) {
                    val mixer = getMixerByName(mixerName)
                    if (mixer == null) {
                        onError("Audio device not found: $mixerName. Using default.")
                        AudioSystem.getLine(info) as TargetDataLine
                    } else {
                        mixer.getLine(info) as TargetDataLine
                    }
                } else {
                    AudioSystem.getLine(info) as TargetDataLine
                }

                dataLine.open(format)
                dataLine.start()

                val buffer = ByteArray(BUFFER_SIZE)
                val fft = if (onFrequencyData != null) FastFourierTransformer(DftNormalization.STANDARD) else null

                while (isActive) {
                    val bytesRead = dataLine.read(buffer, 0, buffer.size)
                    if (bytesRead > 0) {
                        val level = calculateAudioLevel(buffer, bytesRead)
                        onLevelChange(level)

                        if (onFrequencyData != null && fft != null) {
                            val frequencyData = performFFT(buffer, bytesRead, fft)
                            onFrequencyData(frequencyData)
                        }
                    }
                }
            } catch (e: Exception) {
                onError("Error initializing audio: ${e.message}")
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

    private fun performFFT(audioData: ByteArray, bytesRead: Int, fft: FastFourierTransformer): FloatArray {
        val numSamples = bytesRead / 2
        val fftBuffer = DoubleArray(numSamples)

        for (i in 0 until numSamples) {
            val byteIndex = i * 2
            // Little-endian conversion from 2 bytes to a short
            val sample = ((audioData[byteIndex + 1].toInt() shl 8) or (audioData[byteIndex].toInt() and 0xFF)).toShort()
            fftBuffer[i] = sample.toDouble()
        }

        // Perform FFT
        val result = fft.transform(fftBuffer, TransformType.FORWARD)

        // Calculate magnitudes
        val magnitudes = FloatArray(numSamples / 2)
        for (i in 0 until numSamples / 2) {
            magnitudes[i] = result[i].abs().toFloat()
        }
        return magnitudes
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

        val amplifiedLevel = (normalizedRms).coerceIn(0f, 1f)
        smoothedLevel += (amplifiedLevel - smoothedLevel) * SMOOTHING_FACTOR

        return smoothedLevel
    }

    /**
     * Cancels the audio processing coroutine scope. This should be called when the AudioProcessor is no longer needed.
     */
    fun release() {
        audioScope.cancel()
    }

    private fun getMixerByName(name: String): Mixer? {
        return AudioSystem.getMixerInfo()
            .firstOrNull { it.name == name }
            ?.let { AudioSystem.getMixer(it) }
    }

    companion object {
        private const val SAMPLE_RATE = 16000f
        private const val SAMPLE_SIZE_IN_BITS = 16
        private const val CHANNELS = 1
        private const val IS_SIGNED = true
        private const val IS_BIG_ENDIAN = false // For WAV format, data is usually little-endian
        private const val BUFFER_SIZE = 2048
        private const val MAX_AMPLITUDE = 32767.0 // Max value for 16-bit signed audio

        private const val SMOOTHING_FACTOR = 0.1f // Increase for faster response, decrease for more smoothing


        /**
         * Returns a list of available audio input device names.
         */
        fun getAvailableInputs(): List<String> {
            val format = AudioFormat(SAMPLE_RATE, SAMPLE_SIZE_IN_BITS, CHANNELS, IS_SIGNED, IS_BIG_ENDIAN)
            val info = DataLine.Info(TargetDataLine::class.java, format)
            return AudioSystem.getMixerInfo()
                .map { AudioSystem.getMixer(it) }
                .filter { it.isLineSupported(info) }
                .map { it.mixerInfo.name }
        }

    }
}
