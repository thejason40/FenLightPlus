package com.fenlight.companion.ui.tmdb

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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fenlight.companion.FenLightApp
import com.fenlight.companion.ui.components.ErrorMessage
import com.fenlight.companion.ui.components.LoadingIndicator
import com.fenlight.companion.ui.components.PaginatedGrid
import com.fenlight.companion.ui.components.PaginatedItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TmdbListsScreen(vm: TmdbListsViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.playMessage) {
        state.playMessage?.let { snackbarHostState.showSnackbar(it); vm.clearPlayMessage() }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (!state.isAuthenticated && !state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(32.dp),
                    ) {
                        Text("Sign in to TMDB to view your lists.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Go to Settings → Service Setup to sign in.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                return@Column
            }

            if (state.listItems.isNotEmpty() || state.selectedListName.isNotEmpty()) {
                TopAppBar(
                    title = { Text(state.selectedListName) },
                    navigationIcon = { IconButton(onClick = vm::clearListItems) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } },
                )
                if (state.isLoading) { LoadingIndicator(modifier = Modifier.padding(32.dp)); return@Column }
                val gridItems = state.listItems.map { item ->
                    PaginatedItem(
                        id = item.id,
                        title = item.title ?: item.name ?: "Unknown",
                        posterUrl = FenLightApp.posterUrl(item.posterPath),
                        rating = null,
                    )
                }
                PaginatedGrid(
                    items = gridItems,
                    isLoading = state.listItemIsLoadingMore,
                    hasMore = state.listItemHasMore,
                    onLoadMore = vm::loadMoreListItems,
                    onItemClick = {},
                    modifier = Modifier.fillMaxSize(),
                )
                return@Column
            }

            if (state.isLoading) { LoadingIndicator(modifier = Modifier.padding(32.dp)); return@Column }
            state.error?.let { ErrorMessage(it, onRetry = vm::loadLists, modifier = Modifier.padding(16.dp)); return@Column }

            if (state.lists.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No lists found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                return@Column
            }

            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(8.dp)) {
                items(state.lists) { list ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        onClick = { vm.loadListItems(list.id, list.name) },
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(list.name, style = MaterialTheme.typography.titleSmall)
                            if (list.description.isNotBlank()) {
                                Text(list.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                            }
                            Text("${list.itemCount} items", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}
