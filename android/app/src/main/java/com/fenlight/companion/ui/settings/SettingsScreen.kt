package com.fenlight.companion.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fenlight.companion.R
import com.fenlight.companion.ui.components.DropdownField
import com.fenlight.companion.ui.setup.SetupViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenSetup: () -> Unit,
    vm: SettingsViewModel = viewModel(),
    setupVm: SetupViewModel = viewModel(),
) {
    val state by vm.state.collectAsState()
    val setupState by setupVm.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(setupState.tmdbAuthUrl) {
        setupState.tmdbAuthUrl?.let { CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(it)) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {

            // ── About ─────────────────────────────────────────────────────────
            SettingsSection(title = "About") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Version", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "${state.currentVersion} (build ${state.currentVersionCode})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // ── Updates ───────────────────────────────────────────────────────
            SettingsSection(title = "Updates") {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Check on startup", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Automatically check for updates when the app opens",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = state.checkUpdateOnStartup, onCheckedChange = vm::toggleCheckUpdateOnStartup)
                }
                HorizontalDivider()
                val upd = state.update
                when {
                    upd.checking -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            Text("Checking for updates…")
                        }
                    }
                    upd.upToDate -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Text("App is up to date", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    upd.available -> {
                        val info = upd.updateInfo!!
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Version ${info.versionName} is available", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                            if (info.releaseNotes.isNotBlank()) {
                                Text(info.releaseNotes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (upd.downloading) {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                Text("Downloading…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                Button(onClick = { vm.downloadUpdate(info) }, modifier = Modifier.fillMaxWidth()) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Download & Install")
                                }
                            }
                        }
                    }
                    upd.error != null -> {
                        Text("Update check failed: ${upd.error}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Button(onClick = vm::checkForUpdate, modifier = Modifier.fillMaxWidth(), enabled = !upd.checking && !upd.downloading) {
                    Text("Check for Updates")
                }
            }

            // ── Accounts ──────────────────────────────────────────────────────
            SettingsSection(title = "Accounts") {

                // Kodi subsection
                ServiceSectionHeader(
                    icon = Icons.Default.Dns,
                    name = "Kodi",
                    subtitle = if (setupState.kodiConnected && setupState.kodiHost.isNotBlank())
                        "${setupState.kodiHost}:${setupState.kodiPort}"
                    else
                        "Not connected",
                )
                if (setupState.kodiConnected && setupState.kodiHost.isNotBlank()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Connected", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                            Text("${setupState.kodiHost}:${setupState.kodiPort}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    Text("Not connected.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                OutlinedButton(onClick = onOpenSetup, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Reconfigure Kodi")
                }

                HorizontalDivider()

                // TMDB subsection
                ServiceSectionHeader(
                    iconRes = R.drawable.icon_tmdb,
                    name = "TMDB",
                    subtitle = if (setupState.tmdbAuthed)
                        setupState.tmdbUsername.ifBlank { "Connected" }
                    else
                        "Required for personal lists",
                )
                when {
                    setupState.tmdbAuthed -> {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Signed in", color = MaterialTheme.colorScheme.primary)
                            TextButton(onClick = setupVm::signOutTmdb) { Text("Sign out") }
                        }
                    }
                    setupState.tmdbPolling -> {
                        Text("Complete sign-in in your browser, then tap below.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(onClick = setupVm::completeTmdbAuth, modifier = Modifier.fillMaxWidth()) {
                            Text("I've approved it — complete sign-in")
                        }
                        TextButton(onClick = setupVm::cancelTmdbAuth) { Text("Cancel") }
                    }
                    else -> {
                        if (setupState.tmdbError != null) Text(setupState.tmdbError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        Button(onClick = setupVm::startTmdbAuth, modifier = Modifier.fillMaxWidth(), enabled = !setupState.tmdbLoading) {
                            if (setupState.tmdbLoading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            else Text("Sign in to TMDB")
                        }
                    }
                }

                HorizontalDivider()

                // Trakt subsection
                ServiceSectionHeader(
                    iconRes = R.drawable.icon_trakt,
                    name = "Trakt",
                    subtitle = if (setupState.traktAuthed)
                        setupState.traktUsername.ifBlank { "Connected" }
                    else
                        "Continue Watching and personal lists",
                )
                when {
                    setupState.traktAuthed -> {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Signed in", color = MaterialTheme.colorScheme.primary)
                            TextButton(onClick = setupVm::signOutTrakt) { Text("Sign out") }
                        }
                    }
                    setupState.traktPolling -> {
                        Text("Visit trakt.tv/activate and enter this code:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(setupState.traktUserCode, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.CenterHorizontally))
                        OutlinedButton(
                            onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://trakt.tv/activate/${setupState.traktUserCode}"))) },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Open trakt.tv/activate") }
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        TextButton(onClick = setupVm::cancelTraktAuth) { Text("Cancel") }
                    }
                    else -> {
                        if (setupState.traktError != null) Text(setupState.traktError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        Button(onClick = setupVm::startTraktAuth, modifier = Modifier.fillMaxWidth(), enabled = !setupState.traktLoading) {
                            if (setupState.traktLoading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            else Text("Sign in to Trakt")
                        }
                    }
                }

                HorizontalDivider()

                // Real Debrid subsection
                ServiceSectionHeader(
                    iconRes = R.drawable.icon_realdebrid,
                    name = "Real Debrid",
                    subtitle = if (setupState.rdAuthed)
                        setupState.rdUsername.ifBlank { "Connected" }
                    else
                        "Browse your cloud files",
                )
                when {
                    setupState.rdAuthed -> {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Signed in", color = MaterialTheme.colorScheme.primary)
                            TextButton(onClick = setupVm::signOutRd) { Text("Sign out") }
                        }
                    }
                    setupState.rdPolling -> {
                        Text("Visit real-debrid.com/device and enter this code:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(setupState.rdUserCode, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.CenterHorizontally))
                        OutlinedButton(
                            onClick = {
                                val url = setupState.rdDirectVerificationUrl.ifBlank { "https://real-debrid.com/device" }
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(if (setupState.rdDirectVerificationUrl.isNotBlank()) "Open real-debrid.com/device (code pre-filled)" else "Open real-debrid.com/device") }
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        TextButton(onClick = setupVm::cancelRdAuth) { Text("Cancel") }
                    }
                    else -> {
                        if (setupState.rdError != null) Text(setupState.rdError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        Button(onClick = setupVm::startRdAuth, modifier = Modifier.fillMaxWidth(), enabled = !setupState.rdLoading) {
                            if (setupState.rdLoading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            else Text("Sign in to Real Debrid")
                        }
                    }
                }
            }

            // ── Content ───────────────────────────────────────────────────────
            SettingsSection(title = "Content") {
                Text(
                    "Region filters popular, trending, now playing and upcoming results to your region.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                DropdownField(label = "Region", options = tmdbRegions, selected = state.region, onSelect = vm::setRegion)
                HorizontalDivider()
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Exclude adult content", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Hide titles flagged as adult in TMDB results",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = state.excludeAdult, onCheckedChange = vm::toggleExcludeAdult)
                }
            }
        }
    }
}

@Composable
private fun ServiceSectionHeader(iconRes: Int, name: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Image(painter = painterResource(iconRes), contentDescription = null, modifier = Modifier.size(28.dp))
        Column {
            Text(name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ServiceSectionHeader(icon: androidx.compose.ui.graphics.vector.ImageVector, name: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Column {
            Text(name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private val tmdbRegions: List<Pair<String, String>> = buildList {
    add("" to "Any Region")
    addAll(
        listOf(
    "AD" to "Andorra", "AE" to "United Arab Emirates", "AG" to "Antigua and Barbuda",
    "AL" to "Albania", "AO" to "Angola", "AR" to "Argentina", "AT" to "Austria",
    "AU" to "Australia", "AZ" to "Azerbaijan", "BA" to "Bosnia and Herzegovina",
    "BB" to "Barbados", "BE" to "Belgium", "BF" to "Burkina Faso", "BG" to "Bulgaria",
    "BH" to "Bahrain", "BM" to "Bermuda", "BO" to "Bolivia", "BR" to "Brazil",
    "BS" to "Bahamas", "BT" to "Bhutan", "BW" to "Botswana", "BY" to "Belarus",
    "BZ" to "Belize", "CA" to "Canada", "CD" to "Congo (Democratic Republic)",
    "CH" to "Switzerland", "CI" to "Côte d'Ivoire", "CL" to "Chile", "CM" to "Cameroon",
    "CO" to "Colombia", "CR" to "Costa Rica", "CV" to "Cape Verde", "CY" to "Cyprus",
    "CZ" to "Czech Republic", "DE" to "Germany", "DK" to "Denmark", "DO" to "Dominican Republic",
    "DZ" to "Algeria", "EC" to "Ecuador", "EE" to "Estonia", "EG" to "Egypt",
    "ES" to "Spain", "FI" to "Finland", "FJ" to "Fiji", "FR" to "France",
    "GB" to "United Kingdom", "GH" to "Ghana", "GM" to "Gambia", "GQ" to "Equatorial Guinea",
    "GR" to "Greece", "GT" to "Guatemala", "GY" to "Guyana", "HK" to "Hong Kong",
    "HN" to "Honduras", "HR" to "Croatia", "HU" to "Hungary", "ID" to "Indonesia",
    "IE" to "Ireland", "IL" to "Israel", "IN" to "India", "IQ" to "Iraq",
    "IS" to "Iceland", "IT" to "Italy", "JM" to "Jamaica", "JO" to "Jordan",
    "JP" to "Japan", "KE" to "Kenya", "KR" to "South Korea", "KW" to "Kuwait",
    "LB" to "Lebanon", "LC" to "Saint Lucia", "LI" to "Liechtenstein", "LT" to "Lithuania",
    "LU" to "Luxembourg", "LV" to "Latvia", "LY" to "Libya", "MA" to "Morocco",
    "MC" to "Monaco", "MD" to "Moldova", "ME" to "Montenegro", "MK" to "North Macedonia",
    "ML" to "Mali", "MT" to "Malta", "MU" to "Mauritius", "MW" to "Malawi",
    "MX" to "Mexico", "MY" to "Malaysia", "MZ" to "Mozambique", "NE" to "Niger",
    "NG" to "Nigeria", "NI" to "Nicaragua", "NL" to "Netherlands", "NO" to "Norway",
    "NZ" to "New Zealand", "OM" to "Oman", "PA" to "Panama", "PE" to "Peru",
    "PG" to "Papua New Guinea", "PH" to "Philippines", "PK" to "Pakistan", "PL" to "Poland",
    "PS" to "Palestinian Territory", "PT" to "Portugal", "PY" to "Paraguay", "QA" to "Qatar",
    "RO" to "Romania", "RS" to "Serbia", "RU" to "Russia", "SA" to "Saudi Arabia",
    "SC" to "Seychelles", "SE" to "Sweden", "SG" to "Singapore", "SI" to "Slovenia",
    "SK" to "Slovakia", "SL" to "Sierra Leone", "SM" to "San Marino", "SN" to "Senegal",
    "SV" to "El Salvador", "TD" to "Chad", "TH" to "Thailand", "TN" to "Tunisia",
    "TR" to "Turkey", "TT" to "Trinidad and Tobago", "TW" to "Taiwan", "TZ" to "Tanzania",
    "UA" to "Ukraine", "UG" to "Uganda", "US" to "United States", "UY" to "Uruguay",
    "VA" to "Vatican City", "VE" to "Venezuela", "XK" to "Kosovo", "YE" to "Yemen",
    "ZA" to "South Africa", "ZM" to "Zambia", "ZW" to "Zimbabwe",
        ).sortedBy { it.second }
    )
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
        }
    }
}
