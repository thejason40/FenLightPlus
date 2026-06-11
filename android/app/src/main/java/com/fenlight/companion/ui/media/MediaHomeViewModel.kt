package com.fenlight.companion.ui.media

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fenlight.companion.FenLightApp
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

data class MediaHomeUiState(
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
)

abstract class MediaHomeViewModel(
    application: Application,
    makeCatalog: (FenLightApp) -> MediaCatalog,
) : AndroidViewModel(application) {

    private val app = application as FenLightApp
    private val catalog = makeCatalog(app)

    private val _state = MutableStateFlow(MediaHomeUiState())
    val state: StateFlow<MediaHomeUiState> = _state.asStateFlow()

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
                _state.update { it.copy(genres = catalog.genres()) }
            } catch (_: Exception) {}
        }
    }

    private fun loadRowConfigs() {
        viewModelScope.launch {
            val json = catalog.browseRowsJson.first()
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

    private suspend fun fetchPage1Items(config: BrowseRowConfig, region: String?, excludeAdult: Boolean): List<PaginatedItem> =
        when (config.type) {
            RowType.TMDB_LIST -> {
                val listId = config.listId ?: return emptyList()
                catalog.tmdbListPage(listId, 1).items
            }
            RowType.TRAKT_LIST -> {
                val slug = config.traktSlug ?: return emptyList()
                catalog.traktListPage(slug, config.traktUser ?: "me", 1, excludeAdult).items
            }
            RowType.TRENDING -> catalog.trendingPage(1, region, excludeAdult).items
            else -> catalog.standardPage(config.type, config.filters, 1, region, excludeAdult).items
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
                        _state.update { it.copy(watchProviders = catalog.watchProviders(region)) }
                    } catch (_: Exception) {}
                }
                async {
                    try {
                        val token = app.prefs.tmdbAccessToken.first()
                        val accountId = app.prefs.tmdbAccountId.first()
                        if (token.isNotBlank() && accountId.isNotBlank()) {
                            val lists = app.tmdbV4Api.accountLists(accountId).results
                            _state.update { it.copy(availableTmdbLists = lists) }
                        }
                    } catch (_: Exception) {}
                }
                async {
                    try {
                        if (app.prefs.traktAccessToken.first().isBlank()) return@async
                        val traktApi = app.authedTraktApi
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
            catalog.saveBrowseRowsJson(json)
        }
    }

    private fun defaultLabel(type: RowType, filters: DiscoverFilters, genres: List<Genre>): String {
        if (type != RowType.CUSTOM) return type.displayName()
        val genrePart = genres.firstOrNull { it.id.toString() == filters.genreId }?.name ?: "All"
        val sortPart = when (filters.sortBy) {
            "popularity.desc" -> "Popular"; "vote_average.desc" -> "Top Rated"
            catalog.newSortKey -> "New"; else -> "Sorted"
        }
        return "$genrePart · $sortPart"
    }
}

fun RowType.displayName() = when (this) {
    RowType.POPULAR -> "Popular"; RowType.TRENDING -> "Trending"
    RowType.NOW_PLAYING -> "Now Playing"; RowType.UPCOMING -> "Upcoming"
    RowType.TOP_RATED -> "Top Rated"; RowType.ON_THE_AIR -> "On the Air"
    RowType.AIRING_TODAY -> "Airing Today"; RowType.CUSTOM -> "Custom"
    RowType.TMDB_LIST -> "TMDB List"; RowType.TRAKT_LIST -> "Trakt List"
}
