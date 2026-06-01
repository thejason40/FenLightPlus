package com.fenlight.companion.ui.browse

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fenlight.companion.FenLightApp
import com.fenlight.companion.data.model.BrowseRowConfig
import com.fenlight.companion.data.model.DiscoverFilters
import com.fenlight.companion.data.model.RowType
import com.fenlight.companion.ui.components.PaginatedItem
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class BrowseAllUiState(
    val title: String = "",
    val items: List<PaginatedItem> = emptyList(),
    val isLoading: Boolean = false,
    val hasMore: Boolean = true,
    val page: Int = 0,
    val error: String? = null,
)

class BrowseAllViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as FenLightApp
    private val _state = MutableStateFlow(BrowseAllUiState())
    val state: StateFlow<BrowseAllUiState> = _state.asStateFlow()

    private var config: BrowseRowConfig? = null
    private var mediaType: String = "movie"
    private var initialised = false

    fun init(rowConfig: BrowseRowConfig, mediaType: String) {
        if (initialised) return
        initialised = true
        this.config = rowConfig
        this.mediaType = mediaType
        _state.update { it.copy(title = rowConfig.label) }
        loadNextPage()
    }

    fun loadNextPage() {
        val s = _state.value
        val cfg = config ?: return
        if (s.isLoading || !s.hasMore) return
        val nextPage = s.page + 1
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val region = app.prefs.region.first().takeIf { it.isNotBlank() }
                val excludeAdult = app.prefs.excludeAdult.first()

                if (mediaType == "tv") {
                    val result = fetchTvPage(cfg, nextPage, region, excludeAdult)
                    val newItems = result.results.map { show ->
                        PaginatedItem(
                            id = show.id, title = show.name,
                            posterUrl = FenLightApp.posterUrl(show.posterPath),
                            rating = show.voteAverage.takeIf { it > 0 },
                            backdropUrl = FenLightApp.backdropUrl(show.backdropPath),
                        )
                    }
                    _state.update {
                        it.copy(
                            isLoading = false,
                            items = (it.items + newItems).distinctBy { i -> i.id },
                            page = nextPage,
                            hasMore = nextPage < result.totalPages,
                        )
                    }
                } else {
                    val result = fetchMoviePage(cfg, nextPage, region, excludeAdult)
                    val newItems = result.results
                        .filter { !excludeAdult || !it.adult }
                        .map { m ->
                            PaginatedItem(
                                id = m.id, title = m.title,
                                posterUrl = FenLightApp.posterUrl(m.posterPath),
                                rating = m.voteAverage.takeIf { it > 0 },
                                backdropUrl = FenLightApp.backdropUrl(m.backdropPath),
                            )
                        }
                    _state.update {
                        it.copy(
                            isLoading = false,
                            items = (it.items + newItems).distinctBy { i -> i.id },
                            page = nextPage,
                            hasMore = nextPage < result.totalPages,
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private suspend fun fetchMoviePage(
        cfg: BrowseRowConfig,
        page: Int,
        region: String?,
        excludeAdult: Boolean,
    ) = when (cfg.type) {
        RowType.POPULAR -> app.tmdbApi.popularMovies(page, region)
        RowType.TRENDING -> app.tmdbApi.trendingMovies(page, region)
        RowType.NOW_PLAYING -> app.tmdbApi.nowPlayingMovies(page, region)
        RowType.UPCOMING -> app.tmdbApi.upcomingMovies(page, region)
        RowType.TOP_RATED -> app.tmdbApi.topRatedMovies(page, region)
        RowType.CUSTOM -> {
            val f = cfg.filters ?: DiscoverFilters()
            val filters = buildMap<String, String> {
                if (f.genreId.isNotBlank()) put("with_genres", f.genreId)
                if (f.year.isNotBlank()) put("primary_release_year", f.year)
                put("sort_by", f.sortBy)
                if (f.minRating.isNotBlank()) { put("vote_average.gte", f.minRating); put("vote_count.gte", "20") }
                if (f.language.isNotBlank()) put("with_original_language", f.language)
                if (f.watchProviderId.isNotBlank()) {
                    put("with_watch_providers", f.watchProviderId)
                    if (region != null) put("watch_region", region)
                }
                put("include_adult", (!excludeAdult).toString())
            }
            app.tmdbApi.discoverMovies(filters, page, region)
        }
        RowType.ON_THE_AIR, RowType.AIRING_TODAY -> app.tmdbApi.popularMovies(page, region)
    }

    private suspend fun fetchTvPage(
        cfg: BrowseRowConfig,
        page: Int,
        region: String?,
        excludeAdult: Boolean,
    ) = when (cfg.type) {
        RowType.POPULAR -> app.tmdbApi.popularTv(page, region)
        RowType.TRENDING -> app.tmdbApi.trendingTv(page, region)
        RowType.ON_THE_AIR -> app.tmdbApi.onTheAirTv(page, region)
        RowType.AIRING_TODAY -> app.tmdbApi.airingTodayTv(page, region)
        RowType.TOP_RATED -> app.tmdbApi.topRatedTv(page, region)
        RowType.CUSTOM -> {
            val f = cfg.filters ?: DiscoverFilters()
            val filters = buildMap<String, String> {
                if (f.genreId.isNotBlank()) put("with_genres", f.genreId)
                if (f.year.isNotBlank()) put("first_air_date_year", f.year)
                put("sort_by", f.sortBy)
                if (f.minRating.isNotBlank()) { put("vote_average.gte", f.minRating); put("vote_count.gte", "20") }
                if (f.language.isNotBlank()) put("with_original_language", f.language)
                if (f.tvStatus.isNotBlank()) put("with_status", f.tvStatus)
                if (f.tvType.isNotBlank()) put("with_type", f.tvType)
                if (f.watchProviderId.isNotBlank()) {
                    put("with_watch_providers", f.watchProviderId)
                    if (region != null) put("watch_region", region)
                }
                put("include_adult", (!excludeAdult).toString())
            }
            app.tmdbApi.discoverTv(filters, page, region)
        }
        RowType.NOW_PLAYING, RowType.UPCOMING -> app.tmdbApi.popularTv(page, region)
    }
}
