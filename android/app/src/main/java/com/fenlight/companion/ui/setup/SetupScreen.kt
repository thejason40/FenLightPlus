package com.fenlight.companion.ui.setup

import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SetupScreen(
    onSetupComplete: () -> Unit,
    vm: SetupViewModel = viewModel(),
) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current

    // Kodi discovery sheet
    if (state.showDiscoverySheet) {
        KodiDiscoverySheet(
            discovered = state.kodiDiscovered,
            isScanning = state.kodiScanning,
            onSelect = vm::selectDiscoveredKodi,
            onDismiss = vm::dismissDiscoverySheet,
        )
    }

    // Handle TMDB OAuth step change
    LaunchedEffect(state.tmdbAuthUrl) {
        val url = state.tmdbAuthUrl
        if (url != null) {
            CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(url))
        }
    }

    LaunchedEffect(state.setupComplete) {
        if (state.setupComplete) onSetupComplete()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text(
            text = "FenLight+ Companion",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Connect to Kodi and sign in to your media services.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // ── Step 1: Kodi ──────────────────────────────────────────────────────
        SetupCard(
            title = "1. Kodi Connection",
            isDone = state.kodiConnected,
        ) {
            OutlinedTextField(
                value = state.kodiHost,
                onValueChange = vm::onKodiHostChange,
                label = { Text("Kodi IP address") },
                placeholder = { Text("192.168.1.100") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.kodiPort,
                onValueChange = vm::onKodiPortChange,
                label = { Text("Port") },
                placeholder = { Text("8080") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.kodiUser,
                onValueChange = vm::onKodiUserChange,
                label = { Text("Username (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.kodiPass,
                onValueChange = vm::onKodiPassChange,
                label = { Text("Password (optional)") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
            if (state.kodiError != null) {
                Text(
                    text = state.kodiError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = vm::startKodiScan,
                    modifier = Modifier.weight(1f),
                    enabled = !state.kodiScanning && !state.kodiTesting,
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Scan")
                }
                Button(
                    onClick = vm::testKodiConnection,
                    modifier = Modifier.weight(1f),
                    enabled = !state.kodiTesting && !state.kodiScanning && state.kodiHost.isNotBlank(),
                ) {
                    if (state.kodiTesting) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text(if (state.kodiConnected) "Connected ✓" else "Test Connection")
                    }
                }
            }
        }

        // ── Step 2: TMDB ──────────────────────────────────────────────────────
        SetupCard(
            title = "2. TMDB Account (Optional)",
            subtitle = "Required only for personal lists",
            isDone = state.tmdbAuthed,
            enabled = state.kodiConnected,
        ) {
            when {
                state.tmdbAuthed -> {
                    Text("Signed in to TMDB", color = MaterialTheme.colorScheme.primary)
                    TextButton(onClick = vm::signOutTmdb) { Text("Sign out") }
                }
                state.tmdbPolling -> {
                    Text("Complete sign-in in your browser, then come back here.")
                    Button(onClick = vm::completeTmdbAuth, modifier = Modifier.fillMaxWidth()) {
                        Text("I've approved it — complete sign-in")
                    }
                    TextButton(onClick = vm::cancelTmdbAuth) { Text("Cancel") }
                }
                else -> {
                    Text(
                        "Tap below to open TMDB in your browser and authorise this app.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (state.tmdbError != null) {
                        Text(state.tmdbError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    Button(
                        onClick = vm::startTmdbAuth,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.tmdbLoading,
                    ) {
                        if (state.tmdbLoading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        else Text("Log in to TMDB")
                    }
                }
            }
        }

        // ── Step 3: Trakt ─────────────────────────────────────────────────────
        SetupCard(
            title = "3. Trakt (Optional)",
            subtitle = "Next episodes and personal lists",
            isDone = state.traktAuthed,
            enabled = state.kodiConnected,
        ) {
            when {
                state.traktAuthed -> {
                    Text("Signed in to Trakt", color = MaterialTheme.colorScheme.primary)
                    TextButton(onClick = vm::signOutTrakt) { Text("Sign out") }
                }
                state.traktPolling -> {
                    Text("Visit trakt.tv/activate and enter this code:", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = state.traktUserCode,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://trakt.tv/activate"))
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Open trakt.tv/activate") }
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    TextButton(onClick = vm::cancelTraktAuth) { Text("Cancel") }
                }
                else -> {
                    if (state.traktError != null) {
                        Text(state.traktError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = vm::startTraktAuth, enabled = !state.traktLoading, modifier = Modifier.weight(1f)) {
                            if (state.traktLoading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            else Text("Log in to Trakt")
                        }
                    }
                }
            }
        }

        // ── Step 4: Real Debrid ───────────────────────────────────────────────
        SetupCard(
            title = "4. Real Debrid (Optional)",
            subtitle = "Browse your cloud files directly",
            isDone = state.rdAuthed,
            enabled = state.kodiConnected,
        ) {
            when {
                state.rdAuthed -> {
                    Text("Signed in to Real Debrid", color = MaterialTheme.colorScheme.primary)
                    TextButton(onClick = vm::signOutRd) { Text("Sign out") }
                }
                state.rdPolling -> {
                    Text("Visit real-debrid.com/device and enter this code:", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = state.rdUserCode,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://real-debrid.com/device"))
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Open real-debrid.com/device") }
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    TextButton(onClick = vm::cancelRdAuth) { Text("Cancel") }
                }
                else -> {
                    if (state.rdError != null) {
                        Text(state.rdError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    Button(onClick = vm::startRdAuth, enabled = !state.rdLoading, modifier = Modifier.fillMaxWidth()) {
                        if (state.rdLoading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        else Text("Log in to Real Debrid")
                    }
                }
            }
        }

        // ── Done ──────────────────────────────────────────────────────────────
        if (state.kodiConnected) {
            Button(
                onClick = onSetupComplete,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Continue to App")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KodiDiscoverySheet(
    discovered: List<DiscoveredKodi>,
    isScanning: Boolean,
    onSelect: (DiscoveredKodi) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Kodi on your network",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            if (isScanning) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text("Scanning…", style = MaterialTheme.typography.bodyMedium)
                }
            }

            if (discovered.isEmpty() && !isScanning) {
                Text(
                    "No Kodi instances found. Make sure Kodi is running and that Settings → Services → Control → Allow remote control via HTTP is enabled.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            discovered.forEach { kodi ->
                ListItem(
                    headlineContent = { Text(kodi.name) },
                    supportingContent = {
                        Text(
                            "${kodi.host}:${kodi.port}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    },
                    leadingContent = {
                        Icon(Icons.Default.Tv, contentDescription = null)
                    },
                    trailingContent = {
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(kodi) },
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun SetupCard(
    title: String,
    isDone: Boolean,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isDone)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (subtitle != null) {
                        Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (isDone) Icon(Icons.Default.CheckCircle, contentDescription = "Done", tint = MaterialTheme.colorScheme.primary)
            }
            if (enabled) content()
            else Text("Complete the previous step first.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
