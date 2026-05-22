package com.fenlight.companion.ui.tvshows

import androidx.compose.foundation.layout.*
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
fun TvBrowseScreen(
    onShowClick: (Int) -> Unit,
    vm: TvViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(selectedTabIndex = state.tab.ordinal, edgePadding = 0.dp) {
            listOf("Popular", "Trending", "On the Air", "Airing Today", "Search", "Discover")
                .forEachIndexed { i, label ->
                    Tab(
                        selected = state.tab.ordinal == i,
                        onClick = { vm.selectTab(TvBrowseTab.values()[i]) },
                        text = { Text(label) },
                    )
                }
        }

        when (state.tab) {
            TvBrowseTab.SEARCH -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = vm::onSearchQueryChange,
                        placeholder = { Text("Search TV shows…") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                    )
                    PaginatedGrid(
                        items = state.items,
                        isLoading = state.isLoading,
                        hasMore = state.hasMore,
                        onLoadMore = vm::loadNextPage,
                        onItemClick = { onShowClick(it.id) },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            TvBrowseTab.DISCOVER -> {
                val sortOptions = listOf(
                    "popularity.desc" to "Most Popular",
                    "vote_average.desc" to "Highest Rated",
                    "first_air_date.desc" to "Newest",
                )
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(modifier = Modifier.padding(8.dp)) {
                        var expanded by remember { mutableStateOf(false) }
                        Box {
                            OutlinedTextField(
                                value = sortOptions.firstOrNull { it.first == state.sortBy }?.second ?: "Sort",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Sort by") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                sortOptions.forEach { (key, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = { vm.onSortChange(key); expanded = false },
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
                        onItemClick = { onShowClick(it.id) },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            else -> PaginatedGrid(
                items = state.items,
                isLoading = state.isLoading,
                hasMore = state.hasMore,
                onLoadMore = vm::loadNextPage,
                onItemClick = { onShowClick(it.id) },
                modifier = Modifier.fillMaxSize(),
            )
        }

        state.error?.let {
            ErrorMessage(it, onRetry = vm::loadNextPage, modifier = Modifier.padding(16.dp))
        }
    }
}
