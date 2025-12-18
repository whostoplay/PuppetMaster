package org.menagerie.puppet_master.navigation

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideOrientation
import cafe.adriel.voyager.transitions.SlideTransition
import org.menagerie.puppet_master.ui.theme.MainTheme

/**
 * The main entry point for the application's navigation.
 *
 * This composable sets up the navigation stack using Voyager's `Navigator` and applies a `SlideTransition`.
 * The initial screen is [SplashScreen].
 *
 * @param window The window object, which can be passed to the initial screen.
 */
@Composable
fun AppNavigator(window: Any? = null) {
    MainTheme {
        Navigator(screen = SplashScreen(window)) {
            SlideTransition(it, orientation = SlideOrientation.Vertical)
        }
    }
}
