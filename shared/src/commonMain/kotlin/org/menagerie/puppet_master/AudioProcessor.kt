package org.menagerie.puppet_master

/**
 * A platform-agnostic interface for processing audio input.
 */
expect class AudioProcessor(context: Any) {

    /**
     * Starts listening to the microphone.
     * @param onVoiceActivity A callback that will be invoked with `true` when the user is speaking
     * and `false` when they are not.
     */
    fun start(onVoiceActivity: (Boolean) -> Unit)

    /**
     * Stops listening to the microphone.
     */
    fun stop()
}
