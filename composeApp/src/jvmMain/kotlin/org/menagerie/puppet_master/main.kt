package org.menagerie.puppet_master

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import org.menagerie.puppet_master.navigation.AppNavigator
import java.awt.Window as AwtWindow
import java.io.File
import java.util.Properties

/**
 * The main entry point of the application on desktop.
 * It sets up the main window, loads its previous state (size and position),
 * and saves the state upon closing.
 */
fun main() = application {
    val windowState = rememberWindowStateFromProperties()
    var windowRef: AwtWindow? = null

    Window(
        onCloseRequest = {
            windowRef?.let { windowState.saveToProperties(it) }
            exitApplication()
        },
        title = Constants.Window.WINDOW_TITLE,
        state = windowState
    ) {
        SideEffect { windowRef = window }
        Box(modifier = Modifier.fillMaxSize().padding(WindowInsets.captionBar.asPaddingValues())) {
            AppNavigator(window)
        }
    }
}

/**
 * A composable function that remembers the [WindowState] from a properties file.
 * If the file does not exist or a property is malformed, it uses default values.
 *
 * @return A [WindowState] object with the loaded or default values.
 */
@Composable
private fun rememberWindowStateFromProperties(): WindowState {
    val propertiesFile = File(Constants.Window.PROPERTIES_FILE_NAME)
    val properties = Properties().apply {
        if (propertiesFile.exists()) {
            runCatching {
                propertiesFile.reader().use(::load)
            }
        }
    }

    fun getDpProperty(key: String): Dp {
        val defaultValue = Constants.Window.DEFAULT_PROPERTIES[key]!!
        val value = properties.getProperty(key, defaultValue)
        return (value.toFloatOrNull() ?: defaultValue.toFloat()).dp
    }

    val width = getDpProperty(Constants.Window.WIDTH_PROPERTY)
    val height = getDpProperty(Constants.Window.HEIGHT_PROPERTY)
    val x = getDpProperty(Constants.Window.X_PROPERTY)
    val y = getDpProperty(Constants.Window.Y_PROPERTY)

    return rememberWindowState(
        width = width,
        height = height,
        position = WindowPosition(x, y)
    )
}

/**
 * Saves the window's current size and position to a properties file.
 */
private fun WindowState.saveToProperties(window: AwtWindow) {
    val propertiesFile = File(Constants.Window.PROPERTIES_FILE_NAME)
    Properties().apply {
        setProperty(Constants.Window.WIDTH_PROPERTY, window.width.toString())
        setProperty(Constants.Window.HEIGHT_PROPERTY, window.height.toString())
        val pos = position
        if (pos is WindowPosition.Absolute) {
            setProperty(Constants.Window.X_PROPERTY, pos.x.value.toString())
            setProperty(Constants.Window.Y_PROPERTY, pos.y.value.toString())
        }
    }.store(propertiesFile.writer(), Constants.Window.WINDOW_PROPERTIES_HEADER)
}
