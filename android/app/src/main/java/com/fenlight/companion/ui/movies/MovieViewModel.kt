package com.fenlight.companion.ui.movies

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fenlight.companion.FenLightApp
import com.fenlight.companion.data.api.KodiRpc
import com.fenlight.companion.data.model.Genre
import com.fenlight.companion.data.model.Movie
import com.fenlight.companion.ui.components.PaginatedItem
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class MovieBrowseTab { POPULAR, TRENDING, NOW_PLAYING, UPCOMING, SEARCH, DISCOVER }

data class DiscoverFilters(
    val genreId: String = "",
    val year: String = "",
    val sortBy: String = "popularity.desc",
    val minRating: String = "",
    val language: String = "",
)

data class MovieUiState(
    val tab: MovieBrowseTab = MovieBrowseTab.POPULAR,
    val items: List<PaginatedItem> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val hasMore: Boolean = true,
    val page: Int = 0,
    val error: String? = null,
    val searchQuery: String = "",
    val discoverFilters: DiscoverFilters = DiscoverFilters(),
    val discoverShowForm: Boolean = true,
    val movieGenres: List<Genre> = emptyList(),
    val selectedMovie: Movie? = null,
    val playMessage: String? = null,
)

class MovieViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as FenLightApp
    private val _state = MutableStateFlow(MovieUiState())
    val state: StateFlow<MovieUiState> = _state.asStateFlow()

    private companion object { const val CACHE_MS = 24 * 60 * 60 * 1000L }
    private data class CachedTab(val items: List<PaginatedItem>, val page: Int, val hasMore: Boolean, val fetchedAt: Long)
    private val tabCache = mutableMapOf<MovieBrowseTab, CachedTab>()

    init {
        loadMovieGenres()
        val cached = tabCache[MovieBrowseTab.POPULAR]
        if (cached == null || System.currentTimeMillis() - cached.fetchedAt > CACHE_MS) {
            loadNextPage()
        } else {
            _state.update { it.copy(items = cached.items, page = cached.page, hasMore = cached.hasMore) }
        }
    }

    fun selectTab(tab: MovieBrowseTab) {
        if (_state.value.tab == tab) return
        // Save current tab to cache
        val cur = _state.value
        if (cur.items.isNotEmpty()) tabCache[cur.tab] = CachedTab(cur.items, cur.page, cur.hasMore, tabCache[cur.tab]?.fetchedAt ?: System.currentTimeMillis())
        if (tab == MovieBrowseTab.DISCOVER) {
            _state.update { MovieUiState(tab = tab, discoverShowForm = true, movieGenres = it.movieGenres) }
            return
        }
        val cached = tabCache[tab]
        if (cached != null && System.currentTimeMillis() - cached.fetchedAt < CACHE_MS) {
            _state.update { MovieUiState(tab = tab, items = cached.items, page = cached.page, hasMore = cached.hasMore, movieGenres = it.movieGenres) }
        } else {
            _state.update { MovieUiState(tab = tab, movieGenres = it.movieGenres) }
            loadNextPage()
        }
    }

    fun onSearchQueryChange(q: String) {
        _state.update { it.copy(searchQuery = q, items = emptyList(), page = 0, hasMore = true) }
        if (q.length >= 2) loadNextPage()
    }

    fun onDiscoverFilterChange(filters: DiscoverFilters) {
        _state.update { it.copy(discoverFilters = filters) }
    }

    fun runDiscover() {
        _state.update { it.copy(items = emptyList(), page = 0, hasMore = true, discoverShowForm = false) }
        loadNextPage()
    }

    fun showDiscoverForm() {
        _state.update { it.copy(discoverShowForm = true, items = emptyList(), page = 0, hasMore = true) }
    }

    fun refresh() {
        tabCache.remove(_state.value.tab)
        _state.update { it.copy(isRefreshing = true, items = emptyList(), page = 0, hasMore = true, error = null) }
        loadNextPage()
    }

    fun loadNextPage() {
        val s = _state.value
        if (s.isLoading || !s.hasMore) return
        val nextPage = s.page + 1
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val region = app.prefs.region.first().takeIf { it.isNotBlank() }
                val excludeAdult = app.prefs.excludeAdult.first()
                val result = when (s.tab) {
                    MovieBrowseTab.POPULAR -> app.tmdbApi.popularMovies(nextPage, region = region)
                    MovieBrowseTab.TRENDING -> app.tmdbApi.trendingMovies(nextPage, region = region)
                    MovieBrowseTab.NOW_PLAYING -> app.tmdbApi.nowPlayingMovies(nextPage, region = region)
                    MovieBrowseTab.UPCOMING -> app.tmdbApi.upcomingMovies(nextPage, region = region)
                    MovieBrowseTab.SEARCH -> app.tmdbApi.searchMovies(s.searchQuery, nextPage, includeAdult = !excludeAdult)
                    MovieBrowseTab.DISCOVER -> {
                        val f = s.discoverFilters
                        val filters = buildMap<String, String> {
                            if (f.genreId.isNotBlank()) put("with_genres", f.genreId)
                            if (f.year.isNotBlank()) put("primary_release_year", f.year)
                            put("sort_by", f.sortBy)
                            if (f.minRating.isNotBlank()) {
                                put("vote_average.gte", f.minRating)
                                put("vote_count.gte", "20")
                            }
                            if (f.language.isNotBlank()) put("with_original_language", f.language)
                            put("include_adult", (!excludeAdult).toString())
                        }
                        app.tmdbApi.discoverMovies(filters, nextPage, region = region)
                    }
                }
                val newItems = result.results
                    .filter { !excludeAdult || !it.adult }
                    .map { m ->
                    PaginatedItem(
                        id = m.id,
                        title = m.title,
                        posterUrl = FenLightApp.posterUrl(m.posterPath),
                        rating = m.voteAverage.takeIf { it > 0 },
                        backdropUrl = FenLightApp.backdropUrl(m.backdropPath),
                    )
                }
                _state.update {
                    val updated = it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        items = (it.items + newItems).distinctBy { item -> item.id },
                        page = nextPage,
                        hasMore = nextPage < result.totalPages,
                    )
                    if (nextPage == 1) tabCache[updated.tab] = CachedTab(updated.items, updated.page, updated.hasMore, System.currentTimeMillis())
                    updated
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, isRefreshing = false, error = e.message) }
            }
        }
    }

    private fun loadMovieGenres() {
        viewModelScope.launch {
            try {
                val genres = app.tmdbApi.movieGenres()
                _state.update { it.copy(movieGenres = genres.genres) }
            } catch (_: Exception) {}
        }
    }

    fun loadMovieDetail(tmdbId: Int) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val movie = app.tmdbApi.movieDetail(tmdbId)
                _state.update { it.copy(isLoading = false, selectedMovie = movie) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun clearSelectedMovie() = _state.update { it.copy(selectedMovie = null) }

    fun playMovie(movie: Movie) {
        viewModelScope.launch {
            try {
                val host = app.prefs.kodiHost.first()
                val port = app.prefs.kodiPort.first()
                val user = app.prefs.kodiUser.first()
                val pass = app.prefs.kodiPass.first()
                val kodi = KodiRpc(host, port, user, pass)
                val year = movie.releaseDate?.take(4)?.toIntOrNull() ?: 0
                kodi.playMovieViaFenLight(movie.id, movie.title, year)
                _state.update { it.copy(playMessage = "Playing ${movie.title} on Kodi…") }
            } catch (e: Exception) {
                _state.update { it.copy(playMessage = "Failed to send play command: ${e.message}") }
            }
        }
    }

    fun clearPlayMessage() = _state.update { it.copy(playMessage = null) }
}
