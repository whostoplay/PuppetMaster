package org.menagerie.puppet_master

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.apache.commons.math3.complex.Complex
import org.apache.commons.math3.transform.DftNormalization
import org.apache.commons.math3.transform.FastFourierTransformer
import org.apache.commons.math3.transform.TransformType
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
    private var smoothedLevel: Float = 0f
    private var noiseProfile: FloatArray = floatArrayOf()

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
    ) {
        audioJob?.cancel()
        audioJob = audioScope.launch {
            var dataLine: TargetDataLine? = null
            try {
                val format = AudioFormat(SAMPLE_RATE, SAMPLE_SIZE_IN_BITS, CHANNELS, IS_SIGNED, IS_BIG_ENDIAN)
                val info = DataLine.Info(TargetDataLine::class.java, format)

                val dataLine = AudioSystem.getLine(info) as TargetDataLine

                dataLine.open(format)
                dataLine.start()

                val buffer = ByteArray(BUFFER_SIZE)
                val fft = FastFourierTransformer(DftNormalization.STANDARD)

                val numSamples = BUFFER_SIZE / 2
                noiseProfile = FloatArray(numSamples / 2 + 1)

                while (isActive) {
                    val bytesRead = dataLine.read(buffer, 0, buffer.size)
                    if (bytesRead > 0) {
                        val currentNumSamples = bytesRead / 2
                        if (currentNumSamples == 0) continue

                        // 1. Convert byte buffer to double array for FFT
                        val samples = DoubleArray(currentNumSamples) { i ->
                            val byteIndex = i * 2
                            ((buffer[byteIndex + 1].toInt() shl 8) or (buffer[byteIndex].toInt() and 0xFF)).toShort().toDouble()
                        }

                        // Pad with zeros if necessary to match BUFFER_SIZE for consistent FFT size
                        val paddedSamples = if (samples.size < numSamples) {
                            samples.copyOf(numSamples)
                        } else {
                            samples
                        }

                        // Calculate unfiltered level to decide on noise profile update
                        val unfilteredRms = sqrt(paddedSamples.sumOf { it * it } / paddedSamples.size)
                        val unfilteredNormalizedRms = (unfilteredRms / MAX_AMPLITUDE).toFloat()

                        // 2. Perform forward FFT
                        val spectrum = fft.transform(paddedSamples, TransformType.FORWARD)

                        // 3. Get magnitudes
                        val magnitudes = FloatArray(numSamples / 2 + 1) { i ->
                            spectrum[i].abs().toFloat()
                        }

                        // 4. Update noise profile if signal is weak (likely just noise)
                        if (unfilteredNormalizedRms < NOISE_LEARN_THRESHOLD) {
                            for (i in noiseProfile.indices) {
                                noiseProfile[i] = (1 - NOISE_PROFILE_ALPHA) * noiseProfile[i] + NOISE_PROFILE_ALPHA * magnitudes[i]
                            }
                        }

                        // 5. Spectral subtraction
                        val cleanedMagnitudes = FloatArray(magnitudes.size) { i ->
                            val reduction = noiseProfile[i] * NOISE_REDUCTION_FACTOR
                            (magnitudes[i] - reduction).coerceAtLeast(0f)
                        }

                        // 6. Reconstruct the complex spectrum with new magnitudes while preserving phase
                        val cleanedSpectrum = Array(spectrum.size) { i ->
                            val originalMagnitude = spectrum[i].abs()
                            if (originalMagnitude > 0) {
                                val magIndex = if (i <= spectrum.size / 2) i else spectrum.size - i
                                val scale = cleanedMagnitudes[magIndex] / originalMagnitude.toFloat()
                                spectrum[i].multiply(scale.toDouble())
                            } else {
                                Complex.ZERO
                            }
                        }

                        // 7. Perform inverse FFT to get cleaned audio signal
                        val cleanedSamplesComplex = fft.transform(cleanedSpectrum, TransformType.INVERSE)
                        val cleanedSamples = DoubleArray(cleanedSamplesComplex.size) { i ->
                            cleanedSamplesComplex[i].real
                        }

                        // 8. Calculate audio level from the cleaned samples
                        val level = calculateAudioLevelFromSamples(cleanedSamples)
                        onLevelChange(level)

                        // 9. Provide cleaned frequency data if requested
                        onFrequencyData?.invoke(cleanedMagnitudes)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                dataLine?.stop()
                dataLine?.close()
            }
        }
    }

    /**
     * Calculates the audio level from an array of cleaned audio samples.
     * The level is the normalized and smoothed Root Mean Square (RMS) of the audio samples.
     *
     * @param samples The array of cleaned audio samples.
     * @return The audio level, a float value between 0.0 and 1.0.
     */
    private fun calculateAudioLevelFromSamples(samples: DoubleArray): Float {
        if (samples.isEmpty()) return 0f

        val sumOfSquares = samples.sumOf { it * it }
        val rms = sqrt(sumOfSquares / samples.size)

        // Normalize, coerce, and smooth
        val normalizedRms = (rms / MAX_AMPLITUDE).toFloat()
        val amplifiedLevel = normalizedRms.coerceIn(0f, 1f)
        smoothedLevel += (amplifiedLevel - smoothedLevel) * SMOOTHING_FACTOR

        return smoothedLevel
    }


    /**
     * Stops listening to the microphone and releases audio resources.
     */
    actual fun stop() {
        audioJob?.cancel()
        // The coroutine's finally block will handle resource cleanup.
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

        private const val SMOOTHING_FACTOR = 0.1f // Increase for faster response, decrease for more smoothing

        // --- Noise Reduction Constants ---
        // How quickly the noise profile adapts. Lower is slower.
        private const val NOISE_PROFILE_ALPHA = 0.05f
        // How aggressively to reduce noise. Higher values can cause more distortion.
        private const val NOISE_REDUCTION_FACTOR = 1.5f
        // Audio level below which we'll assume it's just noise and learn the profile.
        private const val NOISE_LEARN_THRESHOLD = 0.05f
    }
}
