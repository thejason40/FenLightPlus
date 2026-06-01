package com.fenlight.companion.ui.tvshows

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fenlight.companion.ui.components.*
import com.fenlight.companion.ui.movies.DropdownField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TvBrowseScreen(
    onShowClick: (Int) -> Unit,
    onShowRecommendations: (Int) -> Unit = {},
    onShowSimilar: (Int) -> Unit = {},
    vm: TvViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var selectedItem by remember { mutableStateOf<PaginatedItem?>(null) }

    selectedItem?.let { item ->
        ListManagementSheet(
            mediaId = item.id,
            mediaType = "show",
            title = item.title,
            posterUrl = item.posterUrl,
            onShowRecommendations = { onShowRecommendations(item.id) },
            onShowSimilar = { onShowSimilar(item.id) },
            onDismiss = { selectedItem = null },
        )
    }

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
                    if (state.searchQuery.length >= 2 && !state.isLoading && state.items.isEmpty() && state.error == null) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No results for \"${state.searchQuery}\"", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        PaginatedGrid(
                            items = state.items,
                            isLoading = state.isLoading,
                            hasMore = state.hasMore,
                            onLoadMore = vm::loadNextPage,
                            onItemClick = { onShowClick(it.id) },
                            onItemLongClick = { selectedItem = it },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
            TvBrowseTab.DISCOVER -> DiscoverTab(state, vm, onShowClick, onItemLongClick = { selectedItem = it })
            else -> PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = vm::refresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                PaginatedGrid(
                    items = state.items,
                    isLoading = state.isLoading,
                    hasMore = state.hasMore,
                    onLoadMore = vm::loadNextPage,
                    onItemClick = { onShowClick(it.id) },
                    onItemLongClick = { selectedItem = it },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        state.error?.let {
            ErrorMessage(it, onRetry = vm::loadNextPage, modifier = Modifier.padding(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiscoverTab(
    state: TvUiState,
    vm: TvViewModel,
    onShowClick: (Int) -> Unit,
    onItemLongClick: (PaginatedItem) -> Unit,
) {
    if (!state.discoverShowForm && state.items.isNotEmpty()) {
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = vm::refresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TextButton(
                    onClick = vm::showDiscoverForm,
                    modifier = Modifier.padding(horizontal = 8.dp),
                ) {
                    Text("← Change Filters")
                }
                PaginatedGrid(
                    items = state.items,
                    isLoading = state.isLoading,
                    hasMore = state.hasMore,
                    onLoadMore = vm::loadNextPage,
                    onItemClick = { onShowClick(it.id) },
                    onItemLongClick = onItemLongClick,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        return
    }

    val sortOptions = listOf(
        "popularity.desc" to "Most Popular",
        "vote_average.desc" to "Highest Rated",
        "first_air_date.desc" to "Newest Release",
        "vote_count.desc" to "Most Voted",
    )
    val languages = listOf(
        "" to "Any Language", "en" to "English", "es" to "Spanish",
        "fr" to "French", "de" to "German", "it" to "Italian",
        "pt" to "Portuguese", "ja" to "Japanese", "ko" to "Korean",
        "zh" to "Chinese", "hi" to "Hindi",
    )

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Discover TV Shows", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

        // Genre
        DropdownField(
            label = "Genre",
            options = listOf("" to "Any Genre") + state.tvGenres.map { it.id.toString() to it.name },
            selected = state.discoverFilters.genreId,
            onSelect = { vm.onTvDiscoverFilterChange(state.discoverFilters.copy(genreId = it)) },
        )

        // First Air Year
        OutlinedTextField(
            value = state.discoverFilters.firstAirYear,
            onValueChange = { vm.onTvDiscoverFilterChange(state.discoverFilters.copy(firstAirYear = it)) },
            label = { Text("First Air Year") },
            placeholder = { Text("e.g. 2023") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )

        // Sort
        DropdownField(
            label = "Sort By",
            options = sortOptions,
            selected = state.discoverFilters.sortBy,
            onSelect = { vm.onTvDiscoverFilterChange(state.discoverFilters.copy(sortBy = it)) },
        )

        // Min rating
        DropdownField(
            label = "Minimum Rating",
            options = listOf("" to "Any Rating", "5" to "5+", "6" to "6+", "7" to "7+", "7.5" to "7.5+", "8" to "8+", "9" to "9+"),
            selected = state.discoverFilters.minRating,
            onSelect = { vm.onTvDiscoverFilterChange(state.discoverFilters.copy(minRating = it)) },
        )

        // Language
        DropdownField(
            label = "Language",
            options = languages,
            selected = state.discoverFilters.language,
            onSelect = { vm.onTvDiscoverFilterChange(state.discoverFilters.copy(language = it)) },
        )

        Button(
            onClick = vm::runDiscover,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Discover") }
    }
}
