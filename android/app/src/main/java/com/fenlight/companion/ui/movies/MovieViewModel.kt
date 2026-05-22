package com.fenlight.companion.ui.movies

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fenlight.companion.FenLightApp
import com.fenlight.companion.data.api.KodiRpc
import com.fenlight.companion.data.model.Movie
import com.fenlight.companion.data.model.TmdbList
import com.fenlight.companion.data.model.TmdbListItem
import com.fenlight.companion.ui.components.PaginatedItem
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class MovieBrowseTab { POPULAR, TRENDING, NOW_PLAYING, UPCOMING, SEARCH, DISCOVER, LISTS }

data class DiscoverFilters(
    val genreId: String = "",
    val year: String = "",
    val sortBy: String = "popularity.desc",
)

data class MovieUiState(
    val tab: MovieBrowseTab = MovieBrowseTab.POPULAR,
    val items: List<PaginatedItem> = emptyList(),
    val isLoading: Boolean = false,
    val hasMore: Boolean = true,
    val page: Int = 0,
    val error: String? = null,
    val searchQuery: String = "",
    val discoverFilters: DiscoverFilters = DiscoverFilters(),
    val selectedMovie: Movie? = null,
    val myLists: List<TmdbList> = emptyList(),
    val listItems: List<TmdbListItem> = emptyList(),
    val selectedListName: String = "",
    val playMessage: String? = null,
)

class MovieViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as FenLightApp
    private val _state = MutableStateFlow(MovieUiState())
    val state: StateFlow<MovieUiState> = _state.asStateFlow()

    init { loadNextPage() }

    fun selectTab(tab: MovieBrowseTab) {
        if (_state.value.tab == tab) return
        _state.update { MovieUiState(tab = tab) }
        if (tab == MovieBrowseTab.LISTS) loadMyLists() else loadNextPage()
    }

    fun onSearchQueryChange(q: String) {
        _state.update { it.copy(searchQuery = q, items = emptyList(), page = 0, hasMore = true) }
        if (q.length >= 2) loadNextPage()
    }

    fun onDiscoverFilterChange(filters: DiscoverFilters) {
        _state.update { it.copy(discoverFilters = filters, items = emptyList(), page = 0, hasMore = true) }
        loadNextPage()
    }

    fun loadNextPage() {
        val s = _state.value
        if (s.isLoading || !s.hasMore) return
        val nextPage = s.page + 1
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val result = when (s.tab) {
                    MovieBrowseTab.POPULAR -> app.tmdbApi.popularMovies(nextPage)
                    MovieBrowseTab.TRENDING -> app.tmdbApi.trendingMovies(nextPage)
                    MovieBrowseTab.NOW_PLAYING -> app.tmdbApi.nowPlayingMovies(nextPage)
                    MovieBrowseTab.UPCOMING -> app.tmdbApi.upcomingMovies(nextPage)
                    MovieBrowseTab.SEARCH -> app.tmdbApi.searchMovies(s.searchQuery, nextPage)
                    MovieBrowseTab.DISCOVER -> {
                        val f = s.discoverFilters
                        val filters = buildMap<String, String> {
                            if (f.genreId.isNotBlank()) put("with_genres", f.genreId)
                            if (f.year.isNotBlank()) put("primary_release_year", f.year)
                            put("sort_by", f.sortBy)
                        }
                        app.tmdbApi.discoverMovies(filters, nextPage)
                    }
                    MovieBrowseTab.LISTS -> return@launch
                }
                val newItems = result.results.map { m ->
                    PaginatedItem(
                        id = m.id,
                        title = m.title,
                        posterUrl = FenLightApp.posterUrl(m.posterPath),
                        rating = m.voteAverage.takeIf { it > 0 },
                    )
                }
                _state.update {
                    it.copy(
                        isLoading = false,
                        items = it.items + newItems,
                        page = nextPage,
                        hasMore = nextPage < result.totalPages,
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
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

    fun loadMyLists() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val accessToken = app.prefs.tmdbAccessToken.first()
                val accountId = app.prefs.tmdbAccountId.first()
                val v4 = app.buildTmdbV4Api(accessToken)
                val lists = v4.accountLists(accountId)
                _state.update { it.copy(isLoading = false, myLists = lists.results, listItems = emptyList()) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun loadListItems(listId: Int, listName: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, selectedListName = listName) }
            try {
                val accessToken = app.prefs.tmdbAccessToken.first()
                val v4 = app.buildTmdbV4Api(accessToken)
                val detail = v4.listDetail(listId)
                _state.update { it.copy(isLoading = false, listItems = detail.results) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

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
