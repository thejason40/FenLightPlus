package com.fenlight.companion.ui.trakt

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fenlight.companion.FenLightApp
import com.fenlight.companion.data.api.KodiRpc
import com.fenlight.companion.data.model.TraktHistoryEntry
import com.fenlight.companion.data.model.TraktList
import com.fenlight.companion.data.model.TraktListItem
import com.fenlight.companion.data.model.TraktShowProgress
import com.fenlight.companion.data.model.TraktWatchedShow
import com.fenlight.companion.ui.components.PaginatedItem
import com.fenlight.companion.util.Pagination
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class TraktTab { CONTINUE_WATCHING, MY_LISTS, LIKED_LISTS, WATCHLIST, RECENT }

data class TraktUiState(
    val tab: TraktTab = TraktTab.CONTINUE_WATCHING,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val watchedShows: List<TraktWatchedShow> = emptyList(),
    val showProgressMap: Map<String, TraktShowProgress> = emptyMap(),
    val continueWatchingPosters: Map<Int, String?> = emptyMap(), // tmdbId -> poster path
    val myLists: List<TraktList> = emptyList(),
    val likedLists: List<TraktList> = emptyList(),
    val listItems: List<TraktListItem> = emptyList(),
    val listItemsEnriched: List<PaginatedItem> = emptyList(),
    val listItemTypeMap: Map<Int, String> = emptyMap(),
    val listItemPage: Int = 0,
    val listItemHasMore: Boolean = false,
    val listItemIsLoadingMore: Boolean = false,
    val selectedListName: String = "",
    val selectedListSlug: String = "",
    val selectedListUser: String = "me",
    val watchlistMovies: List<TraktListItem> = emptyList(),
    val watchlistShows: List<TraktListItem> = emptyList(),
    val recentHistory: List<TraktHistoryEntry> = emptyList(),
    val recentHistoryPage: Int = 0,
    val recentHistoryHasMore: Boolean = false,
    val recentHistoryIsLoadingMore: Boolean = false,
    val showCreateListDialog: Boolean = false,
    val listToDelete: TraktList? = null,
    val playMessage: String? = null,
    // Public list search
    val listSearchActive: Boolean = false,
    val listSearchQuery: String = "",
    val listSearchResults: List<TraktList> = emptyList(),
    val listSearchPage: Int = 0,
    val listSearchHasMore: Boolean = false,
    val listSearchIsLoadingMore: Boolean = false,
    val isSearchingLists: Boolean = false,
    val listSearchPerformed: Boolean = false,
    // Like / unlike confirmations
    val listToLike: TraktList? = null,
    val listToUnlike: TraktList? = null,
)

class TraktViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as FenLightApp
    private val _state = MutableStateFlow(TraktUiState())
    val state: StateFlow<TraktUiState> = _state.asStateFlow()

    private companion object { const val CACHE_MS = 24 * 60 * 60 * 1000L }
    private val tabFetchedAt = mutableMapOf<TraktTab, Long>()

    init { loadCurrentTab() }

    fun selectTab(tab: TraktTab) {
        _state.update {
            it.copy(
                tab = tab,
                listItems = emptyList(),
                selectedListName = "",
                listItemPage = 0,
                listItemHasMore = false,
                recentHistory = if (tab != TraktTab.RECENT) emptyList() else it.recentHistory,
                recentHistoryPage = if (tab != TraktTab.RECENT) 0 else it.recentHistoryPage,
                recentHistoryHasMore = if (tab != TraktTab.RECENT) false else it.recentHistoryHasMore,
            )
        }
        loadCurrentTab()
    }

    fun refresh() {
        tabFetchedAt.remove(_state.value.tab)
        _state.update { it.copy(isRefreshing = true) }
        loadCurrentTab(force = true)
    }

    private fun loadCurrentTab(force: Boolean = false) {
        val tab = _state.value.tab
        val age = System.currentTimeMillis() - (tabFetchedAt[tab] ?: 0L)
        val hasData = when (tab) {
            TraktTab.CONTINUE_WATCHING -> _state.value.watchedShows.isNotEmpty()
            TraktTab.MY_LISTS -> _state.value.myLists.isNotEmpty()
            TraktTab.LIKED_LISTS -> _state.value.likedLists.isNotEmpty()
            TraktTab.WATCHLIST -> _state.value.watchlistMovies.isNotEmpty() || _state.value.watchlistShows.isNotEmpty()
            TraktTab.RECENT -> _state.value.recentHistory.isNotEmpty()
        }
        if (!force && hasData && age < CACHE_MS) return
        when (tab) {
            TraktTab.CONTINUE_WATCHING -> loadContinueWatching()
            TraktTab.MY_LISTS -> loadMyLists()
            TraktTab.LIKED_LISTS -> loadLikedLists()
            TraktTab.WATCHLIST -> loadWatchlist()
            TraktTab.RECENT -> loadRecent()
        }
    }

    private fun loadContinueWatching() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val api = app.authedTraktApi
                val allShows = api.watchedShows()
                    .sortedByDescending { it.lastWatchedAt }
                    .take(30)

                // Fetch per-show progress in parallel; ignore individual failures
                val progressMap = coroutineScope {
                    allShows.mapNotNull { show ->
                        val slug = show.show.ids.slug ?: return@mapNotNull null
                        async { runCatching { slug to api.showProgress(slug) }.getOrNull() }
                    }.awaitAll().filterNotNull().toMap()
                }

                // Only keep shows where Trakt knows there is a next episode and it has already aired
                val today = java.time.LocalDate.now().toString() // "YYYY-MM-DD"
                val filtered = allShows.filter { show ->
                    val slug = show.show.ids.slug ?: return@filter false
                    val nextEp = progressMap[slug]?.nextEpisode ?: return@filter false
                    val airDate = nextEp.firstAired?.take(10) // "YYYY-MM-DD"
                    airDate == null || airDate <= today
                }

                // Fetch TMDB poster art for each show in parallel (Trakt provides no artwork).
                val posters = coroutineScope {
                    filtered.mapNotNull { w ->
                        val tmdb = w.show.ids.tmdb ?: return@mapNotNull null
                        async { runCatching { tmdb to app.tmdbApi.tvDetail(tmdb, "").posterPath }.getOrNull() }
                    }.awaitAll().filterNotNull().toMap()
                }

                tabFetchedAt[TraktTab.CONTINUE_WATCHING] = System.currentTimeMillis()
                _state.update { it.copy(isLoading = false, isRefreshing = false, watchedShows = filtered, showProgressMap = progressMap, continueWatchingPosters = posters) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, isRefreshing = false, error = e.message) }
            }
        }
    }

    private fun loadMyLists() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val api = app.authedTraktApi
                val lists = api.myLists()
                tabFetchedAt[TraktTab.MY_LISTS] = System.currentTimeMillis()
                _state.update { it.copy(isLoading = false, isRefreshing = false, myLists = lists) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, isRefreshing = false, error = e.message) }
            }
        }
    }

    private fun loadLikedLists() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val api = app.authedTraktApi
                val response = api.likedLists(page = 1, limit = 50)
                val liked = response.body() ?: emptyList()
                tabFetchedAt[TraktTab.LIKED_LISTS] = System.currentTimeMillis()
                _state.update { it.copy(isLoading = false, isRefreshing = false, likedLists = liked.map { it.list }) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, isRefreshing = false, error = e.message) }
            }
        }
    }

    private fun loadWatchlist() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val api = app.authedTraktApi
                coroutineScope {
                    val movies = async { api.getWatchlist("movies") }
                    val shows = async { api.getWatchlist("shows") }
                    tabFetchedAt[TraktTab.WATCHLIST] = System.currentTimeMillis()
                    _state.update { it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        watchlistMovies = movies.await(),
                        watchlistShows = shows.await(),
                    )}
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, isRefreshing = false, error = e.message) }
            }
        }
    }

    private fun loadRecent() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val api = app.authedTraktApi
                val response = api.history(page = 1, limit = 50)
                val entries = response.body() ?: emptyList()
                val pagesHeader = response.headers()[Pagination.TRAKT_PAGE_COUNT_HEADER]
                tabFetchedAt[TraktTab.RECENT] = System.currentTimeMillis()
                _state.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        recentHistory = entries,
                        recentHistoryPage = 1,
                        recentHistoryHasMore = Pagination.hasMoreByTraktHeader(pagesHeader, 1),
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, isRefreshing = false, error = e.message) }
            }
        }
    }

    fun loadMoreRecent() {
        val s = _state.value
        if (s.recentHistoryIsLoadingMore || !s.recentHistoryHasMore) return
        viewModelScope.launch {
            _state.update { it.copy(recentHistoryIsLoadingMore = true) }
            try {
                val api = app.authedTraktApi
                val nextPage = s.recentHistoryPage + 1
                val response = api.history(page = nextPage, limit = 50)
                val entries = response.body() ?: emptyList()
                val pagesHeader = response.headers()[Pagination.TRAKT_PAGE_COUNT_HEADER]
                _state.update {
                    it.copy(
                        recentHistoryIsLoadingMore = false,
                        recentHistory = it.recentHistory + entries,
                        recentHistoryPage = nextPage,
                        recentHistoryHasMore = Pagination.hasMoreByTraktHeader(pagesHeader, nextPage),
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(recentHistoryIsLoadingMore = false, error = e.message) }
            }
        }
    }

    fun loadListItems(slug: String, listName: String, user: String = "me") {
        _state.update {
            it.copy(
                isLoading = true,
                error = null,
                selectedListName = listName,
                selectedListSlug = slug,
                selectedListUser = user,
                listItems = emptyList(),
                listItemsEnriched = emptyList(),
                listItemTypeMap = emptyMap(),
                listItemPage = 0,
                listItemHasMore = false,
            )
        }
        fetchListItemsPage(slug, user, page = 1, append = false)
    }

    fun loadMoreListItems() {
        val s = _state.value
        if (s.listItemIsLoadingMore || !s.listItemHasMore) return
        fetchListItemsPage(s.selectedListSlug, s.selectedListUser, page = s.listItemPage + 1, append = true)
    }

    private fun fetchListItemsPage(slug: String, user: String, page: Int, append: Boolean) {
        viewModelScope.launch {
            _state.update { it.copy(listItemIsLoadingMore = append, isLoading = !append) }
            try {
                val api = app.authedTraktApi
                val response = if (user == "me") {
                    api.myListItems(slug, page = page)
                } else {
                    api.listItems(user, slug, page = page)
                }
                val newItems = response.body() ?: emptyList()
                val pagesHeader = response.headers()[Pagination.TRAKT_PAGE_COUNT_HEADER]

                val enrichedPairs = coroutineScope {
                    newItems.map { item ->
                        async {
                            val tmdbId = item.movie?.ids?.tmdb ?: item.show?.ids?.tmdb
                                ?: return@async null
                            runCatching {
                                if (item.type == "movie") {
                                    val d = app.tmdbApi.movieDetail(tmdbId, "")
                                    PaginatedItem(
                                        id = d.id,
                                        title = d.title,
                                        posterUrl = FenLightApp.posterUrl(d.posterPath),
                                        rating = d.voteAverage.takeIf { it > 0 },
                                        backdropUrl = FenLightApp.backdropUrl(d.backdropPath),
                                    )
                                } else {
                                    val d = app.tmdbApi.tvDetail(tmdbId, "")
                                    PaginatedItem(
                                        id = d.id,
                                        title = d.name,
                                        posterUrl = FenLightApp.posterUrl(d.posterPath),
                                        rating = d.voteAverage.takeIf { it > 0 },
                                        backdropUrl = FenLightApp.backdropUrl(d.backdropPath),
                                    )
                                } to item.type
                            }.getOrNull()
                        }
                    }.awaitAll().filterNotNull()
                }
                val enrichedItems = enrichedPairs.map { it.first }
                val typeMap = enrichedPairs.associate { (item, type) -> item.id to type }

                _state.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        listItemIsLoadingMore = false,
                        listItems = if (append) it.listItems + newItems else newItems,
                        listItemsEnriched = if (append) it.listItemsEnriched + enrichedItems else enrichedItems,
                        listItemTypeMap = if (append) it.listItemTypeMap + typeMap else typeMap,
                        listItemPage = page,
                        listItemHasMore = Pagination.hasMoreByTraktHeader(pagesHeader, page),
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, isRefreshing = false, listItemIsLoadingMore = false, error = e.message) }
            }
        }
    }

    fun clearListItems() = _state.update {
        it.copy(
            listItems = emptyList(),
            listItemsEnriched = emptyList(),
            listItemTypeMap = emptyMap(),
            selectedListName = "",
            selectedListSlug = "",
            listItemPage = 0,
            listItemHasMore = false,
        )
    }

    fun playNextEpisode(watched: TraktWatchedShow) {
        viewModelScope.launch {
            val tmdbId = watched.show.ids.tmdb ?: return@launch
            val slug = watched.show.ids.slug ?: return@launch
            val title = watched.show.title
            val year = watched.show.year ?: 0
            val nextEp = _state.value.showProgressMap[slug]?.nextEpisode ?: return@launch
            try {
                val host = app.prefs.kodiHost.first()
                val port = app.prefs.kodiPort.first()
                val user = app.prefs.kodiUser.first()
                val pass = app.prefs.kodiPass.first()
                KodiRpc(host, port, user, pass).playEpisodeViaFenLight(tmdbId, title, year, nextEp.season, nextEp.number)
                _state.update { it.copy(playMessage = "Playing S${nextEp.season}E${nextEp.number} of $title on Kodi…") }
            } catch (e: Exception) {
                _state.update { it.copy(playMessage = "Failed: ${e.message}") }
            }
        }
    }

    fun playListMovie(item: TraktListItem) {
        viewModelScope.launch {
            val movie = item.movie ?: return@launch
            val tmdbId = movie.ids.tmdb ?: return@launch
            try {
                val host = app.prefs.kodiHost.first()
                val port = app.prefs.kodiPort.first()
                val user = app.prefs.kodiUser.first()
                val pass = app.prefs.kodiPass.first()
                KodiRpc(host, port, user, pass).playMovieViaFenLight(tmdbId, movie.title, movie.year ?: 0)
                _state.update { it.copy(playMessage = "Playing ${movie.title} on Kodi…") }
            } catch (e: Exception) {
                _state.update { it.copy(playMessage = "Failed: ${e.message}") }
            }
        }
    }

    fun playRecentMovie(entry: TraktHistoryEntry) {
        viewModelScope.launch {
            val movie = entry.movie ?: return@launch
            val tmdbId = movie.ids.tmdb ?: return@launch
            try {
                val host = app.prefs.kodiHost.first()
                val port = app.prefs.kodiPort.first()
                val user = app.prefs.kodiUser.first()
                val pass = app.prefs.kodiPass.first()
                KodiRpc(host, port, user, pass).playMovieViaFenLight(tmdbId, movie.title, movie.year ?: 0)
                _state.update { it.copy(playMessage = "Playing ${movie.title} on Kodi…") }
            } catch (e: Exception) {
                _state.update { it.copy(playMessage = "Failed: ${e.message}") }
            }
        }
    }

    fun playRecentEpisode(entry: TraktHistoryEntry) {
        viewModelScope.launch {
            val show = entry.show ?: return@launch
            val ep = entry.episode ?: return@launch
            val tmdbId = show.ids.tmdb ?: return@launch
            try {
                val host = app.prefs.kodiHost.first()
                val port = app.prefs.kodiPort.first()
                val user = app.prefs.kodiUser.first()
                val pass = app.prefs.kodiPass.first()
                KodiRpc(host, port, user, pass).playEpisodeViaFenLight(tmdbId, show.title, show.year ?: 0, ep.season, ep.number)
                _state.update { it.copy(playMessage = "Playing S${ep.season}E${ep.number} of ${show.title} on Kodi…") }
            } catch (e: Exception) {
                _state.update { it.copy(playMessage = "Failed: ${e.message}") }
            }
        }
    }

    fun clearPlayMessage() = _state.update { it.copy(playMessage = null) }

    // ── Public list search ───────────────────────────────────────────────

    fun openListSearch() = _state.update { it.copy(listSearchActive = true) }

    fun closeListSearch() = _state.update {
        it.copy(
            listSearchActive = false,
            listSearchQuery = "",
            listSearchResults = emptyList(),
            listSearchPage = 0,
            listSearchHasMore = false,
            listSearchPerformed = false,
        )
    }

    fun setListSearchQuery(query: String) = _state.update { it.copy(listSearchQuery = query) }

    fun searchLists() {
        val query = _state.value.listSearchQuery.trim()
        if (query.isEmpty()) return
        _state.update {
            it.copy(listSearchResults = emptyList(), listSearchPage = 0, listSearchHasMore = false)
        }
        fetchListSearchPage(query, page = 1, append = false)
    }

    fun loadMoreListSearch() {
        val s = _state.value
        if (s.listSearchIsLoadingMore || !s.listSearchHasMore) return
        fetchListSearchPage(s.listSearchQuery.trim(), page = s.listSearchPage + 1, append = true)
    }

    private fun fetchListSearchPage(query: String, page: Int, append: Boolean) {
        viewModelScope.launch {
            _state.update { it.copy(isSearchingLists = !append, listSearchIsLoadingMore = append) }
            try {
                val response = app.authedTraktApi.searchLists(query, page = page)
                val results = response.body().orEmpty().map { it.list }
                val pagesHeader = response.headers()[Pagination.TRAKT_PAGE_COUNT_HEADER]
                _state.update {
                    it.copy(
                        isSearchingLists = false,
                        listSearchIsLoadingMore = false,
                        listSearchPerformed = true,
                        listSearchResults = if (append) it.listSearchResults + results else results,
                        listSearchPage = page,
                        listSearchHasMore = Pagination.hasMoreByTraktHeader(pagesHeader, page),
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(isSearchingLists = false, listSearchIsLoadingMore = false, playMessage = "Search failed: ${e.message}")
                }
            }
        }
    }

    // ── Like / unlike lists ──────────────────────────────────────────────

    fun confirmLikeList(list: TraktList) = _state.update { it.copy(listToLike = list) }
    fun cancelLikeList() = _state.update { it.copy(listToLike = null) }

    fun likeList(list: TraktList) {
        viewModelScope.launch {
            _state.update { it.copy(listToLike = null) }
            val user = list.user?.pathId
            if (user == null) {
                _state.update { it.copy(playMessage = "Can't like \"${list.name}\" — unknown owner") }
                return@launch
            }
            try {
                app.authedTraktApi.likeList(user, list.ids.trakt?.toString() ?: list.slug)
                tabFetchedAt.remove(TraktTab.LIKED_LISTS) // show it on next visit
                _state.update { it.copy(playMessage = "Liked \"${list.name}\"") }
            } catch (e: Exception) {
                _state.update { it.copy(playMessage = "Failed to like list: ${e.message}") }
            }
        }
    }

    fun confirmUnlikeList(list: TraktList) = _state.update { it.copy(listToUnlike = list) }
    fun cancelUnlikeList() = _state.update { it.copy(listToUnlike = null) }

    fun unlikeList(list: TraktList) {
        viewModelScope.launch {
            _state.update { it.copy(listToUnlike = null) }
            val user = list.user?.pathId
            if (user == null) {
                _state.update { it.copy(playMessage = "Can't unlike \"${list.name}\" — unknown owner") }
                return@launch
            }
            try {
                app.authedTraktApi.unlikeList(user, list.ids.trakt?.toString() ?: list.slug)
                _state.update {
                    it.copy(
                        // Slugs are unique per owner; trakt ids may be absent
                        likedLists = it.likedLists.filterNot { l ->
                            l.slug == list.slug && l.user?.username == list.user?.username
                        },
                        playMessage = "Unliked \"${list.name}\"",
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(playMessage = "Failed to unlike list: ${e.message}") }
            }
        }
    }

    // List management
    fun showCreateListDialog() = _state.update { it.copy(showCreateListDialog = true) }
    fun dismissCreateListDialog() = _state.update { it.copy(showCreateListDialog = false) }
    fun confirmDeleteList(list: TraktList) = _state.update { it.copy(listToDelete = list) }
    fun cancelDeleteList() = _state.update { it.copy(listToDelete = null) }

    fun createTraktList(name: String, description: String) {
        viewModelScope.launch {
            _state.update { it.copy(showCreateListDialog = false) }
            try {
                val api = app.authedTraktApi
                api.createList(mapOf(
                    "name" to name,
                    "description" to description,
                    "privacy" to "private",
                    "display_numbers" to false,
                    "allow_comments" to true,
                ))
                tabFetchedAt.remove(TraktTab.MY_LISTS)
                loadMyLists()
            } catch (e: Exception) {
                _state.update { it.copy(playMessage = "Failed to create list: ${e.message}") }
            }
        }
    }

    fun deleteTraktList(slug: String) {
        viewModelScope.launch {
            _state.update { it.copy(listToDelete = null) }
            try {
                val api = app.authedTraktApi
                api.deleteList(slug)
                tabFetchedAt.remove(TraktTab.MY_LISTS)
                loadMyLists()
            } catch (e: Exception) {
                _state.update { it.copy(playMessage = "Failed to delete list: ${e.message}") }
            }
        }
    }
}
