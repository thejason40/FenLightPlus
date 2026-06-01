package com.fenlight.companion.ui.movies

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieBrowseScreen(
    onMovieClick: (Int) -> Unit,
    onShowRecommendations: (Int) -> Unit = {},
    onShowSimilar: (Int) -> Unit = {},
    vm: MovieViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var selectedItem by remember { mutableStateOf<PaginatedItem?>(null) }

    selectedItem?.let { item ->
        ListManagementSheet(
            mediaId = item.id,
            mediaType = "movie",
            title = item.title,
            posterUrl = item.posterUrl,
            onShowRecommendations = { onShowRecommendations(item.id) },
            onShowSimilar = { onShowSimilar(item.id) },
            onDismiss = { selectedItem = null },
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(selectedTabIndex = state.tab.ordinal, edgePadding = 0.dp) {
            listOf("Popular", "Trending", "Now Playing", "Upcoming", "Search", "Discover")
                .forEachIndexed { i, label ->
                    Tab(
                        selected = state.tab.ordinal == i,
                        onClick = { vm.selectTab(MovieBrowseTab.values()[i]) },
                        text = { Text(label) },
                    )
                }
        }

        when (state.tab) {
            MovieBrowseTab.SEARCH -> SearchTab(state, vm, onMovieClick, onItemLongClick = { selectedItem = it })
            MovieBrowseTab.DISCOVER -> DiscoverTab(state, vm, onMovieClick, onItemLongClick = { selectedItem = it })
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
                    onItemClick = { onMovieClick(it.id) },
                    onItemLongClick = { selectedItem = it },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        state.error?.let {
            ErrorMessage(message = it, onRetry = vm::loadNextPage, modifier = Modifier.padding(16.dp))
        }
    }
}

@Composable
private fun SearchTab(
    state: MovieUiState,
    vm: MovieViewModel,
    onMovieClick: (Int) -> Unit,
    onItemLongClick: (PaginatedItem) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = vm::onSearchQueryChange,
            placeholder = { Text("Search movies…") },
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
                onItemClick = { onMovieClick(it.id) },
                onItemLongClick = onItemLongClick,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiscoverTab(
    state: MovieUiState,
    vm: MovieViewModel,
    onMovieClick: (Int) -> Unit,
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
                    onItemClick = { onMovieClick(it.id) },
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
        "release_date.desc" to "Newest Release",
        "revenue.desc" to "Box Office",
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
        Text("Discover Movies", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

        // Genre
        DropdownField(
            label = "Genre",
            options = listOf("" to "Any Genre") + state.movieGenres.map { it.id.toString() to it.name },
            selected = state.discoverFilters.genreId,
            onSelect = { vm.onDiscoverFilterChange(state.discoverFilters.copy(genreId = it)) },
        )

        // Year
        OutlinedTextField(
            value = state.discoverFilters.year,
            onValueChange = { vm.onDiscoverFilterChange(state.discoverFilters.copy(year = it)) },
            label = { Text("Release Year") },
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
            onSelect = { vm.onDiscoverFilterChange(state.discoverFilters.copy(sortBy = it)) },
        )

        // Min rating
        DropdownField(
            label = "Minimum Rating",
            options = listOf("" to "Any Rating", "5" to "5+", "6" to "6+", "7" to "7+", "7.5" to "7.5+", "8" to "8+", "9" to "9+"),
            selected = state.discoverFilters.minRating,
            onSelect = { vm.onDiscoverFilterChange(state.discoverFilters.copy(minRating = it)) },
        )

        // Language
        DropdownField(
            label = "Language",
            options = languages,
            selected = state.discoverFilters.language,
            onSelect = { vm.onDiscoverFilterChange(state.discoverFilters.copy(language = it)) },
        )

        Button(
            onClick = vm::runDiscover,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Discover") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DropdownField(
    label: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val currentLabel = options.firstOrNull { it.first == selected }?.second ?: options.firstOrNull()?.second ?: ""
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = currentLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (key, name) ->
                DropdownMenuItem(text = { Text(name) }, onClick = { onSelect(key); expanded = false })
            }
        }
    }
}
