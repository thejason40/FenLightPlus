package com.fenlight.companion.ui.movies

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fenlight.companion.ui.components.*

@Composable
fun MovieBrowseScreen(
    onMovieClick: (Int) -> Unit,
    vm: MovieViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        // Tab row
        ScrollableTabRow(selectedTabIndex = state.tab.ordinal, edgePadding = 0.dp) {
            listOf("Popular", "Trending", "Now Playing", "Upcoming", "Search", "Discover", "My Lists")
                .forEachIndexed { i, label ->
                    Tab(
                        selected = state.tab.ordinal == i,
                        onClick = { vm.selectTab(MovieBrowseTab.values()[i]) },
                        text = { Text(label) },
                    )
                }
        }

        when (state.tab) {
            MovieBrowseTab.SEARCH -> SearchTab(state, vm, onMovieClick)
            MovieBrowseTab.DISCOVER -> DiscoverTab(state, vm, onMovieClick)
            MovieBrowseTab.LISTS -> ListsTab(state, vm, onMovieClick)
            else -> PaginatedGrid(
                items = state.items,
                isLoading = state.isLoading,
                hasMore = state.hasMore,
                onLoadMore = vm::loadNextPage,
                onItemClick = { onMovieClick(it.id) },
                modifier = Modifier.fillMaxSize(),
            )
        }

        state.error?.let {
            ErrorMessage(message = it, onRetry = vm::loadNextPage, modifier = Modifier.padding(16.dp))
        }
    }
}

@Composable
private fun SearchTab(state: MovieUiState, vm: MovieViewModel, onMovieClick: (Int) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = vm::onSearchQueryChange,
            placeholder = { Text("Search movies…") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(8.dp),
        )
        PaginatedGrid(
            items = state.items,
            isLoading = state.isLoading,
            hasMore = state.hasMore,
            onLoadMore = vm::loadNextPage,
            onItemClick = { onMovieClick(it.id) },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun DiscoverTab(state: MovieUiState, vm: MovieViewModel, onMovieClick: (Int) -> Unit) {
    val sortOptions = listOf(
        "popularity.desc" to "Most Popular",
        "vote_average.desc" to "Highest Rated",
        "release_date.desc" to "Newest",
        "revenue.desc" to "Box Office",
    )
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = state.discoverFilters.year,
                onValueChange = { vm.onDiscoverFilterChange(state.discoverFilters.copy(year = it)) },
                label = { Text("Year") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Box(modifier = Modifier.weight(1.5f)) {
                OutlinedTextField(
                    value = sortOptions.firstOrNull { it.first == state.discoverFilters.sortBy }?.second ?: "Sort",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Sort by") },
                    modifier = Modifier.fillMaxWidth(),
                )
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    sortOptions.forEach { (key, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                vm.onDiscoverFilterChange(state.discoverFilters.copy(sortBy = key))
                                expanded = false
                            },
                        )
                    }
                }
            }
        }
        PaginatedGrid(
            items = state.items,
            isLoading = state.isLoading,
            hasMore = state.hasMore,
            onLoadMore = vm::loadNextPage,
            onItemClick = { onMovieClick(it.id) },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun ListsTab(state: MovieUiState, vm: MovieViewModel, onMovieClick: (Int) -> Unit) {
    if (state.listItems.isNotEmpty()) {
        // Showing items of a specific list
        val gridItems = state.listItems.map { item ->
            PaginatedItem(
                id = item.id,
                title = item.title ?: item.name ?: "Unknown",
                posterUrl = com.fenlight.companion.FenLightApp.posterUrl(item.posterPath),
                rating = null,
            )
        }
        PaginatedGrid(
            items = gridItems,
            isLoading = state.isLoading,
            hasMore = false,
            onLoadMore = {},
            onItemClick = { onMovieClick(it.id) },
            modifier = Modifier.fillMaxSize(),
        )
    } else if (state.isLoading) {
        LoadingIndicator(modifier = Modifier.padding(32.dp))
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(8.dp)) {
            items(state.myLists) { list ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    onClick = { vm.loadListItems(list.id, list.name) },
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(list.name, style = MaterialTheme.typography.titleSmall)
                        if (list.description.isNotBlank()) {
                            Text(list.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("${list.itemCount} items", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
