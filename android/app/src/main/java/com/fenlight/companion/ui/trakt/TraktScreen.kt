package com.fenlight.companion.ui.trakt

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fenlight.companion.data.model.TraktList
import com.fenlight.companion.data.model.TraktListItem
import com.fenlight.companion.data.model.TraktShowProgress
import com.fenlight.companion.data.model.TraktWatchedShow
import com.fenlight.companion.ui.components.ErrorMessage
import com.fenlight.companion.ui.components.ListManagementSheet
import com.fenlight.companion.ui.components.LoadingIndicator
import com.fenlight.companion.ui.components.rememberPlayMessageSnackbar

private fun placeholderColor(title: String): Color {
    val colors = listOf(
        Color(0xFF1C2D3E), Color(0xFF2E1C1C), Color(0xFF1E2040),
        Color(0xFF1C2E28), Color(0xFF2A1C2E), Color(0xFF1A2C2C),
    )
    return colors[Math.abs(title.hashCode()) % colors.size]
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TraktScreen(vm: TraktViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbarHostState = rememberPlayMessageSnackbar(state.playMessage) { vm.clearPlayMessage() }

    // Create list dialog
    if (state.showCreateListDialog) {
        var listName by remember { mutableStateOf("") }
        var listDesc by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = vm::dismissCreateListDialog,
            title = { Text("New Trakt List") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = listName,
                        onValueChange = { listName = it },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = listDesc,
                        onValueChange = { listDesc = it },
                        label = { Text("Description (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { vm.createTraktList(listName, listDesc) }, enabled = listName.isNotBlank()) {
                    Text("Create")
                }
            },
            dismissButton = { TextButton(onClick = vm::dismissCreateListDialog) { Text("Cancel") } },
        )
    }

    // Delete confirmation dialog
    state.listToDelete?.let { list ->
        AlertDialog(
            onDismissRequest = vm::cancelDeleteList,
            title = { Text("Delete \"${list.name}\"?") },
            text = { Text("This will permanently delete the list and all its items. This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = { vm.deleteTraktList(list.slug) },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = vm::cancelDeleteList) { Text("Cancel") } },
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (state.tab == TraktTab.MY_LISTS && state.listItems.isEmpty() && state.selectedListName.isEmpty()) {
                FloatingActionButton(onClick = vm::showCreateListDialog) {
                    Icon(Icons.Default.Add, contentDescription = "Create list")
                }
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.listItems.isNotEmpty() || state.selectedListName.isNotEmpty()) {
                // Show list items with pagination
                ListItemsScreen(
                    listName = state.selectedListName,
                    listSlug = state.selectedListSlug,
                    isMine = state.selectedListUser == "me",
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

            ScrollableTabRow(selectedTabIndex = state.tab.ordinal, edgePadding = 0.dp) {
                listOf("Continue Watching", "My Lists", "Liked Lists", "Watchlist").forEachIndexed { i, label ->
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
                    TraktTab.CONTINUE_WATCHING -> ContinueWatchingList(state.watchedShows, state.showProgressMap, vm::playNextEpisode)
                    TraktTab.MY_LISTS -> TraktListList(
                        lists = state.myLists,
                        onListClick = { list -> vm.loadListItems(list.slug, list.name, "me") },
                        onListLongClick = { list -> vm.confirmDeleteList(list) },
                    )
                    TraktTab.LIKED_LISTS -> TraktListList(
                        lists = state.likedLists,
                        onListClick = { list ->
                            val user = list.user?.username ?: "me"
                            vm.loadListItems(list.slug, list.name, user)
                        },
                    )
                    TraktTab.WATCHLIST -> WatchlistTab(state.watchlistMovies, state.watchlistShows, vm::playListMovie)
                }
            }
        }
    }
}

@Composable
private fun ContinueWatchingList(
    shows: List<TraktWatchedShow>,
    progressMap: Map<String, TraktShowProgress>,
    onPlay: (TraktWatchedShow) -> Unit,
) {
    if (shows.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No shows in progress", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(8.dp)) {
        items(shows) { watched ->
            val slug = watched.show.ids.slug ?: ""
            val prog = progressMap[slug]
            val nextEp = prog?.nextEpisode
            val initials = watched.show.title
                .split(' ')
                .take(2)
                .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                .joinToString("")
            val progressFraction = prog?.let {
                if (it.aired > 0) (it.completed.toFloat() / it.aired).coerceIn(0f, 1f) else 0f
            } ?: 0f

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(10.dp),
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Box(
                        modifier = Modifier
                            .width(46.dp)
                            .height(68.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(placeholderColor(watched.show.title)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f),
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = watched.show.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (nextEp != null) {
                            val label = if (!nextEp.title.isNullOrBlank())
                                "Next · S${nextEp.season}E${nextEp.number} · ${nextEp.title}"
                            else
                                "Next · S${nextEp.season}E${nextEp.number}"
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (prog != null) {
                            Text(
                                text = "${prog.completed}/${prog.aired} episodes",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            )
                        }
                        LinearProgressIndicator(
                            progress = { progressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .clip(RoundedCornerShape(1.dp)),
                        )
                    }

                    FilledIconButton(
                        onClick = { onPlay(watched) },
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TraktListList(
    lists: List<TraktList>,
    onListClick: (TraktList) -> Unit,
    onListLongClick: ((TraktList) -> Unit)? = null,
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .combinedClickable(
                        onClick = { onListClick(list) },
                        onLongClick = onListLongClick?.let { { it(list) } },
                    ),
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

@Composable
private fun WatchlistTab(
    movies: List<TraktListItem>,
    shows: List<TraktListItem>,
    onPlayMovie: (TraktListItem) -> Unit,
) {
    if (movies.isEmpty() && shows.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Your watchlist is empty", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
        if (movies.isNotEmpty()) {
            item {
                Text(
                    "Movies",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            items(movies) { item ->
                val movie = item.movie ?: return@items
                ListItem(
                    headlineContent = { Text(movie.title) },
                    supportingContent = { movie.year?.let { Text(it.toString()) } },
                    trailingContent = {
                        if (movie.ids.tmdb != null) {
                            IconButton(onClick = { onPlayMovie(item) }) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    },
                )
                HorizontalDivider()
            }
        }
        if (shows.isNotEmpty()) {
            item {
                Text(
                    "TV Shows",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            items(shows) { item ->
                val show = item.show ?: return@items
                ListItem(
                    headlineContent = { Text(show.title) },
                    supportingContent = { show.year?.let { Text(it.toString()) } },
                )
                HorizontalDivider()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun ListItemsScreen(
    listName: String,
    listSlug: String,
    isMine: Boolean,
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

    var selectedItem by remember { mutableStateOf<TraktListItem?>(null) }

    selectedItem?.let { item ->
        val tmdbId = item.movie?.ids?.tmdb ?: item.show?.ids?.tmdb
        val mediaType = if (item.type == "show") "show" else "movie"
        val title = item.movie?.title ?: item.show?.title ?: ""
        if (tmdbId != null) {
            ListManagementSheet(
                mediaId = tmdbId,
                mediaType = mediaType,
                title = title,
                posterUrl = null,
                currentTraktListSlug = if (isMine) listSlug else null,
                currentTraktListName = if (isMine) listName else null,
                onDismiss = { selectedItem = null },
            )
        } else {
            selectedItem = null
        }
    }

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
                        modifier = Modifier.combinedClickable(
                            onClick = {},
                            onLongClick = { selectedItem = item },
                        ),
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
