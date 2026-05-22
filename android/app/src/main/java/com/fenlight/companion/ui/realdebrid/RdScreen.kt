package com.fenlight.companion.ui.realdebrid

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fenlight.companion.data.model.RdTorrent
import com.fenlight.companion.data.model.RdTorrentInfo
import com.fenlight.companion.ui.components.ErrorMessage
import com.fenlight.companion.ui.components.LoadingIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RdScreen(vm: RdViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.playMessage) {
        state.playMessage?.let { snackbarHostState.showSnackbar(it); vm.clearPlayMessage() }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            val torrent = state.selectedTorrent
            if (torrent != null) {
                TorrentFileList(torrent, onBack = vm::clearSelectedTorrent, onPlay = { vm.playTorrentFile(torrent, it) })
                return@Column
            }

            TabRow(selectedTabIndex = state.tab.ordinal) {
                listOf("Cloud Torrents", "Downloads").forEachIndexed { i, label ->
                    Tab(
                        selected = state.tab.ordinal == i,
                        onClick = { vm.selectTab(RdTab.values()[i]) },
                        text = { Text(label) },
                    )
                }
            }

            if (state.isLoading) { LoadingIndicator(modifier = Modifier.padding(32.dp)); return@Column }
            state.error?.let { ErrorMessage(it, onRetry = { vm.selectTab(state.tab) }, modifier = Modifier.padding(16.dp)); return@Column }

            when (state.tab) {
                RdTab.TORRENTS -> TorrentList(state.torrents, onTorrentClick = { vm.loadTorrentInfo(it.id) })
                RdTab.DOWNLOADS -> DownloadList(state.downloads, onPlay = { vm.playDownload(it) })
            }
        }
    }
}

@Composable
private fun TorrentList(torrents: List<RdTorrent>, onTorrentClick: (RdTorrent) -> Unit) {
    if (torrents.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No torrents in your Real Debrid cloud", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(8.dp)) {
        items(torrents) { torrent ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                onClick = { onTorrentClick(torrent) },
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(torrent.filename, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 2)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(formatBytes(torrent.bytes), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        StatusChip(torrent.status, torrent.progress)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(status: String, progress: Int) {
    val color = when (status) {
        "downloaded" -> MaterialTheme.colorScheme.primary
        "downloading" -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }
    Text(
        text = if (status == "downloading") "$status $progress%" else status,
        style = MaterialTheme.typography.labelSmall,
        color = color,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TorrentFileList(
    torrent: RdTorrentInfo,
    onBack: () -> Unit,
    onPlay: (com.fenlight.companion.data.model.RdFile) -> Unit,
) {
    val videoExtensions = setOf("mkv", "mp4", "avi", "mov", "wmv", "m4v", "ts", "flv")
    val files = torrent.files.filter {
        it.selected == 1 && it.path.substringAfterLast('.').lowercase() in videoExtensions
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(torrent.filename, maxLines = 1) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } },
        )
        if (files.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No playable video files", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return
        }
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(8.dp)) {
            items(files) { file ->
                ListItem(
                    headlineContent = { Text(file.path.substringAfterLast('/'), maxLines = 2) },
                    supportingContent = { Text(formatBytes(file.bytes), style = MaterialTheme.typography.labelSmall) },
                    trailingContent = {
                        IconButton(onClick = { onPlay(file) }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun DownloadList(
    downloads: List<com.fenlight.companion.data.model.RdDownload>,
    onPlay: (com.fenlight.companion.data.model.RdDownload) -> Unit,
) {
    if (downloads.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No downloads found", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(8.dp)) {
        items(downloads) { dl ->
            ListItem(
                headlineContent = { Text(dl.filename, maxLines = 2) },
                supportingContent = { Text(formatBytes(dl.filesize), style = MaterialTheme.typography.labelSmall) },
                trailingContent = {
                    IconButton(onClick = { onPlay(dl) }) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = MaterialTheme.colorScheme.primary)
                    }
                },
            )
            HorizontalDivider()
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return "%.1f %s".format(bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}
