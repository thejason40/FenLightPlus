package com.fenlight.companion.ui.lists

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fenlight.companion.FenLightApp
import com.fenlight.companion.data.model.TmdbList
import com.fenlight.companion.data.model.TraktList
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ListManagementState(
    val hasTraktAuth: Boolean = false,
    val hasTmdbAuth: Boolean = false,
    val watchlistedIds: Set<Int> = emptySet(),
    val traktLists: List<TraktList> = emptyList(),
    val tmdbLists: List<TmdbList> = emptyList(),
    val actionMessage: String? = null,
)

class ListManagementViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as FenLightApp
    private val _state = MutableStateFlow(ListManagementState())
    val state: StateFlow<ListManagementState> = _state.asStateFlow()

    // Maps a caller-supplied mediaType to the string Trakt expects in request bodies
    private fun traktKey(mediaType: String) =
        if (mediaType == "show" || mediaType == "tv") "shows" else "movies"

    // Maps a caller-supplied mediaType to the string TMDB v4 expects
    private fun tmdbType(mediaType: String) =
        if (mediaType == "show" || mediaType == "tv") "tv" else "movie"

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
                val api = app.buildAuthedTraktApi(token)
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
                val lists = app.buildAuthedTraktApi(token).myLists()
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
                val result = app.buildTmdbV4Api(token).accountLists(accountId)
                _state.update { it.copy(tmdbLists = result.results) }
            } catch (_: Exception) {}
        }
    }

    fun addToTraktWatchlist(tmdbId: Int, mediaType: String) {
        viewModelScope.launch {
            try {
                val api = app.buildAuthedTraktApi(app.prefs.traktAccessToken.first())
                api.addToWatchlist(mapOf(traktKey(mediaType) to listOf(mapOf("ids" to mapOf("tmdb" to tmdbId)))))
                _state.update { it.copy(watchlistedIds = it.watchlistedIds + tmdbId, actionMessage = "Added to Watchlist") }
            } catch (e: Exception) {
                _state.update { it.copy(actionMessage = "Failed: ${e.message}") }
            }
        }
    }

    fun removeFromTraktWatchlist(tmdbId: Int, mediaType: String) {
        viewModelScope.launch {
            try {
                val api = app.buildAuthedTraktApi(app.prefs.traktAccessToken.first())
                api.removeFromWatchlist(mapOf(traktKey(mediaType) to listOf(mapOf("ids" to mapOf("tmdb" to tmdbId)))))
                _state.update { it.copy(watchlistedIds = it.watchlistedIds - tmdbId, actionMessage = "Removed from Watchlist") }
            } catch (e: Exception) {
                _state.update { it.copy(actionMessage = "Failed: ${e.message}") }
            }
        }
    }

    fun addToTraktList(tmdbId: Int, mediaType: String, slug: String) {
        viewModelScope.launch {
            try {
                val api = app.buildAuthedTraktApi(app.prefs.traktAccessToken.first())
                api.addToListItems(slug, mapOf(traktKey(mediaType) to listOf(mapOf("ids" to mapOf("tmdb" to tmdbId)))))
                _state.update { it.copy(actionMessage = "Added to list") }
            } catch (e: Exception) {
                _state.update { it.copy(actionMessage = "Failed: ${e.message}") }
            }
        }
    }

    fun removeFromTraktList(tmdbId: Int, mediaType: String, slug: String) {
        viewModelScope.launch {
            try {
                val api = app.buildAuthedTraktApi(app.prefs.traktAccessToken.first())
                api.removeFromListItems(slug, mapOf(traktKey(mediaType) to listOf(mapOf("ids" to mapOf("tmdb" to tmdbId)))))
                _state.update { it.copy(actionMessage = "Removed from list") }
            } catch (e: Exception) {
                _state.update { it.copy(actionMessage = "Failed: ${e.message}") }
            }
        }
    }

    fun addToTmdbList(tmdbId: Int, mediaType: String, listId: Int) {
        viewModelScope.launch {
            try {
                val token = app.prefs.tmdbAccessToken.first()
                app.buildTmdbV4Api(token).addItemToList(
                    listId,
                    mapOf("items" to listOf(mapOf("media_type" to tmdbType(mediaType), "media_id" to tmdbId))),
                )
                _state.update { it.copy(actionMessage = "Added to TMDB list") }
            } catch (e: Exception) {
                _state.update { it.copy(actionMessage = "Failed: ${e.message}") }
            }
        }
    }

    fun removeFromTmdbList(tmdbId: Int, mediaType: String, listId: Int) {
        viewModelScope.launch {
            try {
                val token = app.prefs.tmdbAccessToken.first()
                app.buildTmdbV4Api(token).removeItemFromList(
                    listId,
                    mapOf("items" to listOf(mapOf("media_type" to tmdbType(mediaType), "media_id" to tmdbId))),
                )
                _state.update { it.copy(actionMessage = "Removed from TMDB list") }
            } catch (e: Exception) {
                _state.update { it.copy(actionMessage = "Failed: ${e.message}") }
            }
        }
    }

    fun clearActionMessage() = _state.update { it.copy(actionMessage = null) }
}
