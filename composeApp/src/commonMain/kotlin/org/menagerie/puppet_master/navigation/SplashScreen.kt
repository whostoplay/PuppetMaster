package org.menagerie.puppet_master.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.delay
import org.menagerie.puppet_master.localisation.Strings

/**
 * A temporary screen that is displayed while the application is loading.
 */
data class SplashScreen(val window: Any?) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        // This effect will be launched when the composable is first displayed.
        LaunchedEffect(Unit) {
            // Simulate a loading delay.
            delay(2000)
            // Replace the current screen with the home screen.
            navigator.replace(HomeScreen(window))
        }

        Scaffold { innerPadding ->
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(Strings.getString(Strings.Keys.LOADING))
            }
        }
    }
}
