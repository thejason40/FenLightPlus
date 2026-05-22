package com.fenlight.companion.ui.realdebrid

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fenlight.companion.FenLightApp
import com.fenlight.companion.data.api.KodiRpc
import com.fenlight.companion.data.model.RdDownload
import com.fenlight.companion.data.model.RdFile
import com.fenlight.companion.data.model.RdTorrent
import com.fenlight.companion.data.model.RdTorrentInfo
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class RdTab { TORRENTS, DOWNLOADS }

data class RdUiState(
    val tab: RdTab = RdTab.TORRENTS,
    val isLoading: Boolean = false,
    val error: String? = null,
    val torrents: List<RdTorrent> = emptyList(),
    val downloads: List<RdDownload> = emptyList(),
    val selectedTorrent: RdTorrentInfo? = null,
    val playMessage: String? = null,
)

class RdViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as FenLightApp
    private val _state = MutableStateFlow(RdUiState())
    val state: StateFlow<RdUiState> = _state.asStateFlow()

    init { loadTorrents() }

    private suspend fun rdApi() = app.buildAuthedRdApi(app.prefs.rdAccessToken.first())

    fun selectTab(tab: RdTab) {
        _state.update { it.copy(tab = tab) }
        when (tab) {
            RdTab.TORRENTS -> loadTorrents()
            RdTab.DOWNLOADS -> loadDownloads()
        }
    }

    fun loadTorrents() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val torrents = rdApi().torrents()
                _state.update { it.copy(isLoading = false, torrents = torrents) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private fun loadDownloads() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val downloads = rdApi().downloads()
                _state.update { it.copy(isLoading = false, downloads = downloads) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun loadTorrentInfo(id: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val info = rdApi().torrentInfo(id)
                _state.update { it.copy(isLoading = false, selectedTorrent = info) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun clearSelectedTorrent() = _state.update { it.copy(selectedTorrent = null) }

    fun playTorrentFile(torrent: RdTorrentInfo, file: RdFile) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                // The file index in links matches selected files order
                val selectedFiles = torrent.files.filter { it.selected == 1 }
                val fileIndex = selectedFiles.indexOfFirst { it.id == file.id }
                if (fileIndex < 0 || fileIndex >= torrent.links.size) {
                    _state.update { it.copy(isLoading = false, playMessage = "File not available — torrent may still be downloading.") }
                    return@launch
                }
                val link = torrent.links[fileIndex]
                val unrestricted = rdApi().unrestrictLink(link)
                sendToKodi(unrestricted.download, file.path.substringAfterLast('/'))
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, playMessage = "Failed: ${e.message}") }
            }
        }
    }

    fun playDownload(download: RdDownload) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val unrestricted = rdApi().unrestrictLink(download.link)
                sendToKodi(unrestricted.download, download.filename)
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, playMessage = "Failed: ${e.message}") }
            }
        }
    }

    private suspend fun sendToKodi(url: String, label: String) {
        val host = app.prefs.kodiHost.first()
        val port = app.prefs.kodiPort.first()
        val user = app.prefs.kodiUser.first()
        val pass = app.prefs.kodiPass.first()
        KodiRpc(host, port, user, pass).playUrl(url)
        _state.update { it.copy(isLoading = false, playMessage = "Sending \"$label\" to Kodi…") }
    }

    fun clearPlayMessage() = _state.update { it.copy(playMessage = null) }
}
