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
    }
}