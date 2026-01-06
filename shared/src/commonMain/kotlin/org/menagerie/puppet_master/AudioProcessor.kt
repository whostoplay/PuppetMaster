package org.menagerie.puppet_master

/**
 * A platform-agnostic interface for processing audio input.
 */
expect class AudioProcessor(context: Any) {

    /**
     * Starts listening to the microphone.
     * @param onLevelChange A callback that will be invoked with the audio level (a value between 0.0 and 1.0).
     * @param onFrequencyData A callback that will be invoked with an array of floats representing the frequency spectrum.
     */
    fun start(
        onLevelChange: (Float) -> Unit,
        onFrequencyData: ((FloatArray) -> Unit)? = null,
        mixerName: String?,
        onError: (String) -> Unit,
    )


    /**
     * Stops listening to the microphone.
     */
    fun stop()
}
