package com.fenlight.companion.ui.trakt

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fenlight.companion.FenLightApp
import com.fenlight.companion.data.api.KodiRpc
import com.fenlight.companion.data.model.TraktCalendarEpisode
import com.fenlight.companion.data.model.TraktList
import com.fenlight.companion.data.model.TraktListItem
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class TraktTab { NEXT_EPISODES, MY_LISTS, LIKED_LISTS }

data class TraktUiState(
    val tab: TraktTab = TraktTab.NEXT_EPISODES,
    val isLoading: Boolean = false,
    val error: String? = null,
    val calendarEpisodes: List<TraktCalendarEpisode> = emptyList(),
    val myLists: List<TraktList> = emptyList(),
    val likedLists: List<TraktList> = emptyList(),
    val listItems: List<TraktListItem> = emptyList(),
    val selectedListName: String = "",
    val playMessage: String? = null,
)

class TraktViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as FenLightApp
    private val _state = MutableStateFlow(TraktUiState())
    val state: StateFlow<TraktUiState> = _state.asStateFlow()

    init { loadCurrentTab() }

    fun selectTab(tab: TraktTab) {
        _state.update { it.copy(tab = tab, listItems = emptyList(), selectedListName = "") }
        loadCurrentTab()
    }

    private fun loadCurrentTab() {
        when (_state.value.tab) {
            TraktTab.NEXT_EPISODES -> loadCalendar()
            TraktTab.MY_LISTS -> loadMyLists()
            TraktTab.LIKED_LISTS -> loadLikedLists()
        }
    }

    private fun loadCalendar() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val token = app.prefs.traktAccessToken.first()
                val api = app.buildAuthedTraktApi(token)
                val start = SimpleDateFormat("yyyy-MM-dd", Locale.US).let {
                    it.format(Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }.time)
                }
                val episodes = api.myCalendar(start, 21)
                _state.update { it.copy(isLoading = false, calendarEpisodes = episodes.sortedBy { ep -> ep.firstAired }) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private fun loadMyLists() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val token = app.prefs.traktAccessToken.first()
                val api = app.buildAuthedTraktApi(token)
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
                val token = app.prefs.traktAccessToken.first()
                val api = app.buildAuthedTraktApi(token)
                val liked = api.likedLists()
                _state.update { it.copy(isLoading = false, likedLists = liked.map { it.list }) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun loadListItems(slug: String, listName: String, user: String = "me") {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, selectedListName = listName) }
            try {
                val token = app.prefs.traktAccessToken.first()
                val api = app.buildAuthedTraktApi(token)
                val items = if (user == "me") api.myListItems(slug) else api.listItems(user, slug)
                _state.update { it.copy(isLoading = false, listItems = items) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun clearListItems() = _state.update { it.copy(listItems = emptyList(), selectedListName = "") }

    fun playEpisode(cal: TraktCalendarEpisode) {
        viewModelScope.launch {
            val tmdbId = cal.show.ids.tmdb ?: return@launch
            val title = cal.show.title
            val year = cal.show.year ?: 0
            val season = cal.episode.season
            val ep = cal.episode.number
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
