package com.fenlight.companion.ui.tmdb

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fenlight.companion.FenLightApp
import com.fenlight.companion.ui.components.ErrorMessage
import com.fenlight.companion.ui.components.ListManagementSheet
import com.fenlight.companion.ui.components.LoadingIndicator
import com.fenlight.companion.ui.components.PaginatedGrid
import com.fenlight.companion.ui.components.PaginatedItem
import com.fenlight.companion.ui.components.rememberPlayMessageSnackbar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TmdbListsScreen(
    onMovieClick: (Int) -> Unit = {},
    onShowClick: (Int) -> Unit = {},
    vm: TmdbListsViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbarHostState = rememberPlayMessageSnackbar(state.playMessage) { vm.clearPlayMessage() }
    var selectedItem by remember { mutableStateOf<PaginatedItem?>(null) }
    val selectedItemMediaType = remember(state.listItems, selectedItem) {
        state.listItems.firstOrNull { it.id == selectedItem?.id }?.mediaType ?: "movie"
    }

    selectedItem?.let { item ->
        ListManagementSheet(
            mediaId = item.id,
            mediaType = selectedItemMediaType,
            title = item.title,
            posterUrl = item.posterUrl,
            currentTmdbListId = state.selectedListId.takeIf { it != 0 },
            currentTmdbListName = state.selectedListName.takeIf { it.isNotBlank() },
            onDismiss = { selectedItem = null },
        )
    }

    // Create list dialog
    if (state.showCreateListDialog) {
        var listName by remember { mutableStateOf("") }
        var listDesc by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = vm::dismissCreateListDialog,
            title = { Text("New TMDB List") },
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
                TextButton(onClick = { vm.createTmdbList(listName, listDesc) }, enabled = listName.isNotBlank()) {
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
                    onClick = { vm.deleteTmdbList(list.id) },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = vm::cancelDeleteList) { Text("Cancel") } },
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (state.isAuthenticated && state.listItems.isEmpty() && state.selectedListName.isEmpty() && !state.isLoading) {
                FloatingActionButton(onClick = vm::showCreateListDialog) {
                    Icon(Icons.Default.Add, contentDescription = "Create list")
                }
            }
        },
    ) { padding ->
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
                val mediaTypeById = remember(state.listItems) {
                    state.listItems.associate { it.id to it.mediaType }
                }
                val gridItems = state.listItems.map { item ->
                    PaginatedItem(
                        id = item.id,
                        title = item.title ?: item.name ?: "Unknown",
                        posterUrl = FenLightApp.posterUrl(item.posterPath),
                        rating = null,
                        backdropUrl = null,
                    )
                }
                PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = vm::refresh,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    PaginatedGrid(
                        items = gridItems,
                        isLoading = state.listItemIsLoadingMore,
                        hasMore = state.listItemHasMore,
                        onLoadMore = vm::loadMoreListItems,
                        onItemClick = { item ->
                            when (mediaTypeById[item.id]) {
                                "tv" -> onShowClick(item.id)
                                else -> onMovieClick(item.id)
                            }
                        },
                        onItemLongClick = { selectedItem = it },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
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

            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = vm::refresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(8.dp)) {
                    items(state.lists) { list ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .combinedClickable(
                                    onClick = { vm.loadListItems(list.id, list.name) },
                                    onLongClick = { vm.confirmDeleteList(list) },
                                ),
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
}
