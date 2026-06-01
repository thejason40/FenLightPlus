package com.fenlight.companion.ui.components

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember

/**
 * Shared helper for the "show play feedback, then clear it" pattern used by every screen
 * that sends a play command. Returns a [SnackbarHostState] to wire into a Scaffold, and
 * shows [message] (then invokes [onConsumed]) whenever it becomes non-null.
 */
@Composable
fun rememberPlayMessageSnackbar(message: String?, onConsumed: () -> Unit): SnackbarHostState {
    val host = remember { SnackbarHostState() }
    LaunchedEffect(message) {
        message?.let {
            host.showSnackbar(it)
            onConsumed()
        }
    }
    return host
}
