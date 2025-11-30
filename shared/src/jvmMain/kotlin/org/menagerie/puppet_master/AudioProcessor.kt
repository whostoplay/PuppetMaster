package org.menagerie.puppet_master

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.TargetDataLine
import kotlin.math.sqrt

actual class AudioProcessor actual constructor(context: Any) {

    private var audioJob: Job? = null

    actual fun start(onLevelChange: (Float) -> Unit) {
        audioJob = GlobalScope.launch {
            try {
                val format = AudioFormat(16000f, 16, 1, true, false)
                val info = DataLine.Info(TargetDataLine::class.java, format)
                val line = AudioSystem.getLine(info) as TargetDataLine
                line.open(format)
                line.start()

                val buffer = ByteArray(2048)

                while (true) {
                    val bytesRead = line.read(buffer, 0, buffer.size)
                    if (bytesRead > 0) {
                        var sum = 0.0
                        for (i in 0 until bytesRead step 2) {
                            val sample = ((buffer[i + 1].toInt() shl 8) or (buffer[i].toInt() and 0xFF)).toShort()
                            sum += sample * sample
                        }
                        val rms = sqrt(sum / (bytesRead / 2))

                        // Normalize the RMS value to a float between 0.0 and 1.0.
                        // The max RMS value is 32767 for 16-bit PCM audio.
                        val level = (rms / 32767.0).toFloat()
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