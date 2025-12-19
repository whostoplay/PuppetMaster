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

        object FileDialogs {
            val PUPPET_FILE_EXTENSIONS = listOf("puppet")
            val TROUPE_FILE_EXTENSIONS = listOf("troupe")
            const val PUPPET_FILE_EXTENSION = "puppet"
        }

        object LandscapeLayout {
            const val LEFT_PANEL_ID = "leftPanel"
            const val PUPPET_CONTROLS_ID = "puppetControls"
            const val LEFT_DRAG_ID = "leftDrag"
            const val RIGHT_DRAG_ID = "rightDrag"
            const val RIGHT_PANEL_ID = "rightPanel"
            const val STATE_CREATION_ID = "stateCreation"
            const val SPECIAL_EFFECTS_ID = "specialEffects"
        }

        object PortraitLayout {
            const val PUPPET_CONTROLS_ID = "puppetControlsPortrait"
            const val STATE_CREATION_ID = "stateCreationPortrait"
            const val SPECIAL_EFFECTS_ID = "specialEffectsPortrait"
        }

        object Hotkeys {
            const val CTRL = "CTRL"
            const val ALT = "ALT"
            const val SHIFT = "SHIFT"
            const val SEPARATOR = " + "
        }
    }

    object Puppet {
        const val IDLE_STATE_NAME = "idle"
    }
}
