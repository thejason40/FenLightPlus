package com.fenlight.companion

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fenlight.companion.data.prefs.AppPreferences
import com.fenlight.companion.ui.home.HomeScreen
import com.fenlight.companion.ui.setup.SetupScreen
import com.fenlight.companion.ui.theme.FenLightTheme
import kotlinx.coroutines.flow.combine

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle TMDB OAuth redirect (fenlight://tmdb-auth)
        handleTmdbAuthIntent(intent)

        val prefs = (application as FenLightApp).prefs

        setContent {
            FenLightTheme {
                val kodiHost by prefs.kodiHost.collectAsStateWithLifecycle(initialValue = "")
                val tmdbToken by prefs.tmdbAccessToken.collectAsStateWithLifecycle(initialValue = "")
                val traktToken by prefs.traktAccessToken.collectAsStateWithLifecycle(initialValue = "")
                val rdToken by prefs.rdAccessToken.collectAsStateWithLifecycle(initialValue = "")

                val isSetupDone = kodiHost.isNotBlank() && tmdbToken.isNotBlank()
                var showSetup by remember { mutableStateOf(!isSetupDone) }

                LaunchedEffect(isSetupDone) {
                    if (isSetupDone) showSetup = false
                }

                if (showSetup) {
                    SetupScreen(onSetupComplete = { showSetup = false })
                } else {
                    HomeScreen(
                        hasTraktAuth = traktToken.isNotBlank(),
                        hasRdAuth = rdToken.isNotBlank(),
                        onGoToSettings = { showSetup = true },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleTmdbAuthIntent(intent)
    }

    private fun handleTmdbAuthIntent(intent: Intent) {
        // fenlight://tmdb-auth is the redirect URI after TMDB approval
        // The SetupViewModel's "completeTmdbAuth" button handles the token exchange.
        // We don't need to parse any data from the URL — TMDB's v3 flow just needs
        // the user to tap "Complete sign-in" after approving in the browser.
    }
}
