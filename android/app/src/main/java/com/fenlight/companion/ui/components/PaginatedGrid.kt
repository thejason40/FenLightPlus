package com.fenlight.companion.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class PaginatedItem(
    val id: Int,
    val title: String,
    val posterUrl: String?,
    val rating: Double?,
)

@Composable
fun PaginatedGrid(
    items: List<PaginatedItem>,
    isLoading: Boolean,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
    onItemClick: (PaginatedItem) -> Unit,
    modifier: Modifier = Modifier,
    columns: Int = 3,
) {
    val listState = rememberLazyGridState()

    // Trigger load more when nearing the end
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= items.size - 6 && !isLoading && hasMore
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) onLoadMore()
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        state = listState,
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        items(items, key = { it.id }) { item ->
            MediaCard(
                title = item.title,
                posterUrl = item.posterUrl,
                rating = item.rating,
                onClick = { onItemClick(item) },
            )
        }
        if (isLoading) {
            item(span = { GridItemSpan(columns) }) {
                LoadingIndicator(modifier = Modifier.padding(16.dp))
            }
        }
    }
}
