package org.menagerie.puppet_master

import androidx.compose.ui.input.key.KeyEvent

/**
 * Handles global key events for the application.
 *
 * @param event The [KeyEvent] to handle.
 * @param viewModel The [MainViewModel] to delegate actions to.
 * @param settings The current application [SettingsModel].
 * @param isDesktop Whether the application is running on a desktop platform.
 * @param isTextFieldFocused Whether a text field currently has focus.
 * @param onToggleControls A lambda to be invoked to toggle the visibility of the main controls.
 * @return `true` if the event was handled, `false` otherwise.
 */
fun handleKeyEvent(
    event: KeyEvent,
    viewModel: MainViewModel,
    settings: SettingsModel,
    isDesktop: Boolean,
    isTextFieldFocused: Boolean,
    onToggleControls: () -> Unit
): Boolean {
    if (!isTextFieldFocused) {
        viewModel.onKeyEvent(event)
        if (isDesktop) {
            return when {
                settings.toggleListenHotkey.isHotkey(event) -> {
                    viewModel.toggleListening()
                    true
                }

                settings.togglePublishingHotkey.isHotkey(event) -> {
                    viewModel.setPublishing(!viewModel.isPublishing.value)
                    true
                }

                settings.toggleOnlineHotkey.isHotkey(event) -> {
                    viewModel.toggleOperatingMode()
                    true
                }

                settings.toggleFocusHotkey.isHotkey(event) -> {
                    viewModel.toggleFocus()
                    true
                }

                settings.toggleControlsHotkey.isHotkey(event) -> {
                    onToggleControls()
                    true
                }

                settings.checkAudienceHotkey.isHotkey(event) -> {
                    viewModel.toggleForceAudienceCheck()
                    true
                }

                else -> false
            }
        }
    }
    return false
}
