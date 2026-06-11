package com.fenlight.companion.ui.media

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
import com.fenlight.companion.data.model.MediaType
import com.fenlight.companion.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaSearchScreen(
    mediaType: MediaType,
    onBack: () -> Unit,
    onItemClick: (Int) -> Unit,
    onShowRecommendations: (Int) -> Unit = {},
    onShowSimilar: (Int) -> Unit = {},
    vm: MediaSearchViewModel,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var selectedItem by remember { mutableStateOf<PaginatedItem?>(null) }
    val mediaLabel = if (mediaType == MediaType.TV) "TV shows" else "movies"

    selectedItem?.let { item ->
        ListManagementSheet(
            mediaId = item.id,
            mediaType = mediaType.tmdbName,
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
                        placeholder = { Text("Search $mediaLabel…") },
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
                state.query.length >= 2 && !state.isLoading && state.page > 0 && state.items.isEmpty() && state.error == null -> {
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
                            "Type to search $mediaLabel",
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
                        onItemClick = { onItemClick(it.id) },
                        onItemLongClick = { selectedItem = it },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}
