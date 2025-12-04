package org.menagerie.puppet_master.navigation

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen
import org.menagerie.puppet_master.AppContent

class HomeScreen : Screen {

    @Composable
    override fun Content() {
        AppContent()
    }
}