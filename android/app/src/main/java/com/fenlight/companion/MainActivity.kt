package com.fenlight.companion

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fenlight.companion.data.update.UpdateChecker
import com.fenlight.companion.data.update.UpdateInfo
import com.fenlight.companion.data.update.UpdateResult
import com.fenlight.companion.ui.home.HomeScreen
import com.fenlight.companion.ui.settings.SettingsScreen
import com.fenlight.companion.ui.setup.SetupScreen
import com.fenlight.companion.ui.theme.FenLightTheme
import kotlinx.coroutines.flow.first

private enum class AppScreen { Home, Setup, Settings }

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleTmdbAuthIntent(intent)

        val prefs = (application as FenLightApp).prefs

        setContent {
            FenLightTheme {
                val kodiHost by prefs.kodiHost.collectAsStateWithLifecycle(initialValue = "")
                val traktToken by prefs.traktAccessToken.collectAsStateWithLifecycle(initialValue = "")
                val rdToken by prefs.rdAccessToken.collectAsStateWithLifecycle(initialValue = "")

                val isSetupDone = kodiHost.isNotBlank()
                var screen by remember { mutableStateOf(if (isSetupDone) AppScreen.Home else AppScreen.Setup) }

                LaunchedEffect(isSetupDone) {
                    if (isSetupDone && screen == AppScreen.Setup) screen = AppScreen.Home
                }

                // Startup update check — runs once after setup is confirmed done
                var startupUpdate by remember { mutableStateOf<UpdateInfo?>(null) }
                LaunchedEffect(Unit) {
                    if (prefs.kodiHost.first().isBlank()) return@LaunchedEffect
                    if (!prefs.checkUpdateOnStartup.first()) return@LaunchedEffect
                    val result = UpdateChecker().check(BuildConfig.VERSION_CODE)
                    if (result is UpdateResult.Available) startupUpdate = result.info
                }

                when (screen) {
                    AppScreen.Setup -> SetupScreen(onSetupComplete = { screen = AppScreen.Home })
                    AppScreen.Settings -> SettingsScreen(
                        onBack = { screen = AppScreen.Home },
                        onOpenSetup = { screen = AppScreen.Setup },
                    )
                    AppScreen.Home -> HomeScreen(
                        hasTraktAuth = traktToken.isNotBlank(),
                        hasRdAuth = rdToken.isNotBlank(),
                        onGoToSettings = { screen = AppScreen.Settings },
                    )
                }

                // Startup update dialog — shown on top of any screen
                startupUpdate?.let { update ->
                    AlertDialog(
                        onDismissRequest = { startupUpdate = null },
                        title = { Text("Update Available") },
                        text = {
                            Text(
                                "Version ${update.versionName} is available. " +
                                    "Go to Settings → Updates to download and install it."
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                startupUpdate = null
                                screen = AppScreen.Settings
                            }) { Text("Open Settings") }
                        },
                        dismissButton = {
                            TextButton(onClick = { startupUpdate = null }) { Text("Later") }
                        },
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
        // fenlight://tmdb-auth is the redirect URI after TMDB browser approval.
        // The SetupViewModel's "completeTmdbAuth" button handles the token exchange.
    }
}
