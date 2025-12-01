package org.menagerie.puppet_master

import kotlin.math.sqrt

/**
 * Calculates the normalized audio level from a buffer of audio samples.
 * @param buffer The audio buffer containing 16-bit PCM samples.
 * @param readSize The number of samples read into the buffer.
 * @return The normalized audio level (a value between 0.0 and 1.0).
 */
fun calculateAudioLevel(buffer: ShortArray, readSize: Int): Float {
    if (readSize <= 0) return 0.0f

    var sum = 0.0
    for (i in 0 until readSize) {
        sum += buffer[i] * buffer[i]
    }
    val rms = sqrt(sum / readSize)

    // Normalize the RMS value to a float between 0.0 and 1.0.
    // The max RMS value is 32767 for 16-bit PCM audio.
    return (rms / 32767.0).toFloat()
}
