package com.fenlight.companion.ui.home

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fenlight.companion.R
import com.fenlight.companion.data.model.BrowseRowConfig
import com.fenlight.companion.data.model.MediaType
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.fenlight.companion.ui.browse.BrowseAllScreen
import com.fenlight.companion.ui.media.MediaBrowseScreen
import com.fenlight.companion.ui.media.MediaSearchScreen
import com.fenlight.companion.ui.movies.MovieDetailScreen
import com.fenlight.companion.ui.movies.MovieSearchViewModel
import com.fenlight.companion.ui.movies.MovieViewModel
import com.fenlight.companion.ui.person.PersonScreen
import com.fenlight.companion.ui.realdebrid.RdScreen
import com.fenlight.companion.ui.related.RelatedScreen
import com.fenlight.companion.ui.tmdb.TmdbListsScreen
import com.fenlight.companion.ui.trakt.TraktScreen
import com.fenlight.companion.ui.tvshows.EpisodeDetailScreen
import com.fenlight.companion.ui.tvshows.TvDetailScreen
import com.fenlight.companion.ui.tvshows.TvSearchViewModel
import com.fenlight.companion.ui.tvshows.TvViewModel

private sealed class TopDest(val route: String, val label: String, @DrawableRes val iconRes: Int) {
    object Movies : TopDest("movies", "Movies", R.drawable.icon_movies)
    object TV : TopDest("tv", "TV Shows", R.drawable.icon_tv)
    object TmdbLists : TopDest("tmdb_lists", "Lists", R.drawable.icon_tmdb)
    object Trakt : TopDest("trakt", "Trakt", R.drawable.icon_trakt)
    object RealDebrid : TopDest("rd", "Debrid", R.drawable.icon_realdebrid)
}

// Moshi instance for serializing BrowseRowConfig to/from URL-encoded nav args.
// Created lazily at top-level — lightweight and correct.
private val browseRowConfigMoshi: Moshi by lazy {
    Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
}
private val browseRowConfigAdapter by lazy {
    browseRowConfigMoshi.adapter(BrowseRowConfig::class.java)
}

private fun BrowseRowConfig.toNavArg(): String =
    java.net.URLEncoder.encode(browseRowConfigAdapter.toJson(this), "UTF-8")

private fun String.toBrowseRowConfig(): BrowseRowConfig? =
    runCatching {
        browseRowConfigAdapter.fromJson(java.net.URLDecoder.decode(this, "UTF-8"))
    }.getOrNull()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    hasTmdbAuth: Boolean,
    hasTraktAuth: Boolean,
    hasRdAuth: Boolean,
    onGoToSettings: () -> Unit,
) {
    val navController = rememberNavController()
    val topDests = buildList {
        add(TopDest.Movies)
        add(TopDest.TV)
        if (hasTmdbAuth) add(TopDest.TmdbLists)
        if (hasTraktAuth) add(TopDest.Trakt)
        if (hasRdAuth) add(TopDest.RealDebrid)
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Movies and TV rely on this shared top bar (for search + settings). Every other
    // destination renders its own top bar / back affordance, so we hide this one to
    // avoid stacking two app bars (which previously left a large gap at the top).
    val showHomeBar = currentRoute == "movies" || currentRoute == "tv"

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        // Bars handle their own status/navigation-bar insets; don't double-pad content.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            if (showHomeBar) {
                TopAppBar(
                    title = {
                        // Follow the applied app theme (user-selectable), not the OS setting
                        val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
                        val logoRes = if (isDark) R.drawable.logo_dark else R.drawable.logo_light
                        Image(
                            painter = painterResource(logoRes),
                            contentDescription = "FenLight+",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.height(28.dp),
                        )
                    },
                    actions = {
                        // Show search icon when on movies or tv top-level destinations
                        if (currentRoute == "movies") {
                            IconButton(onClick = { navController.navigate("movie_search") }) {
                                Icon(Icons.Default.Search, contentDescription = "Search Movies")
                            }
                        }
                        if (currentRoute == "tv") {
                            IconButton(onClick = { navController.navigate("tv_search") }) {
                                Icon(Icons.Default.Search, contentDescription = "Search TV")
                            }
                        }
                        IconButton(onClick = onGoToSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    },
                    scrollBehavior = scrollBehavior,
                )
            }
        },
        bottomBar = {
            NavigationBar {
                val currentDest = navBackStackEntry?.destination
                topDests.forEach { dest ->
                    NavigationBarItem(
                        icon = {
                            Image(
                                painter = painterResource(dest.iconRes),
                                contentDescription = dest.label,
                                modifier = Modifier.size(24.dp),
                                colorFilter = ColorFilter.tint(LocalContentColor.current),
                            )
                        },
                        label = { Text(dest.label) },
                        selected = currentDest?.hierarchy?.any { it.route == dest.route } == true,
                        onClick = {
                            navController.navigate(dest.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "movies",
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            composable("movies") {
                MediaBrowseScreen(
                    mediaType = MediaType.MOVIE,
                    onItemClick = { id -> navController.navigate("movie_detail/$id") },
                    onShowRecommendations = { id -> navController.navigate("related/movie/$id/recommendations") },
                    onShowSimilar = { id -> navController.navigate("related/movie/$id/similar") },
                    onSeeAll = { config -> navController.navigate("movie_browse_all/${config.toNavArg()}") },
                    vm = viewModel<MovieViewModel>(),
                )
            }
            composable("movie_search") {
                MediaSearchScreen(
                    mediaType = MediaType.MOVIE,
                    onBack = { navController.popBackStack() },
                    onItemClick = { id -> navController.navigate("movie_detail/$id") },
                    onShowRecommendations = { id -> navController.navigate("related/movie/$id/recommendations") },
                    onShowSimilar = { id -> navController.navigate("related/movie/$id/similar") },
                    vm = viewModel<MovieSearchViewModel>(),
                )
            }
            composable("movie_browse_all/{rowConfigJson}") { back ->
                val json = back.arguments?.getString("rowConfigJson") ?: return@composable
                val config = json.toBrowseRowConfig() ?: return@composable
                BrowseAllScreen(
                    rowConfig = config,
                    mediaType = "movie",
                    onBack = { navController.popBackStack() },
                    onItemClick = { id -> navController.navigate("movie_detail/$id") },
                    onShowRecommendations = { id -> navController.navigate("related/movie/$id/recommendations") },
                    onShowSimilar = { id -> navController.navigate("related/movie/$id/similar") },
                )
            }
            composable("movie_detail/{id}") { back ->
                val id = back.arguments?.getString("id")?.toIntOrNull() ?: return@composable
                MovieDetailScreen(
                    tmdbId = id,
                    onBack = { navController.popBackStack() },
                    onPersonClick = { personId -> navController.navigate("person/$personId") },
                    onMovieClick = { movieId -> navController.navigate("movie_detail/$movieId") },
                )
            }
            composable("tv") {
                MediaBrowseScreen(
                    mediaType = MediaType.TV,
                    onItemClick = { id -> navController.navigate("tv_detail/$id") },
                    onShowRecommendations = { id -> navController.navigate("related/tv/$id/recommendations") },
                    onShowSimilar = { id -> navController.navigate("related/tv/$id/similar") },
                    onSeeAll = { config -> navController.navigate("tv_browse_all/${config.toNavArg()}") },
                    vm = viewModel<TvViewModel>(),
                )
            }
            composable("tv_search") {
                MediaSearchScreen(
                    mediaType = MediaType.TV,
                    onBack = { navController.popBackStack() },
                    onItemClick = { id -> navController.navigate("tv_detail/$id") },
                    onShowRecommendations = { id -> navController.navigate("related/tv/$id/recommendations") },
                    onShowSimilar = { id -> navController.navigate("related/tv/$id/similar") },
                    vm = viewModel<TvSearchViewModel>(),
                )
            }
            composable("tv_browse_all/{rowConfigJson}") { back ->
                val json = back.arguments?.getString("rowConfigJson") ?: return@composable
                val config = json.toBrowseRowConfig() ?: return@composable
                BrowseAllScreen(
                    rowConfig = config,
                    mediaType = "tv",
                    onBack = { navController.popBackStack() },
                    onItemClick = { id -> navController.navigate("tv_detail/$id") },
                    onShowRecommendations = { id -> navController.navigate("related/tv/$id/recommendations") },
                    onShowSimilar = { id -> navController.navigate("related/tv/$id/similar") },
                )
            }
            composable("tv_detail/{id}") { back ->
                val id = back.arguments?.getString("id")?.toIntOrNull() ?: return@composable
                TvDetailScreen(
                    tmdbId = id,
                    onBack = { navController.popBackStack() },
                    onPersonClick = { personId -> navController.navigate("person/$personId") },
                    onEpisodeClick = { showId, season, episode ->
                        navController.navigate("episode_detail/$showId/$season/$episode")
                    },
                )
            }
            composable("episode_detail/{showId}/{season}/{episode}") { back ->
                val showId = back.arguments?.getString("showId")?.toIntOrNull() ?: return@composable
                val season = back.arguments?.getString("season")?.toIntOrNull() ?: return@composable
                val episode = back.arguments?.getString("episode")?.toIntOrNull() ?: return@composable
                EpisodeDetailScreen(
                    showId = showId,
                    season = season,
                    episodeNumber = episode,
                    onBack = { navController.popBackStack() },
                )
            }
            composable("tmdb_lists") {
                TmdbListsScreen(
                    onMovieClick = { id -> navController.navigate("movie_detail/$id") },
                    onShowClick = { id -> navController.navigate("tv_detail/$id") },
                    onGoToSettings = onGoToSettings,
                )
            }
            composable("related/{mediaType}/{id}/{kind}") { back ->
                val mediaType = back.arguments?.getString("mediaType") ?: return@composable
                val id = back.arguments?.getString("id")?.toIntOrNull() ?: return@composable
                val kind = back.arguments?.getString("kind") ?: return@composable
                RelatedScreen(
                    mediaType = mediaType,
                    mediaId = id,
                    kind = kind,
                    onBack = { navController.popBackStack() },
                    onItemClick = { itemId ->
                        navController.navigate(if (mediaType == "tv") "tv_detail/$itemId" else "movie_detail/$itemId")
                    },
                    onShowRecommendations = { itemId -> navController.navigate("related/$mediaType/$itemId/recommendations") },
                    onShowSimilar = { itemId -> navController.navigate("related/$mediaType/$itemId/similar") },
                )
            }
            composable("person/{id}") { back ->
                val id = back.arguments?.getString("id")?.toIntOrNull() ?: return@composable
                PersonScreen(
                    personId = id,
                    onBack = { navController.popBackStack() },
                    onCreditClick = { creditId, mediaType ->
                        navController.navigate(if (mediaType == "tv") "tv_detail/$creditId" else "movie_detail/$creditId")
                    },
                )
            }
            composable("trakt") {
                TraktScreen(
                    onMovieClick = { id -> navController.navigate("movie_detail/$id") },
                    onShowClick = { id -> navController.navigate("tv_detail/$id") },
                    onEpisodeClick = { showId, season, episode ->
                        navController.navigate("episode_detail/$showId/$season/$episode")
                    },
                    onGoToSettings = onGoToSettings,
                )
            }
            composable("rd") { RdScreen(onGoToSettings = onGoToSettings) }
        }
    }
}
