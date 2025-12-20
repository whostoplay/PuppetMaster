package org.menagerie.puppet_master

import kotlin.text.get

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
    }

    enum class Language {
        ENGLISH,
        FRENCH,
        PIRATE
    }

    private val strings = mapOf(
        Language.ENGLISH to English,
        Language.FRENCH to French,
        Language.PIRATE to Pirate
    )

    fun getString(key: String): String {
        try {
            val repository = SettingsProvider.get()

            val language = repository.loadSettings().language

            val languageStrings = strings[language] ?: English

            return languageStrings.strings[key] ?: key
        }  catch (e: IllegalStateException) {
            // This happens if SettingsProvider wasn't initialized.
            // Fallback gracefully to the key itself.
            println("Warning: SettingsProvider not initialized. Using string key as fallback. ${e.message}")
            return key
        }
    }

    interface LocalizedStrings {
        val strings: Map<String, String>
    }

    object English : LocalizedStrings {
        override val strings = mapOf(
            Keys.BG_COLOR_TEXT to "BG Color",
            Keys.COLOR_MAP_CONTENT_DESCRIPTION to "Color Map",
            Keys.DRAG_TO_RESIZE_CONTENT_DESCRIPTION to "Drag to resize",
            Keys.PRESS_ANY_KEY to "Press any key...",
            Keys.NOT_ASSIGNED to "Not Assigned",
            Keys.TOGGLE to "Toggle",
            Keys.HOLD to "Hold",
            Keys.OFFLINE to "Offline",
            Keys.ONLINE to "Online",
            Keys.NAME_YOUR_TROUPE_TITLE to "Name Your Troupe",
            Keys.NAME_YOUR_TROUPE_TEXT to "This is the first puppet in your troupe. Please name your troupe to continue.",
            Keys.TROUPE_NAME_LABEL to "Troupe Name",
            Keys.SAVE_BUTTON to "Save",
            Keys.ACTIVE_PUPPET_LABEL to "Active Puppet",
            Keys.NEW_PUPPET_NAME_PLACEHOLDER to "New Puppet Name",
            Keys.CREATE_BUTTON to "Create",
            Keys.LOAD_TROUPE_BUTTON to "Load Troupe",
            Keys.RENAME_TROUPE_BUTTON to "Rename Troupe",
            Keys.IMPORT_PUPPET_BUTTON to "Import Puppet",
            Keys.EXPORT_ACTIVE_PUPPET_BUTTON to "Export Active Puppet",
            Keys.CREATE_NEW_TROUPE_BUTTON to "Create New Troupe",
            Keys.RENAME_TROUPE_TITLE to "Rename Troupe",
            Keys.NEW_TROUPE_NAME_LABEL to "New Troupe Name",
            Keys.RENAME_BUTTON to "Rename",
            Keys.SERVER_CONTROLS_TITLE to "Server Controls",
            Keys.SERVER_IP_LABEL to "Server IP",
            Keys.START_SERVER_BUTTON to "Start Server",
            Keys.STOP_SERVER_BUTTON to "Stop Server",
            Keys.SPECIAL_EFFECTS_TITLE to "Special Effects",
            Keys.ADD_EFFECT_BUTTON to "Add Effect",
            Keys.DELETE_EFFECT_BUTTON to "Delete Effect",
            Keys.DISPLAY_PREVIEW_CHECKBOX to "Display Preview",
            Keys.PREVIOUS_EFFECT_BUTTON to "Previous Effect",
            Keys.NEXT_EFFECT_BUTTON to "Next Effect",
            Keys.TOGGLE_SCALE_DETAILS_BUTTON to "Toggle Scale Details",
            Keys.SCALE_SLIDER to "Scale",
            Keys.SCALE_X_SLIDER to "Scale X",
            Keys.SCALE_Y_SLIDER to "Scale Y",
            Keys.SCALE_SPEED_SLIDER to "Scale Speed",
            Keys.TOGGLE_VIBRATION_DETAILS_BUTTON to "Toggle Vibration Details",
            Keys.VIBRATION_SLIDER to "Vibration",
            Keys.VIBRATION_DISTANCE_SLIDER to "Distance",
            Keys.VIBRATION_SPEED_SLIDER to "Speed",
            Keys.GLOW_LUMINANCE_SLIDER to "Glow (Luminance)",
            Keys.SPIN_SPEED_SLIDER to "Spin Speed",
            Keys.SPIN_DIRECTION_LABEL to "Spin Direction",
            Keys.SPIN_LEFT_ICON to "Spin Left",
            Keys.SPIN_RIGHT_ICON to "Spin Right",
            Keys.PRESERVE_STATE_CHECKBOX to "Preserve this state across puppet changes",
            Keys.RENAME_BEFORE_SAVING_TEXT to "Rename Before Saving",
            Keys.NAME_MUST_BE_UNIQUE_TEXT to "Name Must Be Unique",
            Keys.ASSIGN_STATE_TITLE to "Assign State to Threshold",
            Keys.OVERWRITE_STATE_TITLE to "Overwrite State?",
            Keys.OVERWRITE_STATE_TEXT to "A state with this name already exists. Do you want to overwrite it?",
            Keys.CONNECTION_FAILED_TITLE to "Connection Failed",
            Keys.CONNECTION_FAILED_TEXT to "Could not connect to the server. Would you like to try again?",
            Keys.CANCEL_BUTTON to "Cancel",
            Keys.OVERWRITE_BUTTON to "Overwrite",
            Keys.TRY_AGAIN_BUTTON to "Try Again",
            Keys.WORK_OFFLINE_BUTTON to "Work Offline",
            Keys.GETTING_STARTED to "Create a puppet or load a troupe to get started.",
            Keys.LOAD_STATE_IMAGE to "Load a State Image.",
            Keys.CREATE_OR_SELECT_PUPPET to "Create or select a puppet to get started.",
            Keys.EYE_CONTACT_BUTTON to "Eye Contact",
            Keys.SETTINGS_BUTTON to "Settings",
            Keys.STATES_TITLE to "States",
            Keys.PUPPET_CONTROLS_BUTTON to "Puppet Controls",
            Keys.STATE_CONTROLS_BUTTON to "State Controls",
            Keys.WINDOW_TITLE to "Puppet Master",
            Keys.LOADING to "Loading...",
            Keys.APPLY to "Apply",
            Keys.NONE to "None",
            Keys.EYE_CONTACT_STUDIO_TITLE to "Eye Contact Studio",
            Keys.BACK_BUTTON_CONTENT_DESCRIPTION to "Back",
            Keys.SELECT_EYES_FROM_TROUPE_CONTENT_DESCRIPTION to "Select Eyes from Troupe",
            Keys.SELECT_A_STATE to "Select a State",
            Keys.SYNC_EYE_PARTS to "Sync Eye Parts",
            Keys.FOLLOW_CURSOR to "Follow Cursor",
            Keys.FOCUS_ON_GAME to "Focus on Game",
            Keys.CHECK_ON_AUDIENCE to "Check on Audience",
            Keys.BLINK_RATE_RANGE to "Blink Rate Range: %1\$d - %2\$d ms",
            Keys.AUDIENCE_CHECK_RATE to "Audience Check Rate: %1\$d ms",
            Keys.AUDIENCE_CHECK_DURATION to "Audience Check Duration: %1\$d ms",
            Keys.LEFT_EYE to "Left Eye",
            Keys.RIGHT_EYE to "Right Eye",
            Keys.IRIS to "Iris",
            Keys.PUPIL to "Pupil",
            Keys.BLINK to "Blink",
            Keys.CLOSED_EYES_PREVIEW to "Closed Eyes Preview",
            Keys.SELECT_STATE_TO_BEGIN to "Select a state to begin.",
            Keys.STATE_PREVIEW_CONTENT_DESCRIPTION to "State preview",
            Keys.GAME_SCREEN to "Game Screen",
            Keys.SELECT_PART_TITLE to "Select %1\$s",
            Keys.DRAGGABLE_CLOSED_EYE_CONTENT_DESCRIPTION to "Draggable closed eye",
            Keys.DRAGGABLE_OPEN_EYE_CONTENT_DESCRIPTION to "Draggable open eye",
            Keys.DRAGGABLE_PUPIL_CONTENT_DESCRIPTION to "Draggable pupil",
            Keys.SETTINGS_TITLE to "Settings",
            Keys.BACK to "Back",
            Keys.SERVER_IP_ADDRESS to "Server IP Address",
            Keys.START_OFFLINE to "Start Offline",
            Keys.FULLSCREEN_ON_STARTUP to "Fullscreen on Startup",
            Keys.TOGGLE_LISTEN_HOTKEY to "Toggle Listen Hotkey",
            Keys.TOGGLE_PUBLISHING_HOTKEY to "Toggle Publishing Hotkey",
            Keys.TOGGLE_ONLINE_HOTKEY to "Toggle Online Hotkey",
            Keys.CHANGE_FOCUS_HOTKEY to "Change Focus Hotkey",
            Keys.CHECK_AUDIENCE_HOTKEY to "Check Audience Hotkey",
            Keys.TOGGLE_CONTROLS_HOTKEY to "Toggle Controls Hotkey",
            Keys.SELECT_AN_EYE_SET to "Select an Eye Set",
            Keys.LIVE_PREVIEW_CONTENT_DESCRIPTION to "Live Preview",
            Keys.LEFT_PUPIL_CONTENT_DESCRIPTION to "Left Pupil",
            Keys.RIGHT_PUPIL_CONTENT_DESCRIPTION to "Right Pupil",
            Keys.CREATE_NEW_STATE to "Create New State",
            Keys.MAIN_IMAGE to "Main Image",
            Keys.SELECTED_IMAGE to "Selected Image",
            Keys.SWAP_IMAGES to "<->",
            Keys.BLINK_IMAGE to "Blink Image",
            Keys.SELECTED_BLINK_IMAGE to "Selected Blink Image",
            Keys.SELECT_IMAGES to "Select Image(s)",
            Keys.ADD_BLINKING_STATE to "Add Blinking State",
            Keys.STATE_NAME to "State Name",
            Keys.SAVE_UPDATE_STATE to "Save/Update State",
            Keys.MISSING_ERROR_MESSAGE to "Please select %1\$s to create or update a state.",
            Keys.EDIT_STATE to "Edit State: %1\$s",
            Keys.APPLIED_EFFECT to "Applied Effect: %1\$s",
            Keys.STATE_HOTKEY to "State Hotkey",
            Keys.LANGUAGE to "Language",
        )
    }

    object French : LocalizedStrings {
        override val strings = mapOf(
            Keys.BG_COLOR_TEXT to "Couleur de fond",
            Keys.COLOR_MAP_CONTENT_DESCRIPTION to "Carte de couleurs",
            Keys.DRAG_TO_RESIZE_CONTENT_DESCRIPTION to "Faites glisser pour redimensionner",
            Keys.PRESS_ANY_KEY to "Appuyez sur n'importe quelle touche...",
            Keys.NOT_ASSIGNED to "Non attribué",
            Keys.TOGGLE to "Basculer",
            Keys.HOLD to "Maintenir",
            Keys.OFFLINE to "Hors ligne",
            Keys.ONLINE to "En ligne",
            Keys.NAME_YOUR_TROUPE_TITLE to "Nommez votre troupe",
            Keys.NAME_YOUR_TROUPE_TEXT to "Ceci est la première marionnette de votre troupe. Veuillez nommer votre troupe pour continuer.",
            Keys.TROUPE_NAME_LABEL to "Nom de la troupe",
            Keys.SAVE_BUTTON to "Enregistrer",
            Keys.ACTIVE_PUPPET_LABEL to "Marionnette active",
            Keys.NEW_PUPPET_NAME_PLACEHOLDER to "Nouveau nom de marionnette",
            Keys.CREATE_BUTTON to "Créer",
            Keys.LOAD_TROUPE_BUTTON to "Charger la troupe",
            Keys.RENAME_TROUPE_BUTTON to "Renommer la troupe",
            Keys.IMPORT_PUPPET_BUTTON to "Importer une marionnette",
            Keys.EXPORT_ACTIVE_PUPPET_BUTTON to "Exporter la marionnette active",
            Keys.CREATE_NEW_TROUPE_BUTTON to "Créer une nouvelle troupe",
            Keys.RENAME_TROUPE_TITLE to "Renommer la troupe",
            Keys.NEW_TROUPE_NAME_LABEL to "Nouveau nom de la troupe",
            Keys.RENAME_BUTTON to "Renommer",
            Keys.SERVER_CONTROLS_TITLE to "Contrôles du serveur",
            Keys.SERVER_IP_LABEL to "IP du serveur",
            Keys.START_SERVER_BUTTON to "Démarrer le serveur",
            Keys.STOP_SERVER_BUTTON to "Arrêter le serveur",
            Keys.SPECIAL_EFFECTS_TITLE to "Effets spéciaux",
            Keys.ADD_EFFECT_BUTTON to "Ajouter un effet",
            Keys.DELETE_EFFECT_BUTTON to "Supprimer l'effet",
            Keys.DISPLAY_PREVIEW_CHECKBOX to "Afficher l'aperçu",
            Keys.PREVIOUS_EFFECT_BUTTON to "Effet précédent",
            Keys.NEXT_EFFECT_BUTTON to "Effet suivant",
            Keys.TOGGLE_SCALE_DETAILS_BUTTON to "Basculer les détails de l'échelle",
            Keys.SCALE_SLIDER to "Échelle",
            Keys.SCALE_X_SLIDER to "Échelle X",
            Keys.SCALE_Y_SLIDER to "Échelle Y",
            Keys.SCALE_SPEED_SLIDER to "Vitesse de l'échelle",
            Keys.TOGGLE_VIBRATION_DETAILS_BUTTON to "Basculer les détails de la vibration",
            Keys.VIBRATION_SLIDER to "Vibration",
            Keys.VIBRATION_DISTANCE_SLIDER to "Distance",
            Keys.VIBRATION_SPEED_SLIDER to "Vitesse",
            Keys.GLOW_LUMINANCE_SLIDER to "Lueur (Luminance)",
            Keys.SPIN_SPEED_SLIDER to "Vitesse de rotation",
            Keys.SPIN_DIRECTION_LABEL to "Sens de rotation",
            Keys.SPIN_LEFT_ICON to "Rotation à gauche",
            Keys.SPIN_RIGHT_ICON to "Rotation à droite",
            Keys.PRESERVE_STATE_CHECKBOX to "Préserver cet état lors des changements de marionnette",
            Keys.RENAME_BEFORE_SAVING_TEXT to "Renommer avant d'enregistrer",
            Keys.NAME_MUST_BE_UNIQUE_TEXT to "Le nom doit être unique",
            Keys.ASSIGN_STATE_TITLE to "Attribuer un état au seuil",
            Keys.OVERWRITE_STATE_TITLE to "Écraser l'état?",
            Keys.OVERWRITE_STATE_TEXT to "Un état avec ce nom existe déjà. Voulez-vous l'écraser?",
            Keys.CONNECTION_FAILED_TITLE to "Échec de la connexion",
            Keys.CONNECTION_FAILED_TEXT to "Impossible de se connecter au serveur. Voulez-vous réessayer?",
            Keys.CANCEL_BUTTON to "Annuler",
            Keys.OVERWRITE_BUTTON to "Écraser",
            Keys.TRY_AGAIN_BUTTON to "Réessayer",
            Keys.WORK_OFFLINE_BUTTON to "Travailler hors ligne",
            Keys.GETTING_STARTED to "Créez une marionnette ou chargez une troupe pour commencer.",
            Keys.LOAD_STATE_IMAGE to "Charger une image d'état.",
            Keys.CREATE_OR_SELECT_PUPPET to "Créez ou sélectionnez une marionnette pour commencer.",
            Keys.EYE_CONTACT_BUTTON to "Contact visuel",
            Keys.SETTINGS_BUTTON to "Paramètres",
            Keys.STATES_TITLE to "États",
            Keys.PUPPET_CONTROLS_BUTTON to "Contrôles de marionnettes",
            Keys.STATE_CONTROLS_BUTTON to "Contrôles d'état",
            Keys.WINDOW_TITLE to "Puppet Master",
            Keys.LOADING to "Chargement...",
            Keys.APPLY to "Appliquer",
            Keys.NONE to "Aucun",
            Keys.EYE_CONTACT_STUDIO_TITLE to "Studio de Contact Visuel",
            Keys.BACK_BUTTON_CONTENT_DESCRIPTION to "Retour",
            Keys.SELECT_EYES_FROM_TROUPE_CONTENT_DESCRIPTION to "Sélectionner les yeux de la troupe",
            Keys.SELECT_A_STATE to "Sélectionnez un état",
            Keys.SYNC_EYE_PARTS to "Synchroniser les parties de l'œil",
            Keys.FOLLOW_CURSOR to "Suivre le curseur",
            Keys.FOCUS_ON_GAME to "Focus sur le jeu",
            Keys.CHECK_ON_AUDIENCE to "Vérifier le public",
            Keys.BLINK_RATE_RANGE to "Plage de clignement: %1\$d - %2\$d ms",
            Keys.AUDIENCE_CHECK_RATE to "Taux de vérification de l'audience: %1\$d ms",
            Keys.AUDIENCE_CHECK_DURATION to "Durée de la vérification de l'audience: %1\$d ms",
            Keys.LEFT_EYE to "Œil gauche",
            Keys.RIGHT_EYE to "Œil droit",
            Keys.IRIS to "Iris",
            Keys.PUPIL to "Pupille",
            Keys.BLINK to "Clignement",
            Keys.CLOSED_EYES_PREVIEW to "Aperçu des yeux fermés",
            Keys.SELECT_STATE_TO_BEGIN to "Sélectionnez un état pour commencer.",
            Keys.STATE_PREVIEW_CONTENT_DESCRIPTION to "Aperçu de l'état",
            Keys.GAME_SCREEN to "Écran de jeu",
            Keys.SELECT_PART_TITLE to "Sélectionner %1\$s",
            Keys.DRAGGABLE_CLOSED_EYE_CONTENT_DESCRIPTION to "Œil fermé déplaçable",
            Keys.DRAGGABLE_OPEN_EYE_CONTENT_DESCRIPTION to "Œil ouvert déplaçable",
            Keys.DRAGGABLE_PUPIL_CONTENT_DESCRIPTION to "Pupille déplaçable",
            Keys.SETTINGS_TITLE to "Paramètres",
            Keys.BACK to "Retour",
            Keys.SERVER_IP_ADDRESS to "Adresse IP du serveur",
            Keys.START_OFFLINE to "Démarrer hors ligne",
            Keys.FULLSCREEN_ON_STARTUP to "Plein écran au démarrage",
            Keys.TOGGLE_LISTEN_HOTKEY to "Raccourci pour basculer l'écoute",
            Keys.TOGGLE_PUBLISHING_HOTKEY to "Raccourci pour basculer la publication",
            Keys.TOGGLE_ONLINE_HOTKEY to "Raccourci pour basculer en ligne",
            Keys.CHANGE_FOCUS_HOTKEY to "Raccourci pour changer le focus",
            Keys.CHECK_AUDIENCE_HOTKEY to "Raccourci pour vérifier l'audience",
            Keys.TOGGLE_CONTROLS_HOTKEY to "Raccourci pour basculer les contrôles",
            Keys.SELECT_AN_EYE_SET to "Sélectionnez un ensemble d'yeux",
            Keys.LIVE_PREVIEW_CONTENT_DESCRIPTION to "Aperçu en direct",
            Keys.LEFT_PUPIL_CONTENT_DESCRIPTION to "Pupille gauche",
            Keys.RIGHT_PUPIL_CONTENT_DESCRIPTION to "Pupille droite",
            Keys.CREATE_NEW_STATE to "Créer un nouvel état",
            Keys.MAIN_IMAGE to "Image principale",
            Keys.SELECTED_IMAGE to "Image sélectionnée",
            Keys.SWAP_IMAGES to "<->",
            Keys.BLINK_IMAGE to "Image clignotante",
            Keys.SELECTED_BLINK_IMAGE to "Image clignotante sélectionnée",
            Keys.SELECT_IMAGES to "Sélectionner une ou plusieurs images",
            Keys.ADD_BLINKING_STATE to "Ajouter un état de clignement",
            Keys.STATE_NAME to "Nom de l'état",
            Keys.SAVE_UPDATE_STATE to "Enregistrer/Mettre à jour l'état",
            Keys.MISSING_ERROR_MESSAGE to "Veuillez sélectionner %1\$s pour créer ou mettre à jour un état.",
            Keys.EDIT_STATE to "Modifier l'état: %1\$s",
            Keys.APPLIED_EFFECT to "Effet appliqué: %1\$s",
            Keys.STATE_HOTKEY to "Raccourci d'état",
            Keys.LANGUAGE to "Langue",
        )
    }

    object Pirate : LocalizedStrings {
        override val strings = mapOf(
            Keys.BG_COLOR_TEXT to "Deck's Hue",
            Keys.COLOR_MAP_CONTENT_DESCRIPTION to "Chart o' Hues",
            Keys.DRAG_TO_RESIZE_CONTENT_DESCRIPTION to "Heave to resize",
            Keys.PRESS_ANY_KEY to "Smash any key...",
            Keys.NOT_ASSIGNED to "Not Claimed",
            Keys.TOGGLE to "Switcharoo",
            Keys.HOLD to "Hold Fast",
            Keys.OFFLINE to "Landlubber",
            Keys.ONLINE to "At Sea",
            Keys.NAME_YOUR_TROUPE_TITLE to "Name Yer Crew",
            Keys.NAME_YOUR_TROUPE_TEXT to "This be the first puppet in yer crew. Give yer crew a name to continue.",
            Keys.TROUPE_NAME_LABEL to "Crew Name",
            Keys.SAVE_BUTTON to "Remember",
            Keys.ACTIVE_PUPPET_LABEL to "First Mate",
            Keys.NEW_PUPPET_NAME_PLACEHOLDER to "New Puppet Name",
            Keys.CREATE_BUTTON to "Craft",
            Keys.LOAD_TROUPE_BUTTON to "Load Crew",
            Keys.RENAME_TROUPE_BUTTON to "Rename Crew",
            Keys.IMPORT_PUPPET_BUTTON to "Import Puppet",
            Keys.EXPORT_ACTIVE_PUPPET_BUTTON to "Export Active Puppet",
            Keys.CREATE_NEW_TROUPE_BUTTON to "Hire New Crew",
            Keys.RENAME_TROUPE_TITLE to "Rename Crew",
            Keys.NEW_TROUPE_NAME_LABEL to "New Crew Name",
            Keys.RENAME_BUTTON to "Rename",
            Keys.SERVER_CONTROLS_TITLE to "Port Controls",
            Keys.SERVER_IP_LABEL to "Where Be Port?",
            Keys.START_SERVER_BUTTON to "Make Port",
            Keys.STOP_SERVER_BUTTON to "Leave Port",
            Keys.SPECIAL_EFFECTS_TITLE to "Wicked Magic",
            Keys.ADD_EFFECT_BUTTON to "Brew Effect",
            Keys.DELETE_EFFECT_BUTTON to "Curse Effect",
            Keys.DISPLAY_PREVIEW_CHECKBOX to "Gaze Upon Preview",
            Keys.PREVIOUS_EFFECT_BUTTON to "Previous Witchery",
            Keys.NEXT_EFFECT_BUTTON to "Next Witchery",
            Keys.TOGGLE_SCALE_DETAILS_BUTTON to "Toggle Scale Details",
            Keys.SCALE_SLIDER to "Scale",
            Keys.SCALE_X_SLIDER to "Scale X",
            Keys.SCALE_Y_SLIDER to "Scale Y",
            Keys.SCALE_SPEED_SLIDER to "Scale Speed",
            Keys.TOGGLE_VIBRATION_DETAILS_BUTTON to "Toggle Vibration Details",
            Keys.VIBRATION_SLIDER to "Vibration",
            Keys.VIBRATION_DISTANCE_SLIDER to "Distance",
            Keys.VIBRATION_SPEED_SLIDER to "Speed",
            Keys.GLOW_LUMINANCE_SLIDER to "Luminant Glow",
            Keys.SPIN_SPEED_SLIDER to "Roll Speed",
            Keys.SPIN_DIRECTION_LABEL to "Roll Direction",
            Keys.SPIN_LEFT_ICON to "Roll Left",
            Keys.SPIN_RIGHT_ICON to "Roll Right",
            Keys.PRESERVE_STATE_CHECKBOX to "Preserve this magic across puppet changes",
            Keys.RENAME_BEFORE_SAVING_TEXT to "Rename Before Savin'",
            Keys.NAME_MUST_BE_UNIQUE_TEXT to "Name Must Be Unique, Matey",
            Keys.ASSIGN_STATE_TITLE to "Mark Yer Spot",
            Keys.OVERWRITE_STATE_TITLE to "Overwrite Yer State?",
            Keys.OVERWRITE_STATE_TEXT to "A state with this name already be claimed. D'ye want to send it to Davy Jones' Locker?",
            Keys.CONNECTION_FAILED_TITLE to "Connection Scuttled",
            Keys.CONNECTION_FAILED_TEXT to "Couldn't make port. Heave ho and try again?",
            Keys.CANCEL_BUTTON to "Belay That",
            Keys.OVERWRITE_BUTTON to "Overwrite",
            Keys.TRY_AGAIN_BUTTON to "Heave Ho!",
            Keys.WORK_OFFLINE_BUTTON to "Go it Alone",
            Keys.GETTING_STARTED to "Craft a puppet or gather a crew t' get started.",
            Keys.LOAD_STATE_IMAGE to "Hoist a State Image.",
            Keys.CREATE_OR_SELECT_PUPPET to "Craft or pick a puppet t' get started.",
            Keys.EYE_CONTACT_BUTTON to "Meet Yer Gaze",
            Keys.SETTINGS_BUTTON to "Ship's Orders",
            Keys.STATES_TITLE to "States o' Being",
            Keys.PUPPET_CONTROLS_BUTTON to "Puppet Ropes",
            Keys.STATE_CONTROLS_BUTTON to "State Ropes",
            Keys.WINDOW_TITLE to "Master O' Puppets",
            Keys.LOADING to "Settin' Sail...",
            Keys.APPLY to "Aye!",
            Keys.NONE to "None",
            Keys.EYE_CONTACT_STUDIO_TITLE to "Eye Contact Studio",
            Keys.BACK_BUTTON_CONTENT_DESCRIPTION to "Avast",
            Keys.SELECT_EYES_FROM_TROUPE_CONTENT_DESCRIPTION to "Plunder Eyes from the Crew",
            Keys.SELECT_A_STATE to "Pick a State o' Being",
            Keys.SYNC_EYE_PARTS to "No Eye Patch For You",
            Keys.FOLLOW_CURSOR to "Follow the Pointy Thing",
            Keys.FOCUS_ON_GAME to "Eyes on the Horizon",
            Keys.CHECK_ON_AUDIENCE to "Check on the Scurvy Dogs",
            Keys.BLINK_RATE_RANGE to "Blindin' o' the Sun: %1\$d - %2\$d ms",
            Keys.AUDIENCE_CHECK_RATE to "Audience Spyin' Rate: %1\$d ms",
            Keys.AUDIENCE_CHECK_DURATION to "Audience Spyin' Duration: %1\$d ms",
            Keys.LEFT_EYE to "Port Eye",
            Keys.RIGHT_EYE to "Starboard Eye",
            Keys.IRIS to "Iris",
            Keys.PUPIL to "Pupil",
            Keys.BLINK to "Blink",
            Keys.CLOSED_EYES_PREVIEW to "Closed Eyes Preview",
            Keys.SELECT_STATE_TO_BEGIN to "Select a state to begin, matey.",
            Keys.STATE_PREVIEW_CONTENT_DESCRIPTION to "State preview",
            Keys.GAME_SCREEN to "Game Porthole",
            Keys.SELECT_PART_TITLE to "Select %1\$s",
            Keys.DRAGGABLE_CLOSED_EYE_CONTENT_DESCRIPTION to "Draggable closed eye",
            Keys.DRAGGABLE_OPEN_EYE_CONTENT_DESCRIPTION to "Draggable open eye",
            Keys.DRAGGABLE_PUPIL_CONTENT_DESCRIPTION to "Draggable pupil",
            Keys.SETTINGS_TITLE to "Ship's Orders",
            Keys.BACK to "Avast",
            Keys.SERVER_IP_ADDRESS to "Where Be Port?",
            Keys.START_OFFLINE to "Go It Alone",
            Keys.FULLSCREEN_ON_STARTUP to "Full Sail at Sunrise",
            Keys.TOGGLE_LISTEN_HOTKEY to "Toggle Ears Hotkey",
            Keys.TOGGLE_PUBLISHING_HOTKEY to "Toggle Parrot Hotkey",
            Keys.TOGGLE_ONLINE_HOTKEY to "Toggle Sea Rover Hotkey",
            Keys.CHANGE_FOCUS_HOTKEY to "Change Gaze Hotkey",
            Keys.CHECK_AUDIENCE_HOTKEY to "Check Scurvy Dogs Hotkey",
            Keys.TOGGLE_CONTROLS_HOTKEY to "Toggle Ropes Hotkey",
            Keys.SELECT_AN_EYE_SET to "Choose a Pair o' Peepers",
            Keys.LIVE_PREVIEW_CONTENT_DESCRIPTION to "Live Preview",
            Keys.LEFT_PUPIL_CONTENT_DESCRIPTION to "Portside Pupil",
            Keys.RIGHT_PUPIL_CONTENT_DESCRIPTION to "Starboard Pupil",
            Keys.CREATE_NEW_STATE to "Craft New State",
            Keys.MAIN_IMAGE to "Ship's Figurehead",
            Keys.SELECTED_IMAGE to "Selected Figurehead",
            Keys.SWAP_IMAGES to "<->",
            Keys.BLINK_IMAGE to "Winkin' Figurehead",
            Keys.SELECTED_BLINK_IMAGE to "Selected Winkin' Figurehead",
            Keys.SELECT_IMAGES to "Choose Yer Colors",
            Keys.ADD_BLINKING_STATE to "Add Winkin' State",
            Keys.STATE_NAME to "State Name",
            Keys.SAVE_UPDATE_STATE to "Save/Update State",
            Keys.MISSING_ERROR_MESSAGE to "Ahoy! Choose %1\$s to craft or update a state.",
            Keys.EDIT_STATE to "Edit State: %1\$s",
            Keys.APPLIED_EFFECT to "Applied Effect: %1\$s",
            Keys.STATE_HOTKEY to "State Hotkey",
            Keys.LANGUAGE to "Tongue",
        )
    }
}