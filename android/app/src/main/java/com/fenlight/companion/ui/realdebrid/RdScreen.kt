package com.fenlight.companion.ui.realdebrid

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fenlight.companion.data.model.RdTorrent
import com.fenlight.companion.data.model.RdTorrentInfo
import com.fenlight.companion.ui.components.ErrorMessage
import com.fenlight.companion.ui.components.LoadingIndicator
import com.fenlight.companion.ui.components.rememberPlayMessageSnackbar

private fun parseRdTitle(filename: String): String {
    return filename
        .substringBeforeLast('.')   // strip extension if it's a single file
        .replace('.', ' ')
        .replace('_', ' ')
        .substringBefore('(').trim()
        .substringBefore('[').trim()
        .ifBlank { filename }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RdScreen(vm: RdViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbarHostState = rememberPlayMessageSnackbar(state.playMessage) { vm.clearPlayMessage() }

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

            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = vm::refresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                when (state.tab) {
                    RdTab.TORRENTS -> TorrentList(
                        torrents = state.torrents,
                        isLoadingMore = state.torrentIsLoadingMore,
                        hasMore = state.torrentHasMore,
                        onLoadMore = vm::loadMoreTorrents,
                        onTorrentClick = { vm.loadTorrentInfo(it.id) },
                    )
                    RdTab.DOWNLOADS -> DownloadList(
                        downloads = state.downloads,
                        isLoadingMore = state.downloadIsLoadingMore,
                        hasMore = state.downloadHasMore,
                        onLoadMore = vm::loadMoreDownloads,
                        onPlay = { vm.playDownload(it) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TorrentList(
    torrents: List<RdTorrent>,
    isLoadingMore: Boolean,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
    onTorrentClick: (RdTorrent) -> Unit,
) {
    val listState = rememberLazyListState()
    val shouldLoadMore by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= torrents.size - 5 && !isLoadingMore && hasMore
        }
    }
    LaunchedEffect(shouldLoadMore) { if (shouldLoadMore) onLoadMore() }

    if (torrents.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No torrents in your Real Debrid cloud", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(8.dp)) {
        items(torrents) { torrent ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                onClick = { onTorrentClick(torrent) },
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(parseRdTitle(torrent.filename), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text(torrent.filename, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(formatBytes(torrent.bytes), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        StatusChip(torrent.status, torrent.progress)
                    }
                }
            }
        }
        if (isLoadingMore) {
            item { LoadingIndicator(modifier = Modifier.padding(16.dp)) }
        }
    }
}

@Composable
private fun StatusChip(status: String, progress: Int) {
    val (dotColor, label) = when (status) {
        "downloaded"  -> Color(0xFF4A8060) to "Downloaded"
        "downloading" -> Color(0xFF5C7591) to "Downloading $progress%"
        "error"       -> Color(0xFFCC4444) to "Error"
        else          -> Color(0xFF4A5E70) to status.replaceFirstChar { it.uppercaseChar() }
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(dotColor))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
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
            title = { Text(parseRdTitle(torrent.filename), maxLines = 1) },
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
    isLoadingMore: Boolean,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
    onPlay: (com.fenlight.companion.data.model.RdDownload) -> Unit,
) {
    val listState = rememberLazyListState()
    val shouldLoadMore by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= downloads.size - 5 && !isLoadingMore && hasMore
        }
    }
    LaunchedEffect(shouldLoadMore) { if (shouldLoadMore) onLoadMore() }

    if (downloads.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No downloads found", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(8.dp)) {
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
        if (isLoadingMore) {
            item { LoadingIndicator(modifier = Modifier.padding(16.dp)) }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return "%.1f %s".format(bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}
