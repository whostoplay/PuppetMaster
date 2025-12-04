package org.menagerie.puppet_master

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import org.menagerie.puppet_master.navigation.AppNavigator
import java.io.File
import java.util.Properties

fun main() = application {
    val propertiesFile = File("window.properties")

    val windowState = run {
        val properties = Properties()
        if (propertiesFile.exists()) {
            properties.load(propertiesFile.reader())
        }
        val width = properties.getProperty("width", "1024").toFloat().dp
        val height = properties.getProperty("height", "768").toFloat().dp
        val x = properties.getProperty("x", "0").toFloat().dp
        val y = properties.getProperty("y", "0").toFloat().dp

        rememberWindowState(
            width = width,
            height = height,
            position = WindowPosition(x, y)
        )
    }

    Window(
        onCloseRequest = {
            val properties = Properties().apply {
                setProperty("width", windowState.size.width.value.toString())
                setProperty("height", windowState.size.height.value.toString())
                val position = windowState.position
                if (position is WindowPosition.Absolute) {
                    setProperty("x", position.x.value.toString())
                    setProperty("y", position.y.value.toString())
                }
            }
            properties.store(propertiesFile.writer(), "Window state")
            exitApplication()
        },
        title = "Puppet Master",
        state = windowState
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(WindowInsets.captionBar.asPaddingValues())) {
            AppNavigator()
        }
    }
}
