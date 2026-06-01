package com.fenlight.companion.ui.tvshows

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fenlight.companion.FenLightApp
import com.fenlight.companion.data.api.KodiRpc
import com.fenlight.companion.data.model.Season
import com.fenlight.companion.data.model.TvShow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class TvDetailUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val show: TvShow? = null,
    val selectedSeason: Season? = null,
    val playMessage: String? = null,
)

/**
 * Lightweight ViewModel for the TV detail screen. Unlike [TvViewModel] it does not eagerly
 * load the popular list + genres on init, so opening a detail screen makes only the
 * single detail request.
 */
class TvDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as FenLightApp
    private val _state = MutableStateFlow(TvDetailUiState())
    val state: StateFlow<TvDetailUiState> = _state.asStateFlow()

    fun loadShowDetail(tmdbId: Int) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, selectedSeason = null) }
            try {
                val show = app.tmdbApi.tvDetail(tmdbId)
                _state.update { it.copy(isLoading = false, show = show) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun loadSeason(tmdbId: Int, seasonNumber: Int) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val season = app.tmdbApi.seasonDetail(tmdbId, seasonNumber)
                _state.update { it.copy(isLoading = false, selectedSeason = season) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun clearSelectedSeason() = _state.update { it.copy(selectedSeason = null) }

    fun playEpisode(showId: Int, showTitle: String, year: Int, season: Int, episode: Int) {
        viewModelScope.launch {
            try {
                val host = app.prefs.kodiHost.first()
                val port = app.prefs.kodiPort.first()
                val user = app.prefs.kodiUser.first()
                val pass = app.prefs.kodiPass.first()
                val kodi = KodiRpc(host, port, user, pass)
                kodi.playEpisodeViaFenLight(showId, showTitle, year, season, episode)
                _state.update { it.copy(playMessage = "Playing S${season}E${episode} of $showTitle on Kodi…") }
            } catch (e: Exception) {
                _state.update { it.copy(playMessage = "Failed: ${e.message}") }
            }
        }
    }

    fun clearPlayMessage() = _state.update { it.copy(playMessage = null) }
}
