package com.fenlight.companion.ui.setup

import android.content.Intent
import android.net.Uri
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
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.fenlight.companion.R

@Composable
fun SetupScreen(
    onSetupComplete: () -> Unit,
    vm: SetupViewModel = viewModel(),
) {
    val state by vm.state.collectAsState()

    if (state.showDiscoverySheet) {
        KodiDiscoverySheet(
            discovered = state.kodiDiscovered,
            isScanning = state.kodiScanning,
            onSelect = vm::selectDiscoveredKodi,
            onDismiss = vm::dismissDiscoverySheet,
        )
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
            text = "Connect to your Kodi instance to get started. Sign in to media services afterwards via Settings.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SetupCard(title = "Kodi Connection", isDone = state.kodiConnected) {
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

        if (state.kodiConnected) {
            Button(onClick = onSetupComplete, modifier = Modifier.fillMaxWidth()) {
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
            Text("Kodi on your network", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (isScanning) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
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
                    supportingContent = { Text("${kodi.host}:${kodi.port}", style = MaterialTheme.typography.bodySmall) },
                    leadingContent = { Icon(Icons.Default.Tv, contentDescription = null) },
                    trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().clickable { onSelect(kodi) },
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
    @DrawableRes iconRes: Int? = null,
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
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (iconRes != null) {
                    Image(painter = painterResource(iconRes), contentDescription = null, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.width(10.dp))
                }
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
