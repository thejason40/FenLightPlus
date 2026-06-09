package com.fenlight.companion.ui.tvshows

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.fenlight.companion.FenLightApp
import com.fenlight.companion.ui.components.CastRow
import com.fenlight.companion.ui.components.ErrorMessage
import com.fenlight.companion.ui.components.rememberPlayMessageSnackbar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TvDetailScreen(
    tmdbId: Int,
    onBack: () -> Unit,
    onPersonClick: (Int) -> Unit = {},
    onEpisodeClick: (showId: Int, season: Int, episode: Int) -> Unit = { _, _, _ -> },
    vm: TvDetailViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(tmdbId) { vm.loadShowDetail(tmdbId) }

    val snackbarHostState = rememberPlayMessageSnackbar(state.playMessage) { vm.clearPlayMessage() }

    val show = state.show
    val season = state.selectedSeason

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(season?.name ?: show?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (season != null) vm.clearSelectedSeason() else onBack()
                    }) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        if (show == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                when {
                    state.error != null -> ErrorMessage(state.error!!, onRetry = { vm.loadShowDetail(tmdbId) })
                    else -> CircularProgressIndicator()
                }
            }
            return@Scaffold
        }

        if (season != null) {
            // Episode list
            val year = show.firstAirDate?.take(4)?.toIntOrNull() ?: 0
            LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                items(season.episodes) { ep ->
                    ListItem(
                        headlineContent = { Text("${ep.episodeNumber}. ${ep.name}") },
                        supportingContent = {
                            Column {
                                ep.airDate?.let { Text(it, style = MaterialTheme.typography.labelSmall) }
                                if (ep.overview.isNotBlank()) Text(ep.overview, maxLines = 2)
                            }
                        },
                        leadingContent = {
                            AsyncImage(
                                model = FenLightApp.posterUrl(ep.stillPath, "w185"),
                                contentDescription = null,
                                modifier = Modifier.width(80.dp).aspectRatio(16f / 9f),
                                contentScale = ContentScale.Crop,
                            )
                        },
                        trailingContent = {
                            IconButton(onClick = {
                                vm.playEpisode(show.id, show.name, year, season.seasonNumber, ep.episodeNumber)
                            }) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = MaterialTheme.colorScheme.primary)
                            }
                        },
                        modifier = Modifier.clickable {
                            onEpisodeClick(show.id, season.seasonNumber, ep.episodeNumber)
                        },
                    )
                    HorizontalDivider()
                }
            }
        } else {
            // Show detail + season list
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
            ) {
                // Backdrop with gradient scrim
                Box {
                    AsyncImage(
                        model = FenLightApp.backdropUrl(show.backdropPath),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                    )
                    Box(
                        modifier = Modifier.matchParentSize().background(
                            Brush.verticalGradient(0.2f to Color.Transparent, 1.0f to Color(0xFF111820))
                        )
                    )
                }
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AsyncImage(
                            model = FenLightApp.posterUrl(show.posterPath),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.width(100.dp).aspectRatio(2f / 3f),
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(show.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            show.firstAirDate?.take(4)?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            if (show.voteAverage > 0) Text("★ ${"%.1f".format(show.voteAverage)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            show.genres?.take(3)?.joinToString(" · ") { it.name }?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary) }
                        }
                    }
                    if (show.overview.isNotBlank()) {
                        Text("Overview", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(show.overview, style = MaterialTheme.typography.bodyMedium)
                    }
                    val cast = show.credits?.cast?.take(15)
                    if (!cast.isNullOrEmpty()) {
                        Text("Cast", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        CastRow(cast = cast, onPersonClick = onPersonClick)
                    }
                    Text("Seasons", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    show.seasons?.filter { it.seasonNumber > 0 }?.forEach { s ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { vm.loadSeason(show.id, s.seasonNumber) },
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                AsyncImage(
                                    model = FenLightApp.posterUrl(s.posterPath, "w185"),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.width(60.dp).aspectRatio(2f / 3f),
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(s.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                    Text("${s.episodeCount} episodes", style = MaterialTheme.typography.bodySmall)
                                    s.airDate?.take(4)?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
