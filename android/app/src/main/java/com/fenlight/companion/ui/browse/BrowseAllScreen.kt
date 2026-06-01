package com.fenlight.companion.ui.browse

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fenlight.companion.data.model.BrowseRowConfig
import com.fenlight.companion.ui.components.ErrorMessage
import com.fenlight.companion.ui.components.ListManagementSheet
import com.fenlight.companion.ui.components.PaginatedGrid
import com.fenlight.companion.ui.components.PaginatedItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseAllScreen(
    rowConfig: BrowseRowConfig,
    mediaType: String,   // "movie" or "tv"
    onBack: () -> Unit,
    onItemClick: (Int) -> Unit,
    onShowRecommendations: (Int) -> Unit = {},
    onShowSimilar: (Int) -> Unit = {},
    vm: BrowseAllViewModel = viewModel(),
) {
    LaunchedEffect(rowConfig.id) { vm.init(rowConfig, mediaType) }
    val state by vm.state.collectAsStateWithLifecycle()
    var selectedItem by remember { mutableStateOf<PaginatedItem?>(null) }

    selectedItem?.let { item ->
        ListManagementSheet(
            mediaId = item.id,
            mediaType = mediaType,
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
                title = { Text(state.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            state.error?.let {
                ErrorMessage(it, onRetry = vm::loadNextPage, modifier = Modifier.padding(16.dp))
                return@Column
            }
            if (state.items.isEmpty() && !state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nothing found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                return@Column
            }
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
