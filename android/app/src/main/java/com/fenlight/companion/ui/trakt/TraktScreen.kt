package com.fenlight.companion.ui.trakt

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.fenlight.companion.FenLightApp
import com.fenlight.companion.data.model.TraktHistoryEntry
import com.fenlight.companion.data.model.TraktList
import com.fenlight.companion.data.model.TraktListItem
import com.fenlight.companion.data.model.TraktShowProgress
import com.fenlight.companion.data.model.TraktWatchedShow
import com.fenlight.companion.ui.components.ErrorMessage
import com.fenlight.companion.ui.components.ListManagementSheet
import com.fenlight.companion.ui.components.LoadingIndicator
import com.fenlight.companion.ui.components.PaginatedGrid
import com.fenlight.companion.ui.components.PaginatedItem
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
fun TraktScreen(
    onMovieClick: (Int) -> Unit = {},
    onShowClick: (Int) -> Unit = {},
    onEpisodeClick: (showId: Int, season: Int, episode: Int) -> Unit = { _, _, _ -> },
    onGoToSettings: () -> Unit = {},
    vm: TraktViewModel = viewModel(),
) {
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

    // Like confirmation (long-press in list search results)
    state.listToLike?.let { list ->
        AlertDialog(
            onDismissRequest = vm::cancelLikeList,
            title = { Text("Like \"${list.name}\"?") },
            text = { Text("Liked lists appear in your Liked Lists tab.") },
            confirmButton = { TextButton(onClick = { vm.likeList(list) }) { Text("Like") } },
            dismissButton = { TextButton(onClick = vm::cancelLikeList) { Text("Cancel") } },
        )
    }

    // Unlike confirmation (long-press in Liked Lists)
    state.listToUnlike?.let { list ->
        AlertDialog(
            onDismissRequest = vm::cancelUnlikeList,
            title = { Text("Unlike \"${list.name}\"?") },
            text = { Text("The list will be removed from your Liked Lists.") },
            confirmButton = {
                TextButton(
                    onClick = { vm.unlikeList(list) },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Unlike") }
            },
            dismissButton = { TextButton(onClick = vm::cancelUnlikeList) { Text("Cancel") } },
        )
    }

    val drilledIn = state.listItems.isNotEmpty() || state.selectedListName.isNotEmpty()
    val searching = state.listSearchActive && !drilledIn

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when {
                            drilledIn -> state.selectedListName
                            searching -> "Search Lists"
                            else -> "Trakt"
                        }
                    )
                },
                navigationIcon = {
                    if (drilledIn) {
                        IconButton(onClick = vm::clearListItems) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                    } else if (searching) {
                        IconButton(onClick = vm::closeListSearch) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                    }
                },
                actions = {
                    if (!drilledIn && !searching) {
                        IconButton(onClick = vm::openListSearch) {
                            Icon(Icons.Default.Search, contentDescription = "Search public lists")
                        }
                        if (state.tab == TraktTab.MY_LISTS) {
                            IconButton(onClick = vm::showCreateListDialog) {
                                Icon(Icons.Default.Add, contentDescription = "Create list")
                            }
                        }
                        IconButton(onClick = onGoToSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (drilledIn) {
                // Show list items with pagination
                ListItemsScreen(
                    listName = state.selectedListName,
                    listSlug = state.selectedListSlug,
                    isMine = state.selectedListUser == "me",
                    enrichedItems = state.listItemsEnriched,
                    typeMap = state.listItemTypeMap,
                    isLoading = state.isLoading,
                    isRefreshing = state.isRefreshing,
                    isLoadingMore = state.listItemIsLoadingMore,
                    hasMore = state.listItemHasMore,
                    onLoadMore = vm::loadMoreListItems,
                    onRefresh = vm::refresh,
                    onMovieClick = onMovieClick,
                    onShowClick = onShowClick,
                )
                return@Column
            }

            if (searching) {
                ListSearchView(
                    query = state.listSearchQuery,
                    results = state.listSearchResults,
                    isSearching = state.isSearchingLists,
                    searchPerformed = state.listSearchPerformed,
                    hasMore = state.listSearchHasMore,
                    isLoadingMore = state.listSearchIsLoadingMore,
                    onQueryChange = vm::setListSearchQuery,
                    onSearch = vm::searchLists,
                    onLoadMore = vm::loadMoreListSearch,
                    onListClick = { list ->
                        val user = list.user?.pathId ?: "me"
                        vm.loadListItems(list.slug, list.name, user)
                    },
                    onListLongClick = vm::confirmLikeList,
                )
                return@Column
            }

            ScrollableTabRow(selectedTabIndex = state.tab.ordinal, edgePadding = 0.dp) {
                listOf("Continue Watching", "My Lists", "Liked Lists", "Watchlist", "Recent").forEachIndexed { i, label ->
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
                    TraktTab.CONTINUE_WATCHING -> ContinueWatchingList(state.watchedShows, state.showProgressMap, state.continueWatchingPosters, vm::playNextEpisode, onShowClick)
                    TraktTab.MY_LISTS -> TraktListList(
                        lists = state.myLists,
                        onListClick = { list -> vm.loadListItems(list.slug, list.name, "me") },
                        onListLongClick = { list -> vm.confirmDeleteList(list) },
                    )
                    TraktTab.LIKED_LISTS -> TraktListList(
                        lists = state.likedLists,
                        onListClick = { list ->
                            val user = list.user?.pathId ?: "me"
                            vm.loadListItems(list.slug, list.name, user)
                        },
                        onListLongClick = { list -> vm.confirmUnlikeList(list) },
                        showOwner = true,
                    )
                    TraktTab.WATCHLIST -> WatchlistTab(state.watchlistMovies, state.watchlistShows, vm::playListMovie, onMovieClick, onShowClick)
                    TraktTab.RECENT -> RecentTab(
                        history = state.recentHistory,
                        hasMore = state.recentHistoryHasMore,
                        isLoadingMore = state.recentHistoryIsLoadingMore,
                        onLoadMore = vm::loadMoreRecent,
                        onMoviePlay = vm::playRecentMovie,
                        onMovieClick = onMovieClick,
                        onEpisodePlay = vm::playRecentEpisode,
                        onEpisodeClick = onEpisodeClick,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContinueWatchingList(
    shows: List<TraktWatchedShow>,
    progressMap: Map<String, TraktShowProgress>,
    posters: Map<Int, String?>,
    onPlay: (TraktWatchedShow) -> Unit,
    onShowClick: (Int) -> Unit,
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
            val tmdbId = watched.show.ids.tmdb
            val prog = progressMap[slug]
            val nextEp = prog?.nextEpisode
            val initials = watched.show.title
                .split(' ')
                .take(2)
                .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                .joinToString("")
            val posterPath = tmdbId?.let { posters[it] }
            val progressFraction = prog?.let {
                if (it.aired > 0) (it.completed.toFloat() / it.aired).coerceIn(0f, 1f) else 0f
            } ?: 0f

            Card(
                onClick = { tmdbId?.let(onShowClick) },
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
                        if (posterPath != null) {
                            AsyncImage(
                                model = FenLightApp.posterUrl(posterPath, "w185"),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            Text(
                                text = initials,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.7f),
                            )
                        }
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
    showOwner: Boolean = false,
) {
    if (lists.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No lists found", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(8.dp)) {
        items(lists) { list ->
            TraktListCard(list, onListClick, onListLongClick, showOwner)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TraktListCard(
    list: TraktList,
    onListClick: (TraktList) -> Unit,
    onListLongClick: ((TraktList) -> Unit)? = null,
    showOwner: Boolean = false,
) {
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
            val owner = list.user?.username?.takeIf { showOwner && it.isNotBlank() }
            val meta = buildString {
                if (owner != null) append("by $owner · ")
                append("${list.itemCount} items · ♥ ${list.likes}")
            }
            Text(meta, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun ListSearchView(
    query: String,
    results: List<TraktList>,
    isSearching: Boolean,
    searchPerformed: Boolean,
    hasMore: Boolean,
    isLoadingMore: Boolean,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onLoadMore: () -> Unit,
    onListClick: (TraktList) -> Unit,
    onListLongClick: (TraktList) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            label = { Text("Search public Trakt lists") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            trailingIcon = {
                IconButton(onClick = onSearch, enabled = query.isNotBlank()) {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )

        when {
            isSearching -> LoadingIndicator(modifier = Modifier.padding(32.dp))
            results.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    if (searchPerformed) "No lists found for \"${query.trim()}\""
                    else "Tap a result to browse it · long-press to like it",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 32.dp),
                )
            }
            else -> {
                val listState = rememberLazyListState()
                // Keyed remember, or the lambda keeps the first composition's values
                val shouldLoadMore by remember(results.size, isLoadingMore, hasMore) {
                    derivedStateOf {
                        val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                        last >= results.size - 5 && !isLoadingMore && hasMore
                    }
                }
                LaunchedEffect(shouldLoadMore) { if (shouldLoadMore) onLoadMore() }

                LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(8.dp)) {
                    items(results) { list ->
                        TraktListCard(list, onListClick, onListLongClick, showOwner = true)
                    }
                    if (isLoadingMore) {
                        item { LoadingIndicator(modifier = Modifier.padding(16.dp)) }
                    }
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
    onMovieClick: (Int) -> Unit,
    onShowClick: (Int) -> Unit,
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
                    modifier = movie.ids.tmdb?.let { id -> Modifier.clickable { onMovieClick(id) } } ?: Modifier,
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
                    modifier = show.ids.tmdb?.let { id -> Modifier.clickable { onShowClick(id) } } ?: Modifier,
                )
                HorizontalDivider()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ListItemsScreen(
    listName: String,
    listSlug: String,
    isMine: Boolean,
    enrichedItems: List<PaginatedItem>,
    typeMap: Map<Int, String>,
    isLoading: Boolean,
    isRefreshing: Boolean,
    isLoadingMore: Boolean,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
    onRefresh: () -> Unit,
    onMovieClick: (Int) -> Unit,
    onShowClick: (Int) -> Unit,
) {
    var selectedItem by remember { mutableStateOf<PaginatedItem?>(null) }

    selectedItem?.let { item ->
        val mediaType = typeMap[item.id] ?: "movie"
        ListManagementSheet(
            mediaId = item.id,
            mediaType = mediaType,
            title = item.title,
            posterUrl = item.posterUrl,
            currentTraktListSlug = if (isMine) listSlug else null,
            currentTraktListName = if (isMine) listName else null,
            onDismiss = { selectedItem = null },
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (isLoading && enrichedItems.isEmpty()) {
            LoadingIndicator(modifier = Modifier.padding(32.dp))
            return@Column
        }
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            PaginatedGrid(
                items = enrichedItems,
                isLoading = isLoadingMore,
                hasMore = hasMore,
                onLoadMore = onLoadMore,
                onItemClick = { item ->
                    if (typeMap[item.id] == "show") onShowClick(item.id) else onMovieClick(item.id)
                },
                onItemLongClick = { selectedItem = it },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun RecentTab(
    history: List<TraktHistoryEntry>,
    hasMore: Boolean,
    isLoadingMore: Boolean,
    onLoadMore: () -> Unit,
    onMoviePlay: (TraktHistoryEntry) -> Unit = {},
    onMovieClick: (Int) -> Unit = {},
    onEpisodePlay: (TraktHistoryEntry) -> Unit = {},
    onEpisodeClick: (showId: Int, season: Int, episode: Int) -> Unit = { _, _, _ -> },
) {
    if (history.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No watch history found", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    val listState = rememberLazyListState()
    // Keyed remember, or the lambda keeps the first composition's history/hasMore values
    val shouldLoadMore by remember(history.size, isLoadingMore, hasMore) {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= history.size - 5 && !isLoadingMore && hasMore
        }
    }
    LaunchedEffect(shouldLoadMore) { if (shouldLoadMore) onLoadMore() }

    LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
        items(history, key = { it.id }) { entry ->
            val title = when (entry.type) {
                "movie" -> entry.movie?.title ?: "Unknown"
                else -> entry.show?.title ?: "Unknown"
            }
            val supporting = buildString {
                when (entry.type) {
                    "episode" -> {
                        val ep = entry.episode
                        if (ep != null) {
                            append("S${ep.season}E${ep.number}")
                            if (!ep.title.isNullOrBlank()) append(" · ${ep.title}")
                        }
                    }
                    "movie" -> entry.movie?.year?.let { append(it.toString()) }
                }
                val watchedDate = entry.watchedAt.take(10)
                if (isNotEmpty()) append(" · ")
                append(watchedDate)
            }
            val rowModifier = when (entry.type) {
                "movie" -> entry.movie?.ids?.tmdb?.let { id -> Modifier.clickable { onMovieClick(id) } } ?: Modifier
                "episode" -> entry.show?.ids?.tmdb?.let { showId ->
                    val ep = entry.episode
                    if (ep != null) Modifier.clickable { onEpisodeClick(showId, ep.season, ep.number) }
                    else Modifier
                } ?: Modifier
                else -> Modifier
            }
            ListItem(
                headlineContent = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                supportingContent = {
                    if (supporting.isNotEmpty()) Text(
                        supporting,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                trailingContent = {
                    when (entry.type) {
                        "movie" -> if (entry.movie?.ids?.tmdb != null) {
                            IconButton(onClick = { onMoviePlay(entry) }) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        "episode" -> if (entry.show?.ids?.tmdb != null && entry.episode != null) {
                            IconButton(onClick = { onEpisodePlay(entry) }) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                },
                modifier = rowModifier,
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
