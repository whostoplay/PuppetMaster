package org.menagerie.puppet_master

object Constants {
    object Caching {
        const val IMAGE_CACHE_DIRECTORY = "image_cache"
        const val DISK_CACHE_MAX_SIZE_BYTES = 512L * 1024 * 1024 // 512MB
        const val MEMORY_CACHE_MAX_SIZE_PERCENT = 0.25
    }

    object Server {
        /**
         * The port that the server listens on.
         */
        const val PORT = 8080
    }

    object Window {
        const val PROPERTIES_FILE_NAME = "window.properties"
        const val WINDOW_TITLE = "Puppet Master"
        const val WINDOW_PROPERTIES_HEADER = "Window state"

        const val WIDTH_PROPERTY = "width"
        const val HEIGHT_PROPERTY = "height"
        const val X_PROPERTY = "x"
        const val Y_PROPERTY = "y"

        val DEFAULT_PROPERTIES = mapOf(
            WIDTH_PROPERTY to "1024",
            HEIGHT_PROPERTY to "768",
            X_PROPERTY to "0",
            Y_PROPERTY to "0"
        )
    }

    object UI {
        const val CONTROLS_VISIBILITY_DELAY_MS = 1500L
        const val DEFAULT_PANEL_WIDTH_FRACTION = 1 / 3f

        object Dialogs {
            const val ASSIGN_STATE_TITLE = "Assign State to Threshold"
            const val OVERWRITE_STATE_TITLE = "Overwrite State?"
            const val OVERWRITE_STATE_TEXT = "A state with this name already exists. Do you want to overwrite it?"
            const val CONNECTION_FAILED_TITLE = "Connection Failed"
            const val CONNECTION_FAILED_TEXT = "Could not connect to the server. Would you like to try again?"
            const val CANCEL_BUTTON = "Cancel"
            const val OVERWRITE_BUTTON = "Overwrite"
            const val TRY_AGAIN_BUTTON = "Try Again"
            const val WORK_OFFLINE_BUTTON = "Work Offline"
        }

        object FileDialogs {
            val PUPPET_FILE_EXTENSIONS = listOf("puppet")
            val TROUPE_FILE_EXTENSIONS = listOf("troupe")
            const val PUPPET_FILE_EXTENSION = "puppet"
        }

        object Placeholders {
            const val GETTING_STARTED = "Create a puppet or load a troupe to get started."
            const val LOAD_STATE_IMAGE = "Load a State Image."
            const val CREATE_OR_SELECT_PUPPET = "Create or select a puppet to get started."
        }

        object LandscapeLayout {
            const val LEFT_PANEL_ID = "leftPanel"
            const val PUPPET_CONTROLS_ID = "puppetControls"
            const val LEFT_DRAG_ID = "leftDrag"
            const val RIGHT_DRAG_ID = "rightDrag"
            const val RIGHT_PANEL_ID = "rightPanel"
            const val STATE_CREATION_ID = "stateCreation"
            const val SPECIAL_EFFECTS_ID = "specialEffects"
            const val EYE_CONTACT_BUTTON = "Eye Contact"
            const val SETTINGS_BUTTON = "Settings"
            const val STATES_TITLE = "States"
        }

        object PortraitLayout {
            const val PUPPET_CONTROLS_ID = "puppetControlsPortrait"
            const val STATE_CREATION_ID = "stateCreationPortrait"
            const val SPECIAL_EFFECTS_ID = "specialEffectsPortrait"
            const val PUPPET_CONTROLS_BUTTON = "Puppet Controls"
            const val STATE_CONTROLS_BUTTON = "State Controls"
        }
    }

    object Puppet {
        const val IDLE_STATE_NAME = "idle"
    }
}
