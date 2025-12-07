package org.menagerie.puppet_master

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.TargetDataLine

actual class AudioProcessor actual constructor(context: Any) {

    private var audioJob: Job? = null
    private var line: TargetDataLine? = null

    actual fun start(onLevelChange: (Float) -> Unit) {
        audioJob?.cancel()
        line?.close()
        audioJob = GlobalScope.launch {
            try {
                val format = AudioFormat(16000f, 16, 1, true, false)
                val info = DataLine.Info(TargetDataLine::class.java, format)
                line = AudioSystem.getLine(info) as TargetDataLine
                line?.open(format)
                line?.start()

                val buffer = ByteArray(2048)

                while (true) {
                    val bytesRead = line?.read(buffer, 0, buffer.size) ?: 0
                    if (bytesRead > 0) {
                        val shortBuffer = ShortArray(bytesRead / 2)
                        for (i in 0 until bytesRead step 2) {
                            shortBuffer[i / 2] = ((buffer[i + 1].toInt() shl 8) or (buffer[i].toInt() and 0xFF)).toShort()
                        }
                        val level = calculateAudioLevel(shortBuffer, shortBuffer.size)
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
        line?.stop()
        line?.close()
        line = null
    }

    private fun calculateAudioLevel(audioData: ShortArray, readSize: Int): Float {
        var sum = 0.0
        for (i in 0 until readSize) {
            sum += audioData[i] * audioData[i]
        }
        val rms = Math.sqrt(sum / readSize)
        val normalizedRms = (rms / 32767).toFloat()
        return normalizedRms.coerceIn(0f, 1f)
    }
}