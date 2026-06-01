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
    val addRowError: String? = null,
    val genres: List<Genre> = emptyList(),
    val watchProviders: List<WatchProvider> = emptyList(),
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
                            val result = fetchPage1ForConfig(config, region, excludeAdult)
                            val items = result.results
                                .filter { !excludeAdult || !it.adult }
                                .map { m ->
                                    PaginatedItem(
                                        id = m.id, title = m.title,
                                        posterUrl = FenLightApp.posterUrl(m.posterPath),
                                        rating = m.voteAverage.takeIf { it > 0 },
                                        backdropUrl = FenLightApp.backdropUrl(m.backdropPath),
                                    )
                                }
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

    private suspend fun fetchPage1ForConfig(config: BrowseRowConfig, region: String?, excludeAdult: Boolean) =
        when (config.type) {
            RowType.POPULAR -> app.tmdbApi.popularMovies(1, region)
            RowType.TRENDING -> app.tmdbApi.trendingMovies(1, region)
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
            // TV-only types — shouldn't appear in movie rows, fall back to popular
            RowType.ON_THE_AIR, RowType.AIRING_TODAY -> app.tmdbApi.popularMovies(1, region)
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
        _state.update { it.copy(showAddRowSheet = true, addRowError = null, pendingRowType = RowType.CUSTOM, pendingRowLabel = "", pendingRowFilters = DiscoverFilters()) }
        viewModelScope.launch {
            try {
                val region = app.prefs.region.first().takeIf { it.isNotBlank() }
                val providers = app.tmdbApi.movieWatchProviders(region).results.sortedBy { it.displayPriority }
                _state.update { it.copy(watchProviders = providers) }
            } catch (_: Exception) {}
        }
    }

    fun dismissAddRowSheet() = _state.update { it.copy(showAddRowSheet = false) }
    fun onPendingRowTypeChange(type: RowType) = _state.update { it.copy(pendingRowType = type) }
    fun onPendingRowLabelChange(label: String) = _state.update { it.copy(pendingRowLabel = label) }
    fun onPendingRowFiltersChange(f: DiscoverFilters) = _state.update { it.copy(pendingRowFilters = f) }

    fun addCustomRow() {
        val customRows = _state.value.rows.filter { it.config.id !in listOf(FIXED_POPULAR, FIXED_TRENDING) }
        if (customRows.size >= MAX_CUSTOM_ROWS) {
            _state.update { it.copy(addRowError = "Maximum $MAX_CUSTOM_ROWS custom rows reached") }
            return
        }
        val s = _state.value
        val label = s.pendingRowLabel.ifBlank { defaultLabel(s.pendingRowType, s.pendingRowFilters, s.genres) }
        val newConfig = BrowseRowConfig(
            id = UUID.randomUUID().toString(),
            label = label,
            type = s.pendingRowType,
            filters = if (s.pendingRowType == RowType.CUSTOM) s.pendingRowFilters else null,
        )
        val newRowState = BrowseRowState(config = newConfig, isLoading = true)
        val updatedRows = _state.value.rows + newRowState
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
}
