package com.fenlight.companion.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PlaylistRemove
import androidx.compose.material.icons.filled.Recommend
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.fenlight.companion.ui.lists.ListManagementViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListManagementSheet(
    mediaId: Int,
    mediaType: String,
    title: String,
    posterUrl: String?,
    onDismiss: () -> Unit,
    // When non-null the user is inside this list — show "Remove from list" instead of "Add to list"
    currentTraktListSlug: String? = null,
    currentTraktListName: String? = null,
    currentTmdbListId: Int? = null,
    currentTmdbListName: String? = null,
    // When provided, the top-level menu offers Recommendations / Similar entries
    onShowRecommendations: (() -> Unit)? = null,
    onShowSimilar: (() -> Unit)? = null,
    vm: ListManagementViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var showListManagement by remember { mutableStateOf(false) }
    var showTraktListPicker by remember { mutableStateOf(false) }
    var showTmdbListPicker by remember { mutableStateOf(false) }
    var showListsContaining by remember { mutableStateOf(false) }
    var showOpenedList by remember { mutableStateOf(false) }

    if (showOpenedList) {
        val openedList = state.openedList ?: return
        ModalBottomSheet(onDismissRequest = { showOpenedList = false; vm.closeList() }) {
            Text(
                openedList.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            openedList.user?.username?.takeIf { it.isNotBlank() }?.let { owner ->
                Text(
                    "by $owner",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp),
                )
            }
            HorizontalDivider()
            val openedListItems = state.openedListItems
            when {
                state.openedListLoading || openedListItems == null -> {
                    Box(
                        Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator() }
                }
                openedListItems.isEmpty() -> {
                    Text(
                        "No items found",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
                else -> {
                    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                        items(openedListItems, key = { it.movie?.ids?.trakt ?: it.show?.ids?.trakt ?: 0 }) { item ->
                            val itemTitle = item.movie?.title ?: item.show?.title ?: ""
                            val year = item.movie?.year ?: item.show?.year
                            val typeLabel = if (item.type == "movie") "Movie" else "Show"
                            ListItem(
                                headlineContent = { Text(itemTitle) },
                                supportingContent = {
                                    Text(if (year != null) "$typeLabel · $year" else typeLabel)
                                },
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
        return
    }

    if (showListsContaining) {
        ModalBottomSheet(onDismissRequest = { showListsContaining = false }) {
            Text(
                "Lists containing \"$title\"",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            HorizontalDivider()
            val lists = state.listsContaining
            when {
                state.listsContainingLoading || lists == null -> {
                    Box(
                        Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator() }
                }
                lists.isEmpty() -> {
                    Text(
                        "No public lists found containing this title",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
                else -> {
                    val listState = rememberLazyListState()
                    val shouldLoadMore by remember {
                        derivedStateOf {
                            state.listsContainingHasMore &&
                                !state.listsContainingLoadingMore &&
                                listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
                                    .let { it != null && it >= listState.layoutInfo.totalItemsCount - 3 }
                        }
                    }
                    LaunchedEffect(shouldLoadMore) {
                        if (shouldLoadMore) vm.loadMoreListsContaining()
                    }
                    LazyColumn(state = listState, contentPadding = PaddingValues(bottom = 24.dp)) {
                        items(lists, key = { it.ids.trakt ?: it.name }) { list ->
                            val isLiked = list.ids.trakt in state.likedListIds
                            ListItem(
                                headlineContent = { Text(list.name) },
                                supportingContent = {
                                    val owner = list.user?.username?.takeIf { it.isNotBlank() }
                                    Text(
                                        buildString {
                                            if (owner != null) append("by $owner · ")
                                            append("${list.itemCount} items · ♥ ${list.likes}")
                                        }
                                    )
                                },
                                trailingContent = if (state.hasTraktAuth) ({
                                    IconButton(onClick = { vm.toggleListLike(list) }) {
                                        Icon(
                                            if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                            contentDescription = if (isLiked) "Unlike list" else "Like list",
                                            tint = if (isLiked) MaterialTheme.colorScheme.primary
                                                   else MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }) else null,
                                modifier = Modifier.clickable {
                                    vm.openList(list)
                                    showOpenedList = true
                                },
                            )
                            HorizontalDivider()
                        }
                        if (state.listsContainingLoadingMore) {
                            item("footer") {
                                Box(
                                    Modifier.fillMaxWidth().padding(16.dp),
                                    contentAlignment = Alignment.Center,
                                ) { CircularProgressIndicator(Modifier.size(24.dp)) }
                            }
                        }
                    }
                }
            }
        }
        return
    }

    if (showTraktListPicker) {
        ModalBottomSheet(onDismissRequest = { showTraktListPicker = false }) {
            Text(
                "Add to Trakt List",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                items(state.traktLists) { list ->
                    ListItem(
                        headlineContent = { Text(list.name) },
                        supportingContent = { Text("${list.itemCount} items") },
                        modifier = Modifier.clickable {
                            vm.addToTraktList(mediaId, mediaType, list.slug)
                            showTraktListPicker = false
                            onDismiss()
                        },
                    )
                    HorizontalDivider()
                }
                if (state.traktLists.isEmpty()) {
                    item {
                        Text(
                            "No lists found",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }
        }
        return
    }

    if (showTmdbListPicker) {
        ModalBottomSheet(onDismissRequest = { showTmdbListPicker = false }) {
            Text(
                "Add to TMDB List",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                items(state.tmdbLists) { list ->
                    ListItem(
                        headlineContent = { Text(list.name) },
                        supportingContent = { Text("${list.itemCount} items") },
                        modifier = Modifier.clickable {
                            vm.addToTmdbList(mediaId, mediaType, list.id)
                            showTmdbListPicker = false
                            onDismiss()
                        },
                    )
                    HorizontalDivider()
                }
                if (state.tmdbLists.isEmpty()) {
                    item {
                        Text(
                            "No lists found",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }
        }
        return
    }

    // List Management submenu — the original add/remove options live here
    if (showListManagement) {
        ModalBottomSheet(onDismissRequest = { showListManagement = false }) {
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                Text(
                    "List Management",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                HorizontalDivider()

                // Trakt section — only when authenticated
                if (state.hasTraktAuth) {
                    Text(
                        "Trakt",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                    val isWatchlisted = mediaId in state.watchlistedIds
                    ListItem(
                        headlineContent = { Text(if (isWatchlisted) "Remove from Watchlist" else "Add to Watchlist") },
                        leadingContent = {
                            Icon(
                                if (isWatchlisted) Icons.Default.Star else Icons.Outlined.Star,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        },
                        modifier = Modifier.clickable {
                            if (isWatchlisted) vm.removeFromTraktWatchlist(mediaId, mediaType)
                            else vm.addToTraktWatchlist(mediaId, mediaType)
                            onDismiss()
                        },
                    )
                    if (currentTraktListSlug != null) {
                        ListItem(
                            headlineContent = { Text("Remove from ${currentTraktListName ?: "list"}") },
                            leadingContent = { Icon(Icons.Default.PlaylistRemove, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            modifier = Modifier.clickable {
                                vm.removeFromTraktList(mediaId, mediaType, currentTraktListSlug)
                                onDismiss()
                            },
                        )
                    } else {
                        ListItem(
                            headlineContent = { Text("Add to Trakt List…") },
                            leadingContent = { Icon(Icons.Default.PlaylistAdd, contentDescription = null) },
                            modifier = Modifier.clickable {
                                vm.loadTraktLists()
                                showTraktListPicker = true
                            },
                        )
                    }
                }

                // TMDB section — only when authenticated
                if (state.hasTmdbAuth) {
                    if (state.hasTraktAuth) HorizontalDivider()
                    Text(
                        "TMDB",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                    if (currentTmdbListId != null) {
                        ListItem(
                            headlineContent = { Text("Remove from ${currentTmdbListName ?: "list"}") },
                            leadingContent = { Icon(Icons.Default.PlaylistRemove, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            modifier = Modifier.clickable {
                                vm.removeFromTmdbList(mediaId, mediaType, currentTmdbListId)
                                onDismiss()
                            },
                        )
                    } else {
                        ListItem(
                            headlineContent = { Text("Add to TMDB List…") },
                            leadingContent = { Icon(Icons.Default.PlaylistAdd, contentDescription = null) },
                            modifier = Modifier.clickable {
                                vm.loadTmdbLists()
                                showTmdbListPicker = true
                            },
                        )
                    }
                }
            }
        }
        return
    }

    // Top-level menu
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            // Header
            ListItem(
                headlineContent = { Text(title, style = MaterialTheme.typography.titleMedium) },
                leadingContent = {
                    AsyncImage(
                        model = posterUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        contentScale = ContentScale.Crop,
                    )
                },
            )
            HorizontalDivider()

            // List Management — only when at least one list service is authenticated
            if (state.hasTraktAuth || state.hasTmdbAuth) {
                ListItem(
                    headlineContent = { Text("List Management") },
                    leadingContent = { Icon(Icons.Default.ViewList, contentDescription = null) },
                    modifier = Modifier.clickable { showListManagement = true },
                )
            }

            // Find public Trakt lists containing this title — public endpoint, no auth required
            ListItem(
                headlineContent = { Text("Find Lists Containing…") },
                leadingContent = { Icon(Icons.Default.TravelExplore, contentDescription = null) },
                modifier = Modifier.clickable {
                    vm.loadListsContaining(mediaId, mediaType)
                    showListsContaining = true
                },
            )

            if (onShowRecommendations != null) {
                ListItem(
                    headlineContent = { Text("Recommendations") },
                    leadingContent = { Icon(Icons.Default.Recommend, contentDescription = null) },
                    modifier = Modifier.clickable { onShowRecommendations(); onDismiss() },
                )
            }

            if (onShowSimilar != null) {
                ListItem(
                    headlineContent = { Text("Similar") },
                    leadingContent = { Icon(Icons.Default.Movie, contentDescription = null) },
                    modifier = Modifier.clickable { onShowSimilar(); onDismiss() },
                )
            }
        }
    }
}
