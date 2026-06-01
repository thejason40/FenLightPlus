package com.fenlight.companion.ui.home

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.fenlight.companion.R
import com.fenlight.companion.ui.movies.MovieBrowseScreen
import com.fenlight.companion.ui.movies.MovieDetailScreen
import com.fenlight.companion.ui.realdebrid.RdScreen
import com.fenlight.companion.ui.related.RelatedScreen
import com.fenlight.companion.ui.tmdb.TmdbListsScreen
import com.fenlight.companion.ui.trakt.TraktScreen
import com.fenlight.companion.ui.tvshows.TvBrowseScreen
import com.fenlight.companion.ui.tvshows.TvDetailScreen

private sealed class TopDest(val route: String, val label: String, @DrawableRes val iconRes: Int) {
    object Movies : TopDest("movies", "Movies", R.drawable.icon_movies)
    object TV : TopDest("tv", "TV Shows", R.drawable.icon_tv)
    object TmdbLists : TopDest("tmdb_lists", "Lists", R.drawable.icon_tmdb)
    object Trakt : TopDest("trakt", "Trakt", R.drawable.icon_trakt)
    object RealDebrid : TopDest("rd", "Debrid", R.drawable.icon_realdebrid)
}

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

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("FenLight+ Companion") },
                actions = {
                    IconButton(onClick = onGoToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDest = navBackStackEntry?.destination
                topDests.forEach { dest ->
                    NavigationBarItem(
                        icon = {
                            Image(
                                painter = painterResource(dest.iconRes),
                                contentDescription = dest.label,
                                modifier = Modifier.size(24.dp),
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
                MovieBrowseScreen(
                    onMovieClick = { id -> navController.navigate("movie_detail/$id") },
                    onShowRecommendations = { id -> navController.navigate("related/movie/$id/recommendations") },
                    onShowSimilar = { id -> navController.navigate("related/movie/$id/similar") },
                )
            }
            composable("movie_detail/{id}") { back ->
                val id = back.arguments?.getString("id")?.toIntOrNull() ?: return@composable
                MovieDetailScreen(tmdbId = id, onBack = { navController.popBackStack() })
            }
            composable("tv") {
                TvBrowseScreen(
                    onShowClick = { id -> navController.navigate("tv_detail/$id") },
                    onShowRecommendations = { id -> navController.navigate("related/tv/$id/recommendations") },
                    onShowSimilar = { id -> navController.navigate("related/tv/$id/similar") },
                )
            }
            composable("tv_detail/{id}") { back ->
                val id = back.arguments?.getString("id")?.toIntOrNull() ?: return@composable
                TvDetailScreen(tmdbId = id, onBack = { navController.popBackStack() })
            }
            composable("tmdb_lists") {
                TmdbListsScreen(
                    onMovieClick = { id -> navController.navigate("movie_detail/$id") },
                    onShowClick = { id -> navController.navigate("tv_detail/$id") },
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
            composable("trakt") { TraktScreen() }
            composable("rd") { RdScreen() }
        }
    }
}
