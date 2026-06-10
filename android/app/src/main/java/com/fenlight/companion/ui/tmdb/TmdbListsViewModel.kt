package com.fenlight.companion.ui.tmdb

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fenlight.companion.FenLightApp
import com.fenlight.companion.data.api.KodiRpc
import com.fenlight.companion.data.model.TmdbList
import com.fenlight.companion.data.model.TmdbListItem
import com.fenlight.companion.ui.components.PaginatedItem
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class TmdbListsUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val isAuthenticated: Boolean = false,
    val lists: List<TmdbList> = emptyList(),
    val listItems: List<TmdbListItem> = emptyList(),
    val selectedListId: Int = 0,
    val selectedListName: String = "",
    val listItemPage: Int = 0,
    val listItemHasMore: Boolean = false,
    val listItemIsLoadingMore: Boolean = false,
    val showCreateListDialog: Boolean = false,
    val listToDelete: TmdbList? = null,
    val playMessage: String? = null,
)

class TmdbListsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as FenLightApp
    private val _state = MutableStateFlow(TmdbListsUiState())
    val state: StateFlow<TmdbListsUiState> = _state.asStateFlow()

    private companion object { const val CACHE_MS = 24 * 60 * 60 * 1000L }
    private var listsFetchedAt = 0L

    init { loadLists() }

    fun loadLists(force: Boolean = false) {
        viewModelScope.launch {
            val accessToken = app.prefs.tmdbAccessToken.first()
            val accountId = app.prefs.tmdbAccountId.first()
            if (accessToken.isBlank()) {
                _state.update { it.copy(isAuthenticated = false, isLoading = false, isRefreshing = false) }
                return@launch
            }
            val age = System.currentTimeMillis() - listsFetchedAt
            if (!force && _state.value.lists.isNotEmpty() && age < CACHE_MS) {
                _state.update { it.copy(isRefreshing = false) }
                return@launch
            }
            _state.update { it.copy(isLoading = true, error = null, isAuthenticated = true) }
            try {
                val result = app.tmdbV4Api.accountLists(accountId)
                listsFetchedAt = System.currentTimeMillis()
                _state.update { it.copy(isLoading = false, isRefreshing = false, lists = result.results, listItems = emptyList(), selectedListName = "") }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, isRefreshing = false, error = e.message) }
            }
        }
    }

    fun refresh() {
        listsFetchedAt = 0L
        if (_state.value.listItems.isNotEmpty()) {
            val id = _state.value.selectedListId
            _state.update { it.copy(isRefreshing = true) }
            fetchListItemsPage(id, page = 1, append = false)
        } else {
            _state.update { it.copy(isRefreshing = true) }
            loadLists(force = true)
        }
    }

    fun loadListItems(listId: Int, listName: String) {
        _state.update {
            it.copy(
                isLoading = true, error = null,
                selectedListId = listId, selectedListName = listName,
                listItems = emptyList(), listItemPage = 0, listItemHasMore = false,
            )
        }
        fetchListItemsPage(listId, page = 1, append = false)
    }

    fun loadMoreListItems() {
        val s = _state.value
        if (s.listItemIsLoadingMore || !s.listItemHasMore) return
        fetchListItemsPage(s.selectedListId, page = s.listItemPage + 1, append = true)
    }

    private fun fetchListItemsPage(listId: Int, page: Int, append: Boolean) {
        viewModelScope.launch {
            _state.update { it.copy(listItemIsLoadingMore = append, isLoading = !append) }
            try {
                val detail = app.tmdbV4Api.listDetail(listId, page)
                _state.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        listItemIsLoadingMore = false,
                        listItems = if (append) it.listItems + detail.results else detail.results,
                        listItemPage = page,
                        listItemHasMore = page < detail.totalPages,
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, isRefreshing = false, listItemIsLoadingMore = false, error = e.message) }
            }
        }
    }

    fun clearListItems() = _state.update {
        it.copy(listItems = emptyList(), selectedListId = 0, selectedListName = "", listItemPage = 0, listItemHasMore = false)
    }

    fun playItem(item: TmdbListItem) {
        viewModelScope.launch {
            try {
                val host = app.prefs.kodiHost.first()
                val port = app.prefs.kodiPort.first()
                val user = app.prefs.kodiUser.first()
                val pass = app.prefs.kodiPass.first()
                val kodi = KodiRpc(host, port, user, pass)
                val title = item.title ?: item.name ?: ""
                val year = (item.releaseDate ?: item.firstAirDate)?.take(4)?.toIntOrNull() ?: 0
                if (item.mediaType == "movie") {
                    kodi.playMovieViaFenLight(item.id, title, year)
                    _state.update { it.copy(playMessage = "Playing $title on Kodi…") }
                }
            } catch (e: Exception) {
                _state.update { it.copy(playMessage = "Failed: ${e.message}") }
            }
        }
    }

    fun clearPlayMessage() = _state.update { it.copy(playMessage = null) }

    // List management
    fun showCreateListDialog() = _state.update { it.copy(showCreateListDialog = true) }
    fun dismissCreateListDialog() = _state.update { it.copy(showCreateListDialog = false) }
    fun confirmDeleteList(list: TmdbList) = _state.update { it.copy(listToDelete = list) }
    fun cancelDeleteList() = _state.update { it.copy(listToDelete = null) }

    fun createTmdbList(name: String, description: String) {
        viewModelScope.launch {
            _state.update { it.copy(showCreateListDialog = false) }
            try {
                app.tmdbV4Api.createList(mapOf(
                    "name" to name,
                    "description" to description,
                    "iso_3166_1" to "US",
                    "iso_639_1" to "en",
                    "public" to false,
                ))
                listsFetchedAt = 0L
                loadLists(force = true)
            } catch (e: Exception) {
                _state.update { it.copy(playMessage = "Failed to create list: ${e.message}") }
            }
        }
    }

    fun deleteTmdbList(listId: Int) {
        viewModelScope.launch {
            _state.update { it.copy(listToDelete = null) }
            try {
                app.tmdbV4Api.deleteList(listId)
                listsFetchedAt = 0L
                loadLists(force = true)
            } catch (e: Exception) {
                _state.update { it.copy(playMessage = "Failed to delete list: ${e.message}") }
            }
        }
    }
}
