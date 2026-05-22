package com.fenlight.companion.ui.tvshows

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fenlight.companion.FenLightApp
import com.fenlight.companion.data.api.KodiRpc
import com.fenlight.companion.data.model.Season
import com.fenlight.companion.data.model.TvShow
import com.fenlight.companion.ui.components.PaginatedItem
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class TvBrowseTab { POPULAR, TRENDING, ON_THE_AIR, AIRING_TODAY, SEARCH, DISCOVER }

data class TvUiState(
    val tab: TvBrowseTab = TvBrowseTab.POPULAR,
    val items: List<PaginatedItem> = emptyList(),
    val isLoading: Boolean = false,
    val hasMore: Boolean = true,
    val page: Int = 0,
    val error: String? = null,
    val searchQuery: String = "",
    val sortBy: String = "popularity.desc",
    val selectedShow: TvShow? = null,
    val selectedSeason: Season? = null,
    val playMessage: String? = null,
)

class TvViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as FenLightApp
    private val _state = MutableStateFlow(TvUiState())
    val state: StateFlow<TvUiState> = _state.asStateFlow()

    init { loadNextPage() }

    fun selectTab(tab: TvBrowseTab) {
        if (_state.value.tab == tab) return
        _state.update { TvUiState(tab = tab) }
        loadNextPage()
    }

    fun onSearchQueryChange(q: String) {
        _state.update { it.copy(searchQuery = q, items = emptyList(), page = 0, hasMore = true) }
        if (q.length >= 2) loadNextPage()
    }

    fun onSortChange(sort: String) {
        _state.update { it.copy(sortBy = sort, items = emptyList(), page = 0, hasMore = true) }
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
                    TvBrowseTab.POPULAR -> app.tmdbApi.popularTv(nextPage)
                    TvBrowseTab.TRENDING -> app.tmdbApi.trendingTv(nextPage)
                    TvBrowseTab.ON_THE_AIR -> app.tmdbApi.onTheAirTv(nextPage)
                    TvBrowseTab.AIRING_TODAY -> app.tmdbApi.airingTodayTv(nextPage)
                    TvBrowseTab.SEARCH -> app.tmdbApi.searchTv(s.searchQuery, nextPage)
                    TvBrowseTab.DISCOVER -> {
                        val filters = buildMap<String, String> { put("sort_by", s.sortBy) }
                        app.tmdbApi.discoverTv(filters, nextPage)
                    }
                }
                val newItems = result.results.map { show ->
                    PaginatedItem(
                        id = show.id,
                        title = show.name,
                        posterUrl = FenLightApp.posterUrl(show.posterPath),
                        rating = show.voteAverage.takeIf { it > 0 },
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

    fun loadShowDetail(tmdbId: Int) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, selectedSeason = null) }
            try {
                val show = app.tmdbApi.tvDetail(tmdbId)
                _state.update { it.copy(isLoading = false, selectedShow = show) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun loadSeason(tmdbId: Int, seasonNumber: Int) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val season = app.tmdbApi.seasonDetail(tmdbId, seasonNumber)
                _state.update { it.copy(isLoading = false, selectedSeason = season) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun clearSelectedSeason() = _state.update { it.copy(selectedSeason = null) }
    fun clearSelectedShow() = _state.update { it.copy(selectedShow = null, selectedSeason = null) }

    fun playEpisode(showId: Int, showTitle: String, year: Int, season: Int, episode: Int) {
        viewModelScope.launch {
            try {
                val host = app.prefs.kodiHost.first()
                val port = app.prefs.kodiPort.first()
                val user = app.prefs.kodiUser.first()
                val pass = app.prefs.kodiPass.first()
                val kodi = KodiRpc(host, port, user, pass)
                kodi.playEpisodeViaFenLight(showId, showTitle, year, season, episode)
                _state.update { it.copy(playMessage = "Playing S${season}E${episode} of $showTitle on Kodi…") }
            } catch (e: Exception) {
                _state.update { it.copy(playMessage = "Failed: ${e.message}") }
            }
        }
    }

    fun clearPlayMessage() = _state.update { it.copy(playMessage = null) }
}
