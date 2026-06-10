package com.fenlight.companion.ui.browse

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fenlight.companion.FenLightApp
import com.fenlight.companion.data.model.*
import com.fenlight.companion.ui.components.PaginatedItem
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

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

                when (cfg.type) {
                    RowType.TMDB_LIST -> {
                        val listId = cfg.listId ?: return@launch
                        val detail = app.tmdbV4Api.listDetail(listId, nextPage)
                        val filterMediaType = if (mediaType == "tv") "tv" else "movie"
                        val newItems = detail.results
                            .filter { it.mediaType == filterMediaType }
                            .map { item ->
                                PaginatedItem(
                                    id = item.id,
                                    title = item.title ?: item.name ?: "",
                                    posterUrl = FenLightApp.posterUrl(item.posterPath),
                                    rating = null,
                                    backdropUrl = null,
                                )
                            }
                        _state.update {
                            it.copy(
                                isLoading = false,
                                items = (it.items + newItems).distinctBy { i -> i.id },
                                page = nextPage,
                                hasMore = nextPage < detail.totalPages,
                            )
                        }
                    }
                    RowType.TRAKT_LIST -> {
                        val slug = cfg.traktSlug ?: return@launch
                        val user = cfg.traktUser ?: "me"
                        val traktApi = app.authedTraktApi
                        val response = if (user == "me") traktApi.myListItems(slug, page = nextPage) else traktApi.listItems(user, slug, page = nextPage)
                        val body = response.body() ?: emptyList()
                        val newItems = if (mediaType == "tv") {
                            val shows = body.mapNotNull { item -> item.show?.takeIf { it.ids.tmdb != null } }
                            supervisorScope {
                                shows.map { s ->
                                    async {
                                        runCatching {
                                            val detail = app.tmdbApi.tvDetail(s.ids.tmdb!!, append = "")
                                            PaginatedItem(
                                                id = detail.id,
                                                title = detail.name,
                                                posterUrl = FenLightApp.posterUrl(detail.posterPath),
                                                rating = detail.voteAverage.takeIf { it > 0 },
                                                backdropUrl = FenLightApp.backdropUrl(detail.backdropPath),
                                            )
                                        }.getOrNull()
                                    }
                                }.awaitAll().filterNotNull()
                            }
                        } else {
                            val movies = body.mapNotNull { item -> item.movie?.takeIf { it.ids.tmdb != null } }
                            supervisorScope {
                                movies.map { m ->
                                    async {
                                        runCatching {
                                            val detail = app.tmdbApi.movieDetail(m.ids.tmdb!!, append = "")
                                            if (excludeAdult && detail.adult) return@runCatching null
                                            PaginatedItem(
                                                id = detail.id,
                                                title = detail.title,
                                                posterUrl = FenLightApp.posterUrl(detail.posterPath),
                                                rating = detail.voteAverage.takeIf { it > 0 },
                                                backdropUrl = FenLightApp.backdropUrl(detail.backdropPath),
                                            )
                                        }.getOrNull()
                                    }
                                }.awaitAll().filterNotNull()
                            }
                        }
                        val totalPages = response.headers()["X-Pagination-Page-Count"]?.toIntOrNull()
                            ?: if (body.size >= 50) nextPage + 1 else nextPage
                        _state.update {
                            it.copy(
                                isLoading = false,
                                items = (it.items + newItems).distinctBy { i -> i.id },
                                page = nextPage,
                                hasMore = nextPage < totalPages,
                            )
                        }
                    }
                    RowType.TRENDING -> {
                        val countries = region?.lowercase()
                        val (newItems, totalPages) = if (mediaType == "tv") {
                            val response = app.traktApi.showsTrending(nextPage, countries = countries)
                            val body = response.body() ?: emptyList()
                            val pages = response.headers()["X-Pagination-Page-Count"]?.toIntOrNull() ?: nextPage
                            val items = supervisorScope {
                                body.filter { it.show.ids.tmdb != null }.map { trending ->
                                    async {
                                        val tmdbId = trending.show.ids.tmdb!!
                                        runCatching {
                                            val detail = app.tmdbApi.tvDetail(tmdbId, append = "")
                                            PaginatedItem(
                                                id = detail.id, title = detail.name,
                                                posterUrl = FenLightApp.posterUrl(detail.posterPath),
                                                rating = detail.voteAverage.takeIf { it > 0 },
                                                backdropUrl = FenLightApp.backdropUrl(detail.backdropPath),
                                            )
                                        }.getOrNull()
                                    }
                                }.awaitAll().filterNotNull()
                            }
                            items to pages
                        } else {
                            val response = app.traktApi.moviesTrending(nextPage, countries = countries)
                            val body = response.body() ?: emptyList()
                            val pages = response.headers()["X-Pagination-Page-Count"]?.toIntOrNull() ?: nextPage
                            val items = supervisorScope {
                                body.filter { it.movie.ids.tmdb != null }.map { trending ->
                                    async {
                                        val tmdbId = trending.movie.ids.tmdb!!
                                        runCatching {
                                            val detail = app.tmdbApi.movieDetail(tmdbId, append = "")
                                            if (excludeAdult && detail.adult) return@runCatching null
                                            PaginatedItem(
                                                id = detail.id, title = detail.title,
                                                posterUrl = FenLightApp.posterUrl(detail.posterPath),
                                                rating = detail.voteAverage.takeIf { it > 0 },
                                                backdropUrl = FenLightApp.backdropUrl(detail.backdropPath),
                                            )
                                        }.getOrNull()
                                    }
                                }.awaitAll().filterNotNull()
                            }
                            items to pages
                        }
                        _state.update {
                            it.copy(
                                isLoading = false,
                                items = (it.items + newItems).distinctBy { i -> i.id },
                                page = nextPage,
                                hasMore = nextPage < totalPages,
                            )
                        }
                    }
                    else -> {
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
        else -> app.tmdbApi.popularMovies(page, region)  // TV-only / list types already handled
    }

    private suspend fun fetchTvPage(
        cfg: BrowseRowConfig,
        page: Int,
        region: String?,
        excludeAdult: Boolean,
    ) = when (cfg.type) {
        RowType.POPULAR -> app.tmdbApi.popularTv(page, region)
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
        else -> app.tmdbApi.popularTv(page, region)  // movie-only / list types already handled
    }
}
