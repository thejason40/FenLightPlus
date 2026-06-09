package com.fenlight.companion.ui.tvshows

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.fenlight.companion.ui.components.ErrorMessage
import com.fenlight.companion.ui.components.rememberPlayMessageSnackbar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodeDetailScreen(
    showId: Int,
    season: Int,
    episodeNumber: Int,
    onBack: () -> Unit,
    vm: EpisodeDetailViewModel = viewModel(),
) {
    LaunchedEffect(showId, season, episodeNumber) { vm.load(showId, season, episodeNumber) }
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbarHostState = rememberPlayMessageSnackbar(state.playMessage) { vm.clearPlayMessage() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    val ep = state.episode
                    Text(if (ep != null) "S${ep.seasonNumber}E${ep.episodeNumber}" else "Episode")
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        val ep = state.episode
        if (ep == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                when {
                    state.error != null -> ErrorMessage(state.error!!, onRetry = { vm.load(showId, season, episodeNumber) })
                    else -> CircularProgressIndicator()
                }
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            Box {
                AsyncImage(
                    model = FenLightApp.backdropUrl(ep.stillPath),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                )
                Box(
                    modifier = Modifier.matchParentSize().background(
                        Brush.verticalGradient(0.3f to Color.Transparent, 1.0f to Color(0xFF111820))
                    )
                )
            }

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (state.showName.isNotBlank()) {
                            Text(
                                state.showName,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            "S${ep.seasonNumber}E${ep.episodeNumber} · ${ep.name}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        val meta = buildList {
                            ep.airDate?.let { add(it) }
                            ep.runtime?.let { add("${it}m") }
                            if (ep.voteAverage > 0) add("★ ${"%.1f".format(ep.voteAverage)}")
                        }.joinToString(" · ")
                        if (meta.isNotBlank()) {
                            Text(
                                meta,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    FilledIconButton(onClick = { vm.play(showId) }) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play")
                    }
                }

                if (ep.overview.isNotBlank()) {
                    Text("Overview", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(ep.overview, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
