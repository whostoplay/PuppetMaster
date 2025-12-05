package org.menagerie.puppet_master.navigation

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import org.menagerie.puppet_master.AppContent
import org.menagerie.puppet_master.MainViewModel
import org.menagerie.puppet_master.getContext

class HomeScreen : Screen {

    @Composable
    override fun Content() {
        val context = getContext()
        val viewModel = rememberScreenModel { MainViewModel(context) }
        AppContent(viewModel)
    }
}