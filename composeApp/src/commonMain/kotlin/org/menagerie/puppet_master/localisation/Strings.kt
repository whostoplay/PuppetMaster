package org.menagerie.puppet_master.localisation

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import org.menagerie.puppet_master.SettingsProvider

object Strings {
    object Keys {
        //region ColorPicker
        const val BG_COLOR_TEXT = "BG_COLOR_TEXT"
        const val COLOR_MAP_CONTENT_DESCRIPTION = "COLOR_MAP_CONTENT_DESCRIPTION"
        //endregion

        //region DraggableSplitter
        const val DRAG_TO_RESIZE_CONTENT_DESCRIPTION = "DRAG_TO_RESIZE_CONTENT_DESCRIPTION"
        //endregion

        //region HotkeySelector
        const val PRESS_ANY_KEY = "PRESS_ANY_KEY"
        const val NOT_ASSIGNED = "NOT_ASSIGNED"
        const val TOGGLE = "TOGGLE"
        const val HOLD = "HOLD"
        //endregion

        //region ModeControls
        const val OFFLINE = "OFFLINE"
        const val ONLINE = "ONLINE"
        const val DIRECT = "DIRECT"
        const val STATE_MACHINE = "STATE_MACHINE"
        //endregion

        //region NameTroupeDialog
        const val NAME_YOUR_TROUPE_TITLE = "NAME_YOUR_TROUPE_TITLE"
        const val NAME_YOUR_TROUPE_TEXT = "NAME_YOUR_TROUPE_TEXT"
        const val TROUPE_NAME_LABEL = "TROUPE_NAME_LABEL"
        const val SAVE_BUTTON = "SAVE_BUTTON"
        //endregion

        //region PuppetControls
        const val ACTIVE_PUPPET_LABEL = "ACTIVE_PUPPET_LABEL"
        const val NEW_PUPPET_NAME_PLACEHOLDER = "NEW_PUPPET_NAME_PLACEHOLDER"
        const val CREATE_BUTTON = "CREATE_BUTTON"
        const val LOAD_TROUPE_BUTTON = "LOAD_TROUPE_BUTTON"
        const val RENAME_TROUPE_BUTTON = "RENAME_TROUPE_BUTTON"
        const val IMPORT_PUPPET_BUTTON = "IMPORT_PUPPET_BUTTON"
        const val EXPORT_ACTIVE_PUPPET_BUTTON = "EXPORT_ACTIVE_PUPPET_BUTTON"
        const val CREATE_NEW_TROUPE_BUTTON = "CREATE_NEW_TROUPE_BUTTON"
        //endregion

        //region RenameTroupeDialog
        const val RENAME_TROUPE_TITLE = "RENAME_TROUPE_TITLE"
        const val NEW_TROUPE_NAME_LABEL = "NEW_TROUPE_NAME_LABEL"
        const val RENAME_BUTTON = "RENAME_BUTTON"
        //endregion

        //region ServerControls
        const val SERVER_CONTROLS_TITLE = "SERVER_CONTROLS_TITLE"
        const val SERVER_IP_LABEL = "SERVER_IP_LABEL"
        const val START_SERVER_BUTTON = "START_SERVER_BUTTON"
        const val STOP_SERVER_BUTTON = "STOP_SERVER_BUTTON"
        //endregion

        //region SpecialEffectsView
        const val SPECIAL_EFFECTS_TITLE = "SPECIAL_EFFECTS_TITLE"
        const val ADD_EFFECT_BUTTON = "ADD_EFFECT_BUTTON"
        const val DELETE_EFFECT_BUTTON = "DELETE_EFFECT_BUTTON"
        const val DISPLAY_PREVIEW_CHECKBOX = "DISPLAY_PREVIEW_CHECKBOX"
        const val PREVIOUS_EFFECT_BUTTON = "PREVIOUS_EFFECT_BUTTON"
        const val NEXT_EFFECT_BUTTON = "NEXT_EFFECT_BUTTON"
        const val TOGGLE_SCALE_DETAILS_BUTTON = "TOGGLE_SCALE_DETAILS_BUTTON"
        const val SCALE_SLIDER = "SCALE_SLIDER"
        const val SCALE_X_SLIDER = "SCALE_X_SLIDER"
        const val SCALE_Y_SLIDER = "SCALE_Y_SLIDER"
        const val SCALE_SPEED_SLIDER = "SCALE_SPEED_SLIDER"
        const val TOGGLE_VIBRATION_DETAILS_BUTTON = "TOGGLE_VIBRATION_DETAILS_BUTTON"
        const val VIBRATION_SLIDER = "VIBRATION_SLIDER"
        const val VIBRATION_DISTANCE_SLIDER = "VIBRATION_DISTANCE_SLIDER"
        const val VIBRATION_SPEED_SLIDER = "VIBRATION_SPEED_SLIDER"
        const val GLOW_LUMINANCE_SLIDER = "GLOW_LUMINANCE_SLIDER"
        const val SPIN_SPEED_SLIDER = "SPIN_SPEED_SLIDER"
        const val SPIN_DIRECTION_LABEL = "SPIN_DIRECTION_LABEL"
        const val SPIN_LEFT_ICON = "SPIN_LEFT_ICON"
        const val SPIN_RIGHT_ICON = "SPIN_RIGHT_ICON"
        const val PRESERVE_STATE_CHECKBOX = "PRESERVE_STATE_CHECKBOX"
        const val RENAME_BEFORE_SAVING_TEXT = "RENAME_BEFORE_SAVING_TEXT"
        const val NAME_MUST_BE_UNIQUE_TEXT = "NAME_MUST_BE_UNIQUE_TEXT"
        //endregion

        //region Common
        const val ASSIGN_STATE_TITLE = "ASSIGN_STATE_TITLE"
        const val OVERWRITE_STATE_TITLE = "OVERWRITE_STATE_TITLE"
        const val OVERWRITE_STATE_TEXT = "OVERWRITE_STATE_TEXT"
        const val CONNECTION_FAILED_TITLE = "CONNECTION_FAILED_TITLE"
        const val CONNECTION_FAILED_TEXT = "CONNECTION_FAILED_TEXT"
        const val CANCEL_BUTTON = "CANCEL_BUTTON"
        const val OVERWRITE_BUTTON = "OVERWRITE_BUTTON"
        const val TRY_AGAIN_BUTTON = "TRY_AGAIN_BUTTON"
        const val WORK_OFFLINE_BUTTON = "WORK_OFFLINE_BUTTON"
        const val GETTING_STARTED = "GETTING_STARTED"
        const val LOAD_STATE_IMAGE = "LOAD_STATE_IMAGE"
        const val CREATE_OR_SELECT_PUPPET = "CREATE_OR_SELECT_PUPPET"
        const val EYE_CONTACT_BUTTON = "EYE_CONTACT_BUTTON"
        const val SETTINGS_BUTTON = "SETTINGS_BUTTON"
        const val STATES_TITLE = "STATES_TITLE"
        const val PUPPET_CONTROLS_BUTTON = "PUPPET_CONTROLS_BUTTON"
        const val STATE_CONTROLS_BUTTON = "STATE_CONTROLS_BUTTON"
        const val WINDOW_TITLE = "WINDOW_TITLE"
        const val LOADING = "LOADING"
        const val APPLY = "APPLY"
        const val NONE = "NONE"
        //endregion

        //region EyeContactScreen
        const val EYE_CONTACT_STUDIO_TITLE = "EYE_CONTACT_STUDIO_TITLE"
        const val BACK_BUTTON_CONTENT_DESCRIPTION = "BACK_BUTTON_CONTENT_DESCRIPTION"
        const val SELECT_EYES_FROM_TROUPE_CONTENT_DESCRIPTION = "SELECT_EYES_FROM_TROUPE_CONTENT_DESCRIPTION"
        const val SELECT_A_STATE = "SELECT_A_STATE"
        const val SYNC_EYE_PARTS = "SYNC_EYE_PARTS"
        const val FOLLOW_CURSOR = "FOLLOW_CURSOR"
        const val FOCUS_ON_GAME = "FOCUS_ON_GAME"
        const val CHECK_ON_AUDIENCE = "CHECK_ON_AUDIENCE"
        const val BLINK_RATE_RANGE = "BLINK_RATE_RANGE"
        const val AUDIENCE_CHECK_RATE = "AUDIENCE_CHECK_RATE"
        const val AUDIENCE_CHECK_DURATION = "AUDIENCE_CHECK_DURATION"
        const val LEFT_EYE = "LEFT_EYE"
        const val RIGHT_EYE = "RIGHT_EYE"
        const val IRIS = "IRIS"
        const val PUPIL = "PUPIL"
        const val BLINK = "BLINK"
        const val CLOSED_EYES_PREVIEW = "CLOSED_EYES_PREVIEW"
        const val SELECT_STATE_TO_BEGIN = "SELECT_STATE_TO_BEGIN"
        const val STATE_PREVIEW_CONTENT_DESCRIPTION = "STATE_PREVIEW_CONTENT_DESCRIPTION"
        const val GAME_SCREEN = "GAME_SCREEN"
        const val SELECT_PART_TITLE = "SELECT_PART_TITLE"
        const val DRAGGABLE_CLOSED_EYE_CONTENT_DESCRIPTION = "DRAGGABLE_CLOSED_EYE_CONTENT_DESCRIPTION"
        const val DRAGGABLE_OPEN_EYE_CONTENT_DESCRIPTION = "DRAGGABLE_OPEN_EYE_CONTENT_DESCRIPTION"
        const val DRAGGABLE_PUPIL_CONTENT_DESCRIPTION = "DRAGGABLE_PUPIL_CONTENT_DESCRIPTION"
        //endregion

        //region SettingsScreen
        const val SETTINGS_TITLE = "SETTINGS_TITLE"
        const val BACK = "BACK"
        const val SERVER_IP_ADDRESS = "SERVER_IP_ADDRESS"
        const val START_OFFLINE = "START_OFFLINE"
        const val FULLSCREEN_ON_STARTUP = "FULLSCREEN_ON_STARTUP"
        const val TOGGLE_LISTEN_HOTKEY = "TOGGLE_LISTEN_HOTKEY"
        const val TOGGLE_PUBLISHING_HOTKEY = "TOGGLE_PUBLISHING_HOTKEY"
        const val TOGGLE_ONLINE_HOTKEY = "TOGGLE_ONLINE_HOTKEY"
        const val CHANGE_FOCUS_HOTKEY = "CHANGE_FOCUS_HOTKEY"
        const val CHECK_AUDIENCE_HOTKEY = "CHECK_AUDIENCE_HOTKEY"
        const val TOGGLE_CONTROLS_HOTKEY = "TOGGLE_CONTROLS_HOTKEY"
        const val LANGUAGE = "LANGUAGE_SETTING"
        //endregion

        //region TroupeEyesPopup
        const val SELECT_AN_EYE_SET = "SELECT_AN_EYE_SET"
        //endregion

        //region LivePreview
        const val LIVE_PREVIEW_CONTENT_DESCRIPTION = "LIVE_PREVIEW_CONTENT_DESCRIPTION"
        const val LEFT_PUPIL_CONTENT_DESCRIPTION = "LEFT_PUPIL_CONTENT_DESCRIPTION"
        const val RIGHT_PUPIL_CONTENT_DESCRIPTION = "RIGHT_PUPIL_CONTENT_DESCRIPTION"
        //endregion

        //region StateCreation
        const val CREATE_NEW_STATE = "CREATE_NEW_STATE"
        const val MAIN_IMAGE = "MAIN_IMAGE"
        const val SELECTED_IMAGE = "SELECTED_IMAGE"
        const val SWAP_IMAGES = "SWAP_IMAGES"
        const val BLINK_IMAGE = "BLINK_IMAGE"
        const val SELECTED_BLINK_IMAGE = "SELECTED_BLINK_IMAGE"
        const val SELECT_IMAGES = "SELECT_IMAGES"
        const val ADD_BLINKING_STATE = "ADD_BLINKING_STATE"
        const val STATE_NAME = "STATE_NAME"
        const val SAVE_UPDATE_STATE = "SAVE_UPDATE_STATE"
        const val MISSING_ERROR_MESSAGE = "MISSING_ERROR_MESSAGE"
        //endregion

        //region StateEditor
        const val EDIT_STATE = "EDIT_STATE"
        const val APPLIED_EFFECT = "APPLIED_EFFECT"
        const val STATE_HOTKEY = "STATE_HOTKEY"
        //endregion

        //region State Graph
        const val STATE_GRAPH_BUTTON = "STATE_GRAPH"
        const val HIGHLIGHT_ON = "HIGHLIGHT_ON"
    }

    enum class Language {
        ENGLISH,
        FRENCH,
        PIRATE
    }

    private val strings = mutableMapOf<Language, LocalizedStrings>()
    private lateinit var currentLanguage: MutableState<Language>

    fun init(language: Language, english: LocalizedStrings, french: LocalizedStrings, pirate: LocalizedStrings) {
        strings[Language.ENGLISH] = english
        strings[Language.FRENCH] = french
        strings[Language.PIRATE] = pirate
        currentLanguage = mutableStateOf(language)
    }

    fun setLanguage(language: Language) {
        currentLanguage.value = language
    }

    fun getString(key: String): String {
        val language = currentLanguage.value
        val languageStrings = strings[language] ?: strings[Language.ENGLISH]
        return languageStrings?.strings?.get(key) ?: key
    }

    fun getString(key: String, vararg formatArgs: Any?): String {
        return String.format(getString(key), *formatArgs)
    }

    interface LocalizedStrings {
        val strings: Map<String, String>
    }
}