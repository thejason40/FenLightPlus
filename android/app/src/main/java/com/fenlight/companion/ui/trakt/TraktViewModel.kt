package com.fenlight.companion.ui.trakt

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fenlight.companion.FenLightApp
import com.fenlight.companion.data.api.KodiRpc
import com.fenlight.companion.data.model.TraktList
import com.fenlight.companion.data.model.TraktListItem
import com.fenlight.companion.data.model.TraktWatchedShow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class TraktTab { CONTINUE_WATCHING, MY_LISTS, LIKED_LISTS }

data class TraktUiState(
    val tab: TraktTab = TraktTab.CONTINUE_WATCHING,
    val isLoading: Boolean = false,
    val error: String? = null,
    val watchedShows: List<TraktWatchedShow> = emptyList(),
    val myLists: List<TraktList> = emptyList(),
    val likedLists: List<TraktList> = emptyList(),
    val listItems: List<TraktListItem> = emptyList(),
    val listItemPage: Int = 0,
    val listItemHasMore: Boolean = false,
    val listItemIsLoadingMore: Boolean = false,
    val selectedListName: String = "",
    val selectedListSlug: String = "",
    val selectedListUser: String = "me",
    val playMessage: String? = null,
)

class TraktViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as FenLightApp
    private val _state = MutableStateFlow(TraktUiState())
    val state: StateFlow<TraktUiState> = _state.asStateFlow()

    init { loadCurrentTab() }

    fun selectTab(tab: TraktTab) {
        _state.update { it.copy(tab = tab, listItems = emptyList(), selectedListName = "", listItemPage = 0, listItemHasMore = false) }
        loadCurrentTab()
    }

    private fun loadCurrentTab() {
        when (_state.value.tab) {
            TraktTab.CONTINUE_WATCHING -> loadContinueWatching()
            TraktTab.MY_LISTS -> loadMyLists()
            TraktTab.LIKED_LISTS -> loadLikedLists()
        }
    }

    private fun loadContinueWatching() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val api = app.buildAuthedTraktApi(app.getValidTraktAccessToken())
                val shows = api.watchedShows()
                val sorted = shows.sortedByDescending { it.lastWatchedAt }
                _state.update { it.copy(isLoading = false, watchedShows = sorted) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private fun loadMyLists() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val api = app.buildAuthedTraktApi(app.getValidTraktAccessToken())
                val lists = api.myLists()
                _state.update { it.copy(isLoading = false, myLists = lists) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
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
                _state.update { it.copy(isLoading = false, likedLists = liked.map { it.list }) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
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
                        listItemIsLoadingMore = false,
                        listItems = if (append) it.listItems + newItems else newItems,
                        listItemPage = page,
                        listItemHasMore = page < totalPages,
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, listItemIsLoadingMore = false, error = e.message) }
            }
        }
    }

    fun clearListItems() = _state.update {
        it.copy(listItems = emptyList(), selectedListName = "", selectedListSlug = "", listItemPage = 0, listItemHasMore = false)
    }

    fun playNextEpisode(watched: TraktWatchedShow) {
        viewModelScope.launch {
            val tmdbId = watched.show.ids.tmdb ?: return@launch
            val title = watched.show.title
            val year = watched.show.year ?: 0
            val (season, ep) = watched.nextEpisode() ?: return@launch
            try {
                val host = app.prefs.kodiHost.first()
                val port = app.prefs.kodiPort.first()
                val user = app.prefs.kodiUser.first()
                val pass = app.prefs.kodiPass.first()
                KodiRpc(host, port, user, pass).playEpisodeViaFenLight(tmdbId, title, year, season, ep)
                _state.update { it.copy(playMessage = "Playing S${season}E${ep} of $title on Kodi…") }
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
}
