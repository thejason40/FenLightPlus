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

private const val PAGE_SIZE = 50

data class RdUiState(
    val tab: RdTab = RdTab.TORRENTS,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val torrents: List<RdTorrent> = emptyList(),
    val torrentPage: Int = 0,
    val torrentHasMore: Boolean = true,
    val torrentIsLoadingMore: Boolean = false,
    val downloads: List<RdDownload> = emptyList(),
    val downloadPage: Int = 0,
    val downloadHasMore: Boolean = true,
    val downloadIsLoadingMore: Boolean = false,
    val selectedTorrent: RdTorrentInfo? = null,
    val playMessage: String? = null,
)

class RdViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as FenLightApp
    private val _state = MutableStateFlow(RdUiState())
    val state: StateFlow<RdUiState> = _state.asStateFlow()

    private companion object { const val CACHE_MS = 24 * 60 * 60 * 1000L }
    private val tabFetchedAt = mutableMapOf<RdTab, Long>()

    init { loadTorrents() }

    private suspend fun rdApi() = app.buildAuthedRdApi(app.getValidRdAccessToken())

    fun selectTab(tab: RdTab) {
        _state.update { it.copy(tab = tab) }
        val age = System.currentTimeMillis() - (tabFetchedAt[tab] ?: 0L)
        when (tab) {
            RdTab.TORRENTS -> if (_state.value.torrents.isEmpty() || age > CACHE_MS) loadTorrents()
            RdTab.DOWNLOADS -> if (_state.value.downloads.isEmpty() || age > CACHE_MS) loadDownloads()
        }
    }

    fun refresh() {
        tabFetchedAt.remove(_state.value.tab)
        _state.update { it.copy(isRefreshing = true) }
        when (_state.value.tab) {
            RdTab.TORRENTS -> loadTorrents()
            RdTab.DOWNLOADS -> loadDownloads()
        }
    }

    fun loadTorrents() {
        _state.update { it.copy(isLoading = true, error = null, torrents = emptyList(), torrentPage = 0, torrentHasMore = true) }
        fetchTorrentPage(page = 1, append = false)
    }

    fun loadMoreTorrents() {
        val s = _state.value
        if (s.torrentIsLoadingMore || !s.torrentHasMore) return
        fetchTorrentPage(page = s.torrentPage + 1, append = true)
    }

    private fun fetchTorrentPage(page: Int, append: Boolean) {
        viewModelScope.launch {
            _state.update { it.copy(torrentIsLoadingMore = append, isLoading = !append) }
            try {
                val results = rdApi().torrents(limit = PAGE_SIZE, page = page)
                if (page == 1) tabFetchedAt[RdTab.TORRENTS] = System.currentTimeMillis()
                _state.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        torrentIsLoadingMore = false,
                        torrents = if (append) it.torrents + results else results,
                        torrentPage = page,
                        torrentHasMore = results.size == PAGE_SIZE,
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, isRefreshing = false, torrentIsLoadingMore = false, error = e.message) }
            }
        }
    }

    fun loadDownloads() {
        _state.update { it.copy(isLoading = true, error = null, downloads = emptyList(), downloadPage = 0, downloadHasMore = true) }
        fetchDownloadPage(page = 1, append = false)
    }

    fun loadMoreDownloads() {
        val s = _state.value
        if (s.downloadIsLoadingMore || !s.downloadHasMore) return
        fetchDownloadPage(page = s.downloadPage + 1, append = true)
    }

    private fun fetchDownloadPage(page: Int, append: Boolean) {
        viewModelScope.launch {
            _state.update { it.copy(downloadIsLoadingMore = append, isLoading = !append) }
            try {
                val results = rdApi().downloads(limit = PAGE_SIZE, page = page)
                if (page == 1) tabFetchedAt[RdTab.DOWNLOADS] = System.currentTimeMillis()
                _state.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        downloadIsLoadingMore = false,
                        downloads = if (append) it.downloads + results else results,
                        downloadPage = page,
                        downloadHasMore = results.size == PAGE_SIZE,
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, isRefreshing = false, downloadIsLoadingMore = false, error = e.message) }
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
