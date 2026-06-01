package com.fenlight.companion.ui.person

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.fenlight.companion.FenLightApp
import com.fenlight.companion.ui.components.ErrorMessage
import com.fenlight.companion.ui.components.MediaCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonScreen(
    personId: Int,
    onBack: () -> Unit,
    onCreditClick: (id: Int, mediaType: String) -> Unit,
    vm: PersonViewModel = viewModel(),
) {
    LaunchedEffect(personId) { vm.loadPerson(personId) }
    val state by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.person?.name ?: "") },
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
                state.isLoading && state.person == null -> {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
                state.error != null && state.person == null -> {
                    ErrorMessage(state.error!!, onRetry = { vm.loadPerson(personId) }, modifier = Modifier.align(Alignment.Center))
                }
                state.person != null -> {
                    val person = state.person!!
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    AsyncImage(
                                        model = FenLightApp.posterUrl(person.profilePath),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .width(110.dp)
                                            .aspectRatio(2f / 3f)
                                            .clip(RoundedCornerShape(8.dp)),
                                    )
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(person.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                        person.knownForDepartment?.takeIf { it.isNotBlank() }?.let {
                                            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                        }
                                        person.birthday?.takeIf { it.isNotBlank() }?.let {
                                            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        person.placeOfBirth?.takeIf { it.isNotBlank() }?.let {
                                            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                                if (person.biography.isNotBlank()) {
                                    Text(person.biography, style = MaterialTheme.typography.bodySmall, maxLines = 6, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                }
                                Text("Known For", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                        items(state.credits, key = { it.id }) { item ->
                            MediaCard(
                                title = item.title,
                                posterUrl = item.posterUrl,
                                rating = item.rating,
                                onClick = { onCreditClick(item.id, state.mediaTypeById[item.id] ?: "movie") },
                            )
                        }
                    }
                }
            }
        }
    }
}
