package com.fenlight.companion.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

data class PaginatedItem(
    val id: Int,
    val title: String,
    val posterUrl: String?,
    val rating: Double?,
    val backdropUrl: String? = null,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PaginatedGrid(
    items: List<PaginatedItem>,
    isLoading: Boolean,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
    onItemClick: (PaginatedItem) -> Unit,
    onItemLongClick: ((PaginatedItem) -> Unit)? = null,
    modifier: Modifier = Modifier,
    columns: Int = 3,
) {
    val listState = rememberLazyGridState()

    // Trigger load more when nearing the end of visible items.
    // items.size must be a key too, or the lambda keeps the previous page's list.
    val shouldLoadMore by remember(items.size, isLoading, hasMore) {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= items.size - 6 && !isLoading && hasMore
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) onLoadMore()
    }
    // Also trigger after each page loads — in case the grid isn't full enough to scroll
    LaunchedEffect(items.size, isLoading) {
        if (!isLoading && hasMore) {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            if (lastVisible >= items.size - 6) onLoadMore()
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        state = listState,
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        // Featured card for the first item — full width
        if (items.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }, contentType = "featured") {
                val featured = items.first()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .combinedClickable(
                            onClick = { onItemClick(featured) },
                            onLongClick = onItemLongClick?.let { { it(featured) } },
                        ),
                ) {
                    AsyncImage(
                        model = featured.backdropUrl ?: featured.posterUrl,
                        contentDescription = featured.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f),
                    )
                    // Gradient scrim
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.verticalGradient(
                                    0.3f to Color.Transparent,
                                    1f to Color.Black.copy(alpha = 0.8f),
                                )
                            )
                    )
                    // Title at bottom-start
                    Text(
                        text = featured.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp),
                    )
                    // Rating at bottom-end
                    if (featured.rating != null && featured.rating > 0) {
                        Text(
                            text = "★ ${"%.1f".format(featured.rating)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(12.dp),
                        )
                    }
                }
            }
        }

        // Remaining items in the standard grid
        items(items.drop(1), key = { it.id }, contentType = { "card" }) { item ->
            MediaCard(
                title = item.title,
                posterUrl = item.posterUrl,
                rating = item.rating,
                onClick = { onItemClick(item) },
                onLongClick = onItemLongClick?.let { { it(item) } },
            )
        }

        if (isLoading) {
            item(span = { GridItemSpan(columns) }, contentType = "loader") {
                LoadingIndicator(modifier = Modifier.padding(16.dp))
            }
        }
    }
}
