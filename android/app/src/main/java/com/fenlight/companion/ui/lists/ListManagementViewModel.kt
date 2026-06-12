package com.fenlight.companion.ui.lists

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fenlight.companion.FenLightApp
import com.fenlight.companion.data.model.MediaType
import com.fenlight.companion.data.model.TmdbList
import com.fenlight.companion.data.model.TraktList
import com.fenlight.companion.data.model.TraktListItem
import com.fenlight.companion.util.MediaTypeMapper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ListManagementState(
    val hasTraktAuth: Boolean = false,
    val hasTmdbAuth: Boolean = false,
    val watchlistedIds: Set<Int> = emptySet(),
    val traktLists: List<TraktList> = emptyList(),
    val tmdbLists: List<TmdbList> = emptyList(),
    // "Find lists containing" — null until loaded
    val listsContainingForTmdbId: Int? = null,
    val listsContaining: List<TraktList>? = null,
    val listsContainingLoading: Boolean = false,
    val listsContainingPage: Int = 0,
    val listsContainingHasMore: Boolean = false,
    val listsContainingLoadingMore: Boolean = false,
    val listsContainingTraktId: String? = null,
    val listsContainingTraktType: String? = null,
    val likedListIds: Set<Int> = emptySet(), // Trakt list ids the user has liked
    // Opened list items (nested sheet)
    val openedList: TraktList? = null,
    val openedListItems: List<TraktListItem>? = null,
    val openedListLoading: Boolean = false,
)

class ListManagementViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as FenLightApp
    private val _state = MutableStateFlow(ListManagementState())
    val state: StateFlow<ListManagementState> = _state.asStateFlow()

    // Confirm list actions to the user. Runs on the main dispatcher (viewModelScope default).
    private fun toast(message: String) {
        Toast.makeText(app, message, Toast.LENGTH_SHORT).show()
    }

    // Maps a caller-supplied mediaType to the strings Trakt / TMDB expect (see MediaTypeMapper)
    private fun traktKey(mediaType: String) = MediaTypeMapper.traktKey(mediaType)
    private fun tmdbType(mediaType: String) = MediaTypeMapper.tmdbType(mediaType)

    init {
        viewModelScope.launch {
            val traktToken = app.prefs.traktAccessToken.first()
            val tmdbToken = app.prefs.tmdbAccessToken.first()
            _state.update { it.copy(hasTraktAuth = traktToken.isNotBlank(), hasTmdbAuth = tmdbToken.isNotBlank()) }
        }
        loadWatchlist()
    }

    private fun loadWatchlist() {
        viewModelScope.launch {
            try {
                val token = app.prefs.traktAccessToken.first()
                if (token.isBlank()) return@launch
                val api = app.authedTraktApi
                val movieItems = api.getWatchlist("movies")
                val showItems = api.getWatchlist("shows")
                val ids = buildSet<Int> {
                    movieItems.mapNotNull { it.movie?.ids?.tmdb }.forEach { add(it) }
                    showItems.mapNotNull { it.show?.ids?.tmdb }.forEach { add(it) }
                }
                _state.update { it.copy(watchlistedIds = ids) }
            } catch (_: Exception) {}
        }
    }

    fun loadTraktLists() {
        if (_state.value.traktLists.isNotEmpty()) return
        viewModelScope.launch {
            try {
                val token = app.prefs.traktAccessToken.first()
                if (token.isBlank()) return@launch
                val lists = app.authedTraktApi.myLists()
                _state.update { it.copy(traktLists = lists) }
            } catch (_: Exception) {}
        }
    }

    fun loadTmdbLists() {
        if (_state.value.tmdbLists.isNotEmpty()) return
        viewModelScope.launch {
            try {
                val token = app.prefs.tmdbAccessToken.first()
                val accountId = app.prefs.tmdbAccountId.first()
                if (token.isBlank() || accountId.isBlank()) return@launch
                val result = app.tmdbV4Api.accountLists(accountId)
                _state.update { it.copy(tmdbLists = result.results) }
            } catch (_: Exception) {}
        }
    }

    fun addToTraktWatchlist(tmdbId: Int, mediaType: String) {
        viewModelScope.launch {
            try {
                val api = app.authedTraktApi
                api.addToWatchlist(mapOf(traktKey(mediaType) to listOf(mapOf("ids" to mapOf("tmdb" to tmdbId)))))
                _state.update { it.copy(watchlistedIds = it.watchlistedIds + tmdbId) }
                toast("Added to Watchlist")
            } catch (e: Exception) {
                toast("Failed: ${e.message}")
            }
        }
    }

    fun removeFromTraktWatchlist(tmdbId: Int, mediaType: String) {
        viewModelScope.launch {
            try {
                val api = app.authedTraktApi
                api.removeFromWatchlist(mapOf(traktKey(mediaType) to listOf(mapOf("ids" to mapOf("tmdb" to tmdbId)))))
                _state.update { it.copy(watchlistedIds = it.watchlistedIds - tmdbId) }
                toast("Removed from Watchlist")
            } catch (e: Exception) {
                toast("Failed: ${e.message}")
            }
        }
    }

    fun addToTraktList(tmdbId: Int, mediaType: String, slug: String) {
        viewModelScope.launch {
            try {
                val api = app.authedTraktApi
                api.addToListItems(slug, mapOf(traktKey(mediaType) to listOf(mapOf("ids" to mapOf("tmdb" to tmdbId)))))
                toast("Added to list")
            } catch (e: Exception) {
                toast("Failed: ${e.message}")
            }
        }
    }

    fun removeFromTraktList(tmdbId: Int, mediaType: String, slug: String) {
        viewModelScope.launch {
            try {
                val api = app.authedTraktApi
                api.removeFromListItems(slug, mapOf(traktKey(mediaType) to listOf(mapOf("ids" to mapOf("tmdb" to tmdbId)))))
                toast("Removed from list")
            } catch (e: Exception) {
                toast("Failed: ${e.message}")
            }
        }
    }

    fun addToTmdbList(tmdbId: Int, mediaType: String, listId: Int) {
        viewModelScope.launch {
            try {
                app.tmdbV4Api.addItemToList(
                    listId,
                    mapOf("items" to listOf(mapOf("media_type" to tmdbType(mediaType), "media_id" to tmdbId))),
                )
                toast("Added to TMDB list")
            } catch (e: Exception) {
                toast("Failed: ${e.message}")
            }
        }
    }

    fun loadListsContaining(tmdbId: Int, mediaType: String) {
        val s = _state.value
        if (s.listsContainingForTmdbId == tmdbId && (s.listsContaining != null || s.listsContainingLoading)) return
        // Reset all "containing" state for the new title
        _state.update {
            it.copy(
                listsContainingForTmdbId = tmdbId,
                listsContaining = null,
                listsContainingLoading = true,
                listsContainingPage = 0,
                listsContainingHasMore = false,
                listsContainingLoadingMore = false,
                listsContainingTraktId = null,
                listsContainingTraktType = null,
            )
        }
        viewModelScope.launch {
            try {
                // Public endpoints — no auth required
                val publicApi = app.traktApi
                val traktType = if (MediaType.from(mediaType) == MediaType.TV) "show" else "movie"
                val lookup = publicApi.lookupByTmdb(tmdbId, traktType)
                val traktId = lookup.firstOrNull { it.type == traktType }
                    ?.let { if (traktType == "movie") it.movie?.ids?.trakt else it.show?.ids?.trakt }
                if (traktId == null) {
                    _state.update { it.copy(listsContaining = emptyList(), listsContainingLoading = false) }
                    return@launch
                }
                val traktIdStr = traktId.toString()
                val response = if (traktType == "movie") publicApi.movieLists(traktIdStr)
                               else publicApi.showLists(traktIdStr)
                val pageCount = response.headers()["X-Pagination-Page-Count"]?.toIntOrNull() ?: 1
                // Mark lists the authenticated user already likes (skip if not logged in)
                val likedIds = runCatching {
                    val token = app.prefs.traktAccessToken.first()
                    if (token.isBlank()) return@runCatching emptyList()
                    app.authedTraktApi.likedLists(page = 1, limit = 100).body().orEmpty().mapNotNull { it.list.ids.trakt }
                }.getOrDefault(emptyList())
                _state.update {
                    it.copy(
                        listsContaining = response.body().orEmpty(),
                        listsContainingLoading = false,
                        listsContainingPage = 1,
                        listsContainingHasMore = 1 < pageCount,
                        listsContainingTraktId = traktIdStr,
                        listsContainingTraktType = traktType,
                        likedListIds = likedIds.toSet(),
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(listsContaining = emptyList(), listsContainingLoading = false) }
                toast("Failed to find lists: ${e.message}")
            }
        }
    }

    fun loadMoreListsContaining() {
        val s = _state.value
        if (s.listsContainingLoadingMore || !s.listsContainingHasMore) return
        val traktId = s.listsContainingTraktId ?: return
        val traktType = s.listsContainingTraktType ?: return
        val nextPage = s.listsContainingPage + 1
        viewModelScope.launch {
            _state.update { it.copy(listsContainingLoadingMore = true) }
            try {
                val response = if (traktType == "movie") app.traktApi.movieLists(traktId, page = nextPage)
                               else app.traktApi.showLists(traktId, page = nextPage)
                val pageCount = response.headers()["X-Pagination-Page-Count"]?.toIntOrNull() ?: nextPage
                _state.update {
                    it.copy(
                        listsContaining = (it.listsContaining ?: emptyList()) + response.body().orEmpty(),
                        listsContainingPage = nextPage,
                        listsContainingHasMore = nextPage < pageCount,
                        listsContainingLoadingMore = false,
                    )
                }
            } catch (_: Exception) {
                _state.update { it.copy(listsContainingLoadingMore = false) }
            }
        }
    }

    fun openList(list: TraktList) {
        _state.update { it.copy(openedList = list, openedListItems = null, openedListLoading = true) }
        viewModelScope.launch {
            try {
                val user = list.user?.pathId ?: run {
                    _state.update { it.copy(openedListItems = emptyList(), openedListLoading = false) }
                    return@launch
                }
                val response = app.traktApi.listItems(user, list.ids.slug)
                _state.update { it.copy(openedListItems = response.body().orEmpty(), openedListLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(openedListItems = emptyList(), openedListLoading = false) }
                toast("Failed to load list: ${e.message}")
            }
        }
    }

    fun closeList() {
        _state.update { it.copy(openedList = null, openedListItems = null, openedListLoading = false) }
    }

    fun toggleListLike(list: TraktList) {
        viewModelScope.launch {
            val traktId = list.ids.trakt ?: return@launch
            val user = list.user?.pathId
            if (user == null) {
                toast("Can't like \"${list.name}\" — unknown owner")
                return@launch
            }
            val api = app.authedTraktApi
            val isLiked = traktId in _state.value.likedListIds
            try {
                if (isLiked) {
                    api.unlikeList(user, traktId.toString())
                    _state.update { it.copy(likedListIds = it.likedListIds - traktId) }
                    toast("Unliked \"${list.name}\"")
                } else {
                    api.likeList(user, traktId.toString())
                    _state.update { it.copy(likedListIds = it.likedListIds + traktId) }
                    toast("Liked \"${list.name}\"")
                }
            } catch (e: Exception) {
                toast("Failed: ${e.message}")
            }
        }
    }

    fun removeFromTmdbList(tmdbId: Int, mediaType: String, listId: Int) {
        viewModelScope.launch {
            try {
                app.tmdbV4Api.removeItemFromList(
                    listId,
                    mapOf("items" to listOf(mapOf("media_type" to tmdbType(mediaType), "media_id" to tmdbId))),
                )
                toast("Removed from TMDB list")
            } catch (e: Exception) {
                toast("Failed: ${e.message}")
            }
        }
    }
}
