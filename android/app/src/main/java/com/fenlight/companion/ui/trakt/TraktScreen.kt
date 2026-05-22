package com.fenlight.companion.ui.trakt

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.fenlight.companion.FenLightApp
import com.fenlight.companion.data.model.TraktCalendarEpisode
import com.fenlight.companion.data.model.TraktList
import com.fenlight.companion.data.model.TraktListItem
import com.fenlight.companion.ui.components.ErrorMessage
import com.fenlight.companion.ui.components.LoadingIndicator

@Composable
fun TraktScreen(vm: TraktViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.playMessage) {
        state.playMessage?.let { snackbarHostState.showSnackbar(it); vm.clearPlayMessage() }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.listItems.isNotEmpty()) {
                // Show list items
                ListItemsScreen(
                    listName = state.selectedListName,
                    items = state.listItems,
                    onBack = vm::clearListItems,
                    onPlayMovie = vm::playListMovie,
                )
                return@Column
            }

            TabRow(selectedTabIndex = state.tab.ordinal) {
                listOf("Next Episodes", "My Lists", "Liked Lists").forEachIndexed { i, label ->
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

            when (state.tab) {
                TraktTab.NEXT_EPISODES -> CalendarList(state.calendarEpisodes, vm::playEpisode)
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

@Composable
private fun CalendarList(
    episodes: List<TraktCalendarEpisode>,
    onPlay: (TraktCalendarEpisode) -> Unit,
) {
    if (episodes.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No upcoming episodes", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(8.dp)) {
        items(episodes) { cal ->
            val ep = cal.episode
            val show = cal.show
            ListItem(
                headlineContent = { Text(show.title, fontWeight = FontWeight.SemiBold) },
                supportingContent = {
                    Text("S${ep.season}E${ep.number} · ${ep.title}")
                    cal.firstAired.take(10).let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                },
                trailingContent = {
                    IconButton(onClick = { onPlay(cal) }) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = MaterialTheme.colorScheme.primary)
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
    onBack: () -> Unit,
    onPlayMovie: (TraktListItem) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(listName) },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
            },
        )
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(8.dp)) {
            items(items) { item ->
                val title = item.movie?.title ?: item.show?.title ?: "Unknown"
                val year = (item.movie?.year ?: item.show?.year)?.toString() ?: ""
                val posterPath = null // Trakt doesn't return images directly; we'd need a TMDB lookup
                ListItem(
                    headlineContent = { Text(title) },
                    supportingContent = { Text(year + if (item.type.isNotBlank()) " · ${item.type}" else "") },
                    leadingContent = {
                        AsyncImage(
                            model = FenLightApp.posterUrl(posterPath),
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            contentScale = ContentScale.Crop,
                        )
                    },
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
        }
    }
}
