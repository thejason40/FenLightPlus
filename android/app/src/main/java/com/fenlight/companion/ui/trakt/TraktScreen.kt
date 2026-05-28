package com.fenlight.companion.ui.trakt

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fenlight.companion.data.model.TraktList
import com.fenlight.companion.data.model.TraktListItem
import com.fenlight.companion.data.model.TraktWatchedShow
import com.fenlight.companion.ui.components.ErrorMessage
import com.fenlight.companion.ui.components.LoadingIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TraktScreen(vm: TraktViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.playMessage) {
        state.playMessage?.let { snackbarHostState.showSnackbar(it); vm.clearPlayMessage() }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.listItems.isNotEmpty() || state.selectedListName.isNotEmpty()) {
                // Show list items with pagination
                ListItemsScreen(
                    listName = state.selectedListName,
                    items = state.listItems,
                    isLoading = state.isLoading,
                    isRefreshing = state.isRefreshing,
                    isLoadingMore = state.listItemIsLoadingMore,
                    hasMore = state.listItemHasMore,
                    onLoadMore = vm::loadMoreListItems,
                    onRefresh = vm::refresh,
                    onBack = vm::clearListItems,
                    onPlayMovie = vm::playListMovie,
                )
                return@Column
            }

            TabRow(selectedTabIndex = state.tab.ordinal) {
                listOf("Continue Watching", "My Lists", "Liked Lists").forEachIndexed { i, label ->
                    Tab(
                        selected = state.tab.ordinal == i,
                        onClick = { vm.selectTab(TraktTab.values()[i]) },
                        text = { Text(label) },
                    )
                }
            }

            if (state.isLoading) {
                LoadingIndicator(modifier = Modifier.padding(32.dp))
                return@Column
            }

            state.error?.let {
                ErrorMessage(it, onRetry = { vm.selectTab(state.tab) }, modifier = Modifier.padding(16.dp))
                return@Column
            }

            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = vm::refresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                when (state.tab) {
                    TraktTab.CONTINUE_WATCHING -> ContinueWatchingList(state.watchedShows, vm::playNextEpisode)
                    TraktTab.MY_LISTS -> TraktListList(state.myLists) { list ->
                        vm.loadListItems(list.slug, list.name, "me")
                    }
                    TraktTab.LIKED_LISTS -> TraktListList(state.likedLists) { list ->
                        val user = list.user?.username ?: "me"
                        vm.loadListItems(list.slug, list.name, user)
                    }
                }
            }
        }
    }
}

@Composable
private fun ContinueWatchingList(
    shows: List<TraktWatchedShow>,
    onPlay: (TraktWatchedShow) -> Unit,
) {
    if (shows.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No recently watched shows", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(8.dp)) {
        items(shows) { watched ->
            val next = watched.nextEpisode()
            ListItem(
                headlineContent = { Text(watched.show.title, fontWeight = FontWeight.SemiBold) },
                supportingContent = {
                    if (next != null) {
                        Text("Next: S${next.first}E${next.second}", style = MaterialTheme.typography.bodySmall)
                    } else {
                        Text("Watched ${watched.plays} episode${if (watched.plays != 1) "s" else ""}", style = MaterialTheme.typography.bodySmall)
                    }
                },
                trailingContent = {
                    if (next != null) {
                        IconButton(onClick = { onPlay(watched) }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Play next", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun TraktListList(
    lists: List<TraktList>,
    onListClick: (TraktList) -> Unit,
) {
    if (lists.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No lists found", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(8.dp)) {
        items(lists) { list ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                onClick = { onListClick(list) },
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(list.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    if (list.description.isNotBlank()) {
                        Text(list.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                    }
                    Text("${list.itemCount} items · ♥ ${list.likes}", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ListItemsScreen(
    listName: String,
    items: List<TraktListItem>,
    isLoading: Boolean,
    isRefreshing: Boolean,
    isLoadingMore: Boolean,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
    onRefresh: () -> Unit,
    onBack: () -> Unit,
    onPlayMovie: (TraktListItem) -> Unit,
) {
    val listState = rememberLazyListState()
    val shouldLoadMore by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= items.size - 5 && !isLoadingMore && hasMore
        }
    }
    LaunchedEffect(shouldLoadMore) { if (shouldLoadMore) onLoadMore() }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(listName) },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
            },
        )
        if (isLoading && items.isEmpty()) {
            LoadingIndicator(modifier = Modifier.padding(32.dp))
            return@Column
        }
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(8.dp)) {
                items(items) { item ->
                    val title = item.movie?.title ?: item.show?.title ?: "Unknown"
                    val year = (item.movie?.year ?: item.show?.year)?.toString() ?: ""
                    ListItem(
                        headlineContent = { Text(title) },
                        supportingContent = { Text(year + if (item.type.isNotBlank()) " · ${item.type}" else "") },
                        trailingContent = {
                            if (item.type == "movie" && item.movie?.ids?.tmdb != null) {
                                IconButton(onClick = { onPlayMovie(item) }) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        },
                    )
                    HorizontalDivider()
                }
                if (isLoadingMore) {
                    item {
                        LoadingIndicator(modifier = Modifier.padding(16.dp))
                    }
                }
            }
        }
    }
}
