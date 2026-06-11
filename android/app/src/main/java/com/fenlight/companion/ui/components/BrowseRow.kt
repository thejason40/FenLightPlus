package com.fenlight.companion.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fenlight.companion.ui.media.BrowseRowState

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BrowseRow(
    state: BrowseRowState,
    onItemClick: (PaginatedItem) -> Unit,
    onItemLongClick: (PaginatedItem) -> Unit,
    onSeeAll: () -> Unit,
    onRemove: (() -> Unit)?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMenu by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        // Header row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onSeeAll,
                    onLongClick = { showMenu = true },
                )
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = state.config.label,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Icon(Icons.Default.ChevronRight, contentDescription = "See all")
        }

        if (showMenu) {
            AlertDialog(
                onDismissRequest = { showMenu = false },
                title = { Text(state.config.label) },
                text = null,
                confirmButton = {
                    TextButton(onClick = { showMenu = false; onSeeAll() }) { Text("See all") }
                },
                dismissButton = if (onRemove != null) {
                    { TextButton(onClick = { showMenu = false; onRemove() }) { Text("Remove", color = MaterialTheme.colorScheme.error) } }
                } else null,
            )
        }

        when {
            state.isLoading && state.items.isEmpty() -> {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(5) {
                        Box(
                            modifier = Modifier
                                .width(130.dp).height(195.dp)
                                .padding(bottom = 4.dp),
                        ) {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = MaterialTheme.shapes.medium,
                            ) {}
                        }
                    }
                }
            }
            state.error != null && state.items.isEmpty() -> {
                ErrorMessage(
                    message = state.error,
                    onRetry = onRetry,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            else -> {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.items, key = { it.id }) { item ->
                        MediaCard(
                            title = item.title,
                            posterUrl = item.posterUrl,
                            rating = item.rating,
                            onClick = { onItemClick(item) },
                            onLongClick = { onItemLongClick(item) },
                        )
                    }
                }
            }
        }
    }
}
