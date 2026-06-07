package com.fenlight.companion.ui.movies

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fenlight.companion.FenLightApp
import com.fenlight.companion.data.api.KodiRpc
import com.fenlight.companion.data.model.*
import com.fenlight.companion.ui.components.PaginatedItem
import com.squareup.moshi.Types
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.UUID

// Simple metadata for list picker in AddBrowseRowSheet
data class TraktListEntry(val slug: String, val user: String, val name: String)

data class BrowseRowState(
    val config: BrowseRowConfig,
    val items: List<PaginatedItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

data class MovieHomeUiState(
    val rows: List<BrowseRowState> = emptyList(),
    val isRefreshing: Boolean = false,
    val showAddRowSheet: Boolean = false,
    val pendingRowType: RowType = RowType.CUSTOM,
    val pendingRowLabel: String = "",
    val pendingRowFilters: DiscoverFilters = DiscoverFilters(),
    val pendingListId: Int? = null,
    val pendingTraktSlug: String? = null,
    val pendingTraktUser: String? = null,
    val addRowError: String? = null,
    val genres: List<Genre> = emptyList(),
    val watchProviders: List<WatchProvider> = emptyList(),
    val availableTmdbLists: List<TmdbList> = emptyList(),
    val availableTraktLists: List<TraktListEntry> = emptyList(),
    val selectedMovie: Movie? = null,
    val playMessage: String? = null,
)

class MovieViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as FenLightApp
    private val _state = MutableStateFlow(MovieHomeUiState())
    val state: StateFlow<MovieHomeUiState> = _state.asStateFlow()

    private companion object {
        const val CACHE_MS = 24 * 60 * 60 * 1000L
        const val MAX_CUSTOM_ROWS = 5
        const val FIXED_POPULAR = "fixed_popular"
        const val FIXED_TRENDING = "fixed_trending"
    }

    private data class CachedRow(val items: List<PaginatedItem>, val fetchedAt: Long)
    private val rowCache = mutableMapOf<String, CachedRow>()

    private val browseRowListType = Types.newParameterizedType(List::class.java, BrowseRowConfig::class.java)
    private val browseRowListAdapter = app.moshi.adapter<List<BrowseRowConfig>>(browseRowListType)

    init {
        loadGenres()
        loadRowConfigs()
    }

    private fun loadGenres() {
        viewModelScope.launch {
            try {
                val genres = app.tmdbApi.movieGenres()
                _state.update { it.copy(genres = genres.genres) }
            } catch (_: Exception) {}
        }
    }

    private fun loadRowConfigs() {
        viewModelScope.launch {
            val json = app.prefs.movieBrowseRowsJson.first()
            val customConfigs = if (json.isNotBlank()) {
                runCatching { browseRowListAdapter.fromJson(json) ?: emptyList() }.getOrDefault(emptyList())
            } else emptyList()

            val fixedRows = listOf(
                BrowseRowConfig(FIXED_POPULAR, "Popular", RowType.POPULAR),
                BrowseRowConfig(FIXED_TRENDING, "Trending", RowType.TRENDING),
            )
            val allConfigs = fixedRows + customConfigs
            _state.update {
                it.copy(rows = allConfigs.map { c -> BrowseRowState(c, isLoading = true) })
            }
            fetchAllRows(allConfigs)
        }
    }

    private fun fetchAllRows(configs: List<BrowseRowConfig>) {
        viewModelScope.launch {
            val region = app.prefs.region.first().takeIf { it.isNotBlank() }
            val excludeAdult = app.prefs.excludeAdult.first()
            supervisorScope {
                configs.map { config ->
                    async {
                        val cached = rowCache[config.id]
                        if (cached != null && System.currentTimeMillis() - cached.fetchedAt < CACHE_MS) {
                            updateRow(config.id) { it.copy(items = cached.items, isLoading = false) }
                            return@async
                        }
                        try {
                            val items = fetchPage1Items(config, region, excludeAdult)
                            rowCache[config.id] = CachedRow(items, System.currentTimeMillis())
                            updateRow(config.id) { it.copy(items = items, isLoading = false, error = null) }
                        } catch (e: Exception) {
                            updateRow(config.id) { it.copy(isLoading = false, error = e.message) }
                        }
                    }
                }.awaitAll()
            }
            _state.update { it.copy(isRefreshing = false) }
        }
    }

    private suspend fun fetchPage1Items(config: BrowseRowConfig, region: String?, excludeAdult: Boolean): List<PaginatedItem> {
        return when (config.type) {
            RowType.TMDB_LIST -> {
                val token = app.prefs.tmdbAccessToken.first()
                val listId = config.listId ?: return emptyList()
                val detail = app.buildTmdbV4Api(token).listDetail(listId, 1)
                detail.results
                    .filter { it.mediaType == "movie" }
                    .map { item ->
                        PaginatedItem(
                            id = item.id,
                            title = item.title ?: item.name ?: "",
                            posterUrl = FenLightApp.posterUrl(item.posterPath),
                            rating = null,
                            backdropUrl = null,
                        )
                    }
            }
            RowType.TRAKT_LIST -> {
                val token = app.getValidTraktAccessToken()
                val slug = config.traktSlug ?: return emptyList()
                val user = config.traktUser ?: "me"
                val traktApi = app.buildAuthedTraktApi(token)
                val response = if (user == "me") traktApi.myListItems(slug, page = 1) else traktApi.listItems(user, slug, page = 1)
                val movies = (response.body() ?: emptyList()).mapNotNull { item -> item.movie?.takeIf { it.ids.tmdb != null } }
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
            RowType.TRENDING -> fetchTraktTrendingMovies(1, region, excludeAdult)
            else -> {
                val result = when (config.type) {
                    RowType.POPULAR -> app.tmdbApi.popularMovies(1, region)
                    RowType.NOW_PLAYING -> app.tmdbApi.nowPlayingMovies(1, region)
                    RowType.UPCOMING -> app.tmdbApi.upcomingMovies(1, region)
                    RowType.TOP_RATED -> app.tmdbApi.topRatedMovies(1, region)
                    RowType.CUSTOM -> {
                        val f = config.filters ?: DiscoverFilters()
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
                        app.tmdbApi.discoverMovies(filters, 1, region)
                    }
                    else -> app.tmdbApi.popularMovies(1, region)  // TV-only fallback
                }
                result.results.filter { !excludeAdult || !it.adult }.map { m ->
                    PaginatedItem(
                        id = m.id, title = m.title,
                        posterUrl = FenLightApp.posterUrl(m.posterPath),
                        rating = m.voteAverage.takeIf { it > 0 },
                        backdropUrl = FenLightApp.backdropUrl(m.backdropPath),
                    )
                }
            }
        }
    }

    private suspend fun fetchTraktTrendingMovies(page: Int, region: String?, excludeAdult: Boolean): List<PaginatedItem> {
        val countries = region?.lowercase()
        val response = app.traktApi.moviesTrending(page, countries = countries)
        val traktItems = response.body() ?: emptyList()
        return supervisorScope {
            traktItems
                .filter { it.movie.ids.tmdb != null }
                .map { trending ->
                    async {
                        val tmdbId = trending.movie.ids.tmdb!!
                        runCatching {
                            val detail = app.tmdbApi.movieDetail(tmdbId, append = "")
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

    private fun updateRow(rowId: String, transform: (BrowseRowState) -> BrowseRowState) {
        _state.update { s -> s.copy(rows = s.rows.map { r -> if (r.config.id == rowId) transform(r) else r }) }
    }

    fun refresh() {
        rowCache.clear()
        val configs = _state.value.rows.map { it.config }
        _state.update { s -> s.copy(isRefreshing = true, rows = s.rows.map { it.copy(items = emptyList(), isLoading = true, error = null) }) }
        fetchAllRows(configs)
    }

    fun retryRow(rowId: String) {
        val config = _state.value.rows.firstOrNull { it.config.id == rowId }?.config ?: return
        rowCache.remove(rowId)
        updateRow(rowId) { it.copy(isLoading = true, error = null) }
        fetchAllRows(listOf(config))
    }

    fun showAddRowSheet() {
        _state.update {
            it.copy(
                showAddRowSheet = true, addRowError = null,
                pendingRowType = RowType.CUSTOM, pendingRowLabel = "", pendingRowFilters = DiscoverFilters(),
                pendingListId = null, pendingTraktSlug = null, pendingTraktUser = null,
            )
        }
        viewModelScope.launch {
            val region = app.prefs.region.first().takeIf { it.isNotBlank() }
            supervisorScope {
                async {
                    try {
                        val providers = app.tmdbApi.movieWatchProviders(region).results.sortedBy { it.displayPriority }
                        _state.update { it.copy(watchProviders = providers) }
                    } catch (_: Exception) {}
                }
                async {
                    try {
                        val token = app.prefs.tmdbAccessToken.first()
                        val accountId = app.prefs.tmdbAccountId.first()
                        if (token.isNotBlank() && accountId.isNotBlank()) {
                            val lists = app.buildTmdbV4Api(token).accountLists(accountId).results
                            _state.update { it.copy(availableTmdbLists = lists) }
                        }
                    } catch (_: Exception) {}
                }
                async {
                    try {
                        val token = runCatching { app.getValidTraktAccessToken() }.getOrNull() ?: return@async
                        val traktApi = app.buildAuthedTraktApi(token)
                        val myLists = traktApi.myLists().map { TraktListEntry(it.slug, "me", it.name) }
                        val likedLists = (traktApi.likedLists().body() ?: emptyList()).map {
                            TraktListEntry(it.list.slug, it.list.user?.username ?: "me", it.list.name)
                        }
                        _state.update { it.copy(availableTraktLists = myLists + likedLists) }
                    } catch (_: Exception) {}
                }
            }
        }
    }

    fun dismissAddRowSheet() = _state.update { it.copy(showAddRowSheet = false) }
    fun onPendingRowTypeChange(type: RowType) = _state.update { it.copy(pendingRowType = type) }
    fun onPendingRowLabelChange(label: String) = _state.update { it.copy(pendingRowLabel = label) }
    fun onPendingRowFiltersChange(f: DiscoverFilters) = _state.update { it.copy(pendingRowFilters = f) }
    fun onPendingListIdChange(id: Int?) = _state.update { it.copy(pendingListId = id) }
    fun onPendingTraktListChange(slug: String?, user: String?) = _state.update { it.copy(pendingTraktSlug = slug, pendingTraktUser = user) }

    fun addCustomRow() {
        val customRows = _state.value.rows.filter { it.config.id !in listOf(FIXED_POPULAR, FIXED_TRENDING) }
        if (customRows.size >= MAX_CUSTOM_ROWS) {
            _state.update { it.copy(addRowError = "Maximum $MAX_CUSTOM_ROWS custom rows reached") }
            return
        }
        val s = _state.value
        val newConfig = when (s.pendingRowType) {
            RowType.TMDB_LIST -> {
                val listId = s.pendingListId ?: run {
                    _state.update { it.copy(addRowError = "Please select a list") }
                    return
                }
                val listName = s.availableTmdbLists.firstOrNull { it.id == listId }?.name ?: "My List"
                BrowseRowConfig(id = UUID.randomUUID().toString(), label = s.pendingRowLabel.ifBlank { listName }, type = RowType.TMDB_LIST, listId = listId)
            }
            RowType.TRAKT_LIST -> {
                val slug = s.pendingTraktSlug ?: run {
                    _state.update { it.copy(addRowError = "Please select a list") }
                    return
                }
                val listName = s.availableTraktLists.firstOrNull { it.slug == slug && it.user == s.pendingTraktUser }?.name ?: "Trakt List"
                BrowseRowConfig(id = UUID.randomUUID().toString(), label = s.pendingRowLabel.ifBlank { listName }, type = RowType.TRAKT_LIST, traktSlug = slug, traktUser = s.pendingTraktUser)
            }
            else -> {
                val label = s.pendingRowLabel.ifBlank { defaultLabel(s.pendingRowType, s.pendingRowFilters, s.genres) }
                BrowseRowConfig(
                    id = UUID.randomUUID().toString(), label = label, type = s.pendingRowType,
                    filters = if (s.pendingRowType == RowType.CUSTOM) s.pendingRowFilters else null,
                )
            }
        }
        val updatedRows = _state.value.rows + BrowseRowState(config = newConfig, isLoading = true)
        _state.update { it.copy(rows = updatedRows, showAddRowSheet = false) }
        persistCustomRows(updatedRows)
        fetchAllRows(listOf(newConfig))
    }

    fun removeRow(rowId: String) {
        if (rowId in listOf(FIXED_POPULAR, FIXED_TRENDING)) return
        rowCache.remove(rowId)
        val updatedRows = _state.value.rows.filter { it.config.id != rowId }
        _state.update { it.copy(rows = updatedRows) }
        persistCustomRows(updatedRows)
    }

    private fun persistCustomRows(rows: List<BrowseRowState>) {
        val customConfigs = rows.filter { it.config.id !in listOf(FIXED_POPULAR, FIXED_TRENDING) }.map { it.config }
        viewModelScope.launch {
            val json = browseRowListAdapter.toJson(customConfigs)
            app.prefs.saveMovieBrowseRows(json)
        }
    }

    private fun defaultLabel(type: RowType, filters: DiscoverFilters, genres: List<Genre>): String {
        if (type != RowType.CUSTOM) return type.displayName()
        val genrePart = genres.firstOrNull { it.id.toString() == filters.genreId }?.name ?: "All"
        val sortPart = when (filters.sortBy) {
            "popularity.desc" -> "Popular"; "vote_average.desc" -> "Top Rated"
            "release_date.desc" -> "New"; else -> "Sorted"
        }
        return "$genrePart · $sortPart"
    }

    // --- Detail / Play ---

    fun loadMovieDetail(tmdbId: Int) {
        viewModelScope.launch {
            try {
                val movie = app.tmdbApi.movieDetail(tmdbId)
                _state.update { it.copy(selectedMovie = movie) }
            } catch (_: Exception) {}
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
                val year = movie.releaseDate?.take(4)?.toIntOrNull() ?: 0
                KodiRpc(host, port, user, pass).playMovieViaFenLight(movie.id, movie.title, year)
                _state.update { it.copy(playMessage = "Playing ${movie.title} on Kodi…") }
            } catch (e: Exception) {
                _state.update { it.copy(playMessage = "Failed: ${e.message}") }
            }
        }
    }

    fun clearPlayMessage() = _state.update { it.copy(playMessage = null) }
}

fun RowType.displayName() = when (this) {
    RowType.POPULAR -> "Popular"; RowType.TRENDING -> "Trending"
    RowType.NOW_PLAYING -> "Now Playing"; RowType.UPCOMING -> "Upcoming"
    RowType.TOP_RATED -> "Top Rated"; RowType.ON_THE_AIR -> "On the Air"
    RowType.AIRING_TODAY -> "Airing Today"; RowType.CUSTOM -> "Custom"
    RowType.TMDB_LIST -> "TMDB List"; RowType.TRAKT_LIST -> "Trakt List"
}
