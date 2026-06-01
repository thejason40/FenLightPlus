package com.fenlight.companion.ui.movies

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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

@Composable
fun MovieDetailScreen(
    tmdbId: Int,
    onBack: () -> Unit,
    onPersonClick: (Int) -> Unit = {},
    vm: MovieDetailViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(tmdbId) { vm.loadMovieDetail(tmdbId) }

    val movie = state.movie

    // Snackbar for play feedback
    val snackbarHostState = rememberPlayMessageSnackbar(state.playMessage) { vm.clearPlayMessage() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (movie != null) {
                Surface(tonalElevation = 3.dp) {
                    Button(
                        onClick = { vm.playMovie(movie) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .navigationBarsPadding(),
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Play on Kodi")
                    }
                }
            }
        },
    ) { padding ->
        if (movie == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                when {
                    state.error != null -> ErrorMessage(state.error!!, onRetry = { vm.loadMovieDetail(tmdbId) })
                    else -> CircularProgressIndicator()
                }
            }
            return@Scaffold
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = padding.calculateBottomPadding())
                    .verticalScroll(rememberScrollState()),
            ) {
                // Backdrop with gradient scrim
                Box {
                    AsyncImage(
                        model = FenLightApp.backdropUrl(movie.backdropPath),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.verticalGradient(
                                    0.2f to Color.Transparent,
                                    1.0f to Color(0xFF111820),
                                )
                            )
                    )
                }

                // Poster + info row overlapping the backdrop seam
                Box(modifier = Modifier.offset(y = (-40).dp)) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        AsyncImage(
                            model = FenLightApp.posterUrl(movie.posterPath),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .width(100.dp)
                                .aspectRatio(2f / 3f)
                                .shadow(16.dp, RoundedCornerShape(8.dp))
                                .clip(RoundedCornerShape(8.dp)),
                        )
                        Column(
                            modifier = Modifier.padding(top = 48.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(movie.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            movie.releaseDate?.take(4)?.let {
                                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (movie.voteAverage > 0) {
                                Text(
                                    "★ ${"%.1f".format(movie.voteAverage)} (${movie.voteCount})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            movie.runtime?.let { Text("${it}m", style = MaterialTheme.typography.bodySmall) }
                            movie.genres?.take(3)?.joinToString(" · ") { it.name }?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                }

                // Content below the poster row
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp).offset(y = (-28).dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (movie.overview.isNotBlank()) {
                        Text("Overview", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(movie.overview, style = MaterialTheme.typography.bodyMedium)
                    }

                    movie.belongsToCollection?.name?.takeIf { it.isNotBlank() }?.let { seriesName ->
                        Text("Series", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(seriesName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                    }

                    val directors = movie.credits?.crew?.filter { it.job == "Director" }?.map { it.name }
                    if (!directors.isNullOrEmpty()) {
                        Text(
                            "Director: ${directors.joinToString()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    val cast = movie.credits?.cast?.take(15)
                    if (!cast.isNullOrEmpty()) {
                        Text("Cast", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        CastRow(cast = cast, onPersonClick = onPersonClick)
                    }

                    Spacer(Modifier.height(16.dp))
                }
            }

            // Floating back button overlaid at top-start
            Surface(
                color = Color.Black.copy(alpha = 0.5f),
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(8.dp),
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
            }
        }
    }
}
