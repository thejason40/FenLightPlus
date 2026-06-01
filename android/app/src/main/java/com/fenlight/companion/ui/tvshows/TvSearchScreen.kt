package com.fenlight.companion.ui.tvshows

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fenlight.companion.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TvSearchScreen(
    onBack: () -> Unit,
    onShowClick: (Int) -> Unit,
    onShowRecommendations: (Int) -> Unit = {},
    onShowSimilar: (Int) -> Unit = {},
    vm: TvSearchViewModel = viewModel(),
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = state.query,
                        onValueChange = vm::onQueryChange,
                        placeholder = { Text("Search TV shows…") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.surface,
                            unfocusedBorderColor = MaterialTheme.colorScheme.surface,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.query.length >= 2 && !state.isLoading && state.items.isEmpty() && state.error == null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "No results for \"${state.query}\"",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                state.error != null && state.items.isEmpty() -> {
                    ErrorMessage(
                        message = state.error!!,
                        onRetry = vm::loadNextPage,
                        modifier = Modifier.align(Alignment.Center).padding(16.dp),
                    )
                }
                state.query.length < 2 && state.items.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "Type to search TV shows",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                else -> {
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
    }
}
