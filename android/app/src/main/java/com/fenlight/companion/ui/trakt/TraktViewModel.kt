package com.fenlight.companion.ui.trakt

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fenlight.companion.FenLightApp
import com.fenlight.companion.data.api.KodiRpc
import com.fenlight.companion.data.model.TraktList
import com.fenlight.companion.data.model.TraktListItem
import com.fenlight.companion.data.model.TraktShowProgress
import com.fenlight.companion.data.model.TraktWatchedShow
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class TraktTab { CONTINUE_WATCHING, MY_LISTS, LIKED_LISTS, WATCHLIST }

data class TraktUiState(
    val tab: TraktTab = TraktTab.CONTINUE_WATCHING,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val watchedShows: List<TraktWatchedShow> = emptyList(),
    val showProgressMap: Map<String, TraktShowProgress> = emptyMap(),
    val myLists: List<TraktList> = emptyList(),
    val likedLists: List<TraktList> = emptyList(),
    val listItems: List<TraktListItem> = emptyList(),
    val listItemPage: Int = 0,
    val listItemHasMore: Boolean = false,
    val listItemIsLoadingMore: Boolean = false,
    val selectedListName: String = "",
    val selectedListSlug: String = "",
    val selectedListUser: String = "me",
    val watchlistMovies: List<TraktListItem> = emptyList(),
    val watchlistShows: List<TraktListItem> = emptyList(),
    val showCreateListDialog: Boolean = false,
    val listToDelete: TraktList? = null,
    val playMessage: String? = null,
)

class TraktViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as FenLightApp
    private val _state = MutableStateFlow(TraktUiState())
    val state: StateFlow<TraktUiState> = _state.asStateFlow()

    private companion object { const val CACHE_MS = 24 * 60 * 60 * 1000L }
    private val tabFetchedAt = mutableMapOf<TraktTab, Long>()

    init { loadCurrentTab() }

    fun selectTab(tab: TraktTab) {
        _state.update { it.copy(tab = tab, listItems = emptyList(), selectedListName = "", listItemPage = 0, listItemHasMore = false) }
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
        }
        if (!force && hasData && age < CACHE_MS) return
        when (tab) {
            TraktTab.CONTINUE_WATCHING -> loadContinueWatching()
            TraktTab.MY_LISTS -> loadMyLists()
            TraktTab.LIKED_LISTS -> loadLikedLists()
            TraktTab.WATCHLIST -> loadWatchlist()
        }
    }

    private fun loadContinueWatching() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val api = app.buildAuthedTraktApi(app.getValidTraktAccessToken())
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

                // Only keep shows where Trakt knows there is a next episode
                val filtered = allShows.filter { show ->
                    val slug = show.show.ids.slug ?: return@filter false
                    progressMap[slug]?.nextEpisode != null
                }

                tabFetchedAt[TraktTab.CONTINUE_WATCHING] = System.currentTimeMillis()
                _state.update { it.copy(isLoading = false, isRefreshing = false, watchedShows = filtered, showProgressMap = progressMap) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, isRefreshing = false, error = e.message) }
            }
        }
    }

    private fun loadMyLists() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val api = app.buildAuthedTraktApi(app.getValidTraktAccessToken())
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
                val api = app.buildAuthedTraktApi(app.getValidTraktAccessToken())
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
                val api = app.buildAuthedTraktApi(app.getValidTraktAccessToken())
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

    fun loadListItems(slug: String, listName: String, user: String = "me") {
        _state.update {
            it.copy(
                isLoading = true,
                error = null,
                selectedListName = listName,
                selectedListSlug = slug,
                selectedListUser = user,
                listItems = emptyList(),
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
                val api = app.buildAuthedTraktApi(app.getValidTraktAccessToken())
                val response = if (user == "me") {
                    api.myListItems(slug, page = page)
                } else {
                    api.listItems(user, slug, page = page)
                }
                val newItems = response.body() ?: emptyList()
                val totalPages = response.headers()["X-Pagination-Page-Count"]?.toIntOrNull() ?: 1
                _state.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        listItemIsLoadingMore = false,
                        listItems = if (append) it.listItems + newItems else newItems,
                        listItemPage = page,
                        listItemHasMore = page < totalPages,
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, isRefreshing = false, listItemIsLoadingMore = false, error = e.message) }
            }
        }
    }

    fun clearListItems() = _state.update {
        it.copy(listItems = emptyList(), selectedListName = "", selectedListSlug = "", listItemPage = 0, listItemHasMore = false)
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

    fun clearPlayMessage() = _state.update { it.copy(playMessage = null) }

    // List management
    fun showCreateListDialog() = _state.update { it.copy(showCreateListDialog = true) }
    fun dismissCreateListDialog() = _state.update { it.copy(showCreateListDialog = false) }
    fun confirmDeleteList(list: TraktList) = _state.update { it.copy(listToDelete = list) }
    fun cancelDeleteList() = _state.update { it.copy(listToDelete = null) }

    fun createTraktList(name: String, description: String) {
        viewModelScope.launch {
            _state.update { it.copy(showCreateListDialog = false) }
            try {
                val api = app.buildAuthedTraktApi(app.getValidTraktAccessToken())
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
                val api = app.buildAuthedTraktApi(app.getValidTraktAccessToken())
                api.deleteList(slug)
                tabFetchedAt.remove(TraktTab.MY_LISTS)
                loadMyLists()
            } catch (e: Exception) {
                _state.update { it.copy(playMessage = "Failed to delete list: ${e.message}") }
            }
        }
    }
}
