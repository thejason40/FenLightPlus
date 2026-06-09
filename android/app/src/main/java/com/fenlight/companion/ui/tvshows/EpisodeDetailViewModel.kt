package com.fenlight.companion.ui.tvshows

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fenlight.companion.FenLightApp
import com.fenlight.companion.data.api.KodiRpc
import com.fenlight.companion.data.model.Episode
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class EpisodeDetailUiState(
    val episode: Episode? = null,
    val showName: String = "",
    val showYear: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val playMessage: String? = null,
)

class EpisodeDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as FenLightApp
    private val _state = MutableStateFlow(EpisodeDetailUiState())
    val state: StateFlow<EpisodeDetailUiState> = _state.asStateFlow()

    private var loadedKey = Triple(-1, -1, -1)

    fun load(showId: Int, season: Int, episodeNumber: Int) {
        val key = Triple(showId, season, episodeNumber)
        if (loadedKey == key && _state.value.episode != null) return
        loadedKey = key
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                coroutineScope {
                    val epDeferred = async { app.tmdbApi.episodeDetail(showId, season, episodeNumber) }
                    val showDeferred = async { app.tmdbApi.tvDetail(showId, append = "") }
                    val ep = epDeferred.await()
                    val show = showDeferred.await()
                    _state.update {
                        it.copy(
                            isLoading = false,
                            episode = ep,
                            showName = show.name,
                            showYear = show.firstAirDate?.take(4)?.toIntOrNull() ?: 0,
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun play(showId: Int) {
        val s = _state.value
        val ep = s.episode ?: return
        viewModelScope.launch {
            try {
                val host = app.prefs.kodiHost.first()
                val port = app.prefs.kodiPort.first()
                val user = app.prefs.kodiUser.first()
                val pass = app.prefs.kodiPass.first()
                KodiRpc(host, port, user, pass)
                    .playEpisodeViaFenLight(showId, s.showName, s.showYear, ep.seasonNumber, ep.episodeNumber)
                _state.update { it.copy(playMessage = "Playing S${ep.seasonNumber}E${ep.episodeNumber} of ${s.showName} on Kodi…") }
            } catch (e: Exception) {
                _state.update { it.copy(playMessage = "Failed: ${e.message}") }
            }
        }
    }

    fun clearPlayMessage() = _state.update { it.copy(playMessage = null) }
}
