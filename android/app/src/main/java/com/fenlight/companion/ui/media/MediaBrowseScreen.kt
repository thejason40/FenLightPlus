package com.fenlight.companion.ui.media

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fenlight.companion.data.model.BrowseRowConfig
import com.fenlight.companion.data.model.MediaType
import com.fenlight.companion.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaBrowseScreen(
    mediaType: MediaType,
    onItemClick: (Int) -> Unit,
    onShowRecommendations: (Int) -> Unit = {},
    onShowSimilar: (Int) -> Unit = {},
    onSeeAll: (BrowseRowConfig) -> Unit = {},
    vm: MediaHomeViewModel,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var selectedItem by remember { mutableStateOf<PaginatedItem?>(null) }

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

    if (state.showAddRowSheet) {
        AddBrowseRowSheet(
            mediaType = mediaType.tmdbName,
            pendingType = state.pendingRowType,
            pendingLabel = state.pendingRowLabel,
            pendingFilters = state.pendingRowFilters,
            pendingListId = state.pendingListId,
            pendingTraktSlug = state.pendingTraktSlug,
            pendingTraktUser = state.pendingTraktUser,
            genres = state.genres,
            watchProviders = state.watchProviders,
            availableTmdbLists = state.availableTmdbLists,
            availableTraktLists = state.availableTraktLists,
            onTypeChange = vm::onPendingRowTypeChange,
            onLabelChange = vm::onPendingRowLabelChange,
            onFiltersChange = vm::onPendingRowFiltersChange,
            onListIdChange = vm::onPendingListIdChange,
            onTraktListChange = vm::onPendingTraktListChange,
            onConfirm = vm::addCustomRow,
            onDismiss = vm::dismissAddRowSheet,
            addRowError = state.addRowError,
        )
    }

    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = vm::refresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(state.rows, key = { it.config.id }) { rowState ->
                val fixedIds = listOf("fixed_popular", "fixed_trending")
                BrowseRow(
                    state = rowState,
                    onItemClick = { onItemClick(it.id) },
                    onItemLongClick = { selectedItem = it },
                    onSeeAll = { onSeeAll(rowState.config) },
                    onRemove = if (rowState.config.id !in fixedIds) {
                        { vm.removeRow(rowState.config.id) }
                    } else null,
                    onRetry = { vm.retryRow(rowState.config.id) },
                )
                HorizontalDivider()
            }
            item {
                TextButton(
                    onClick = vm::showAddRowSheet,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add Row")
                }
            }
        }
    }
}
