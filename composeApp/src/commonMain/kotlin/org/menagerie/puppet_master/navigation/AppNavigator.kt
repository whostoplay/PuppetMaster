package org.menagerie.puppet_master.navigation

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideOrientation
import cafe.adriel.voyager.transitions.SlideTransition
import org.menagerie.puppet_master.ui.theme.MainTheme

@Composable
fun AppNavigator(window: Any? = null) {
    MainTheme {
        Navigator(screen = SplashScreen(window)) {
            SlideTransition(it, orientation = SlideOrientation.Vertical)
        }
    }
}