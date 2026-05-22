package com.fenlight.companion.ui.movies

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.fenlight.companion.FenLightApp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieDetailScreen(
    tmdbId: Int,
    onBack: () -> Unit,
    vm: MovieViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(tmdbId) { vm.loadMovieDetail(tmdbId) }

    val movie = state.selectedMovie

    // Snackbar for play feedback
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.playMessage) {
        state.playMessage?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearPlayMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(movie?.title ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            if (movie != null) {
                ExtendedFloatingActionButton(
                    onClick = { vm.playMovie(movie) },
                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                    text = { Text("Play on Kodi") },
                    containerColor = MaterialTheme.colorScheme.primary,
                )
            }
        },
    ) { padding ->
        if (state.isLoading || movie == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            // Backdrop
            AsyncImage(
                model = FenLightApp.backdropUrl(movie.backdropPath),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
            )

            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AsyncImage(
                        model = FenLightApp.posterUrl(movie.posterPath),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.width(100.dp).aspectRatio(2f / 3f),
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(movie.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        movie.releaseDate?.take(4)?.let { Text("$it", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        if (movie.voteAverage > 0) {
                            Text("★ ${"%.1f".format(movie.voteAverage)} (${movie.voteCount})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                        movie.runtime?.let { Text("${it}m", style = MaterialTheme.typography.bodySmall) }
                        movie.genres?.take(3)?.joinToString(" · ") { it.name }?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }

                if (movie.overview.isNotBlank()) {
                    Text("Overview", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(movie.overview, style = MaterialTheme.typography.bodyMedium)
                }

                val directors = movie.credits?.crew?.filter { it.job == "Director" }?.map { it.name }
                if (!directors.isNullOrEmpty()) {
                    Text("Director: ${directors.joinToString()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                val cast = movie.credits?.cast?.take(6)?.joinToString { it.name }
                if (!cast.isNullOrBlank()) {
                    Text("Cast: $cast", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Spacer(Modifier.height(72.dp)) // space for FAB
            }
        }
    }
}
