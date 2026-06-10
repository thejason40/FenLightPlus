package com.fenlight.companion.ui.setup

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fenlight.companion.FenLightApp
import com.fenlight.companion.data.api.KodiRpc
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class SetupState(
    // Kodi
    val kodiHost: String = "",
    val kodiPort: String = "8080",
    val kodiUser: String = "",
    val kodiPass: String = "",
    val kodiTesting: Boolean = false,
    val kodiConnected: Boolean = false,
    val kodiError: String? = null,
    // Kodi discovery
    val kodiScanning: Boolean = false,
    val kodiDiscovered: List<DiscoveredKodi> = emptyList(),
    val showDiscoverySheet: Boolean = false,
    // TMDB
    val tmdbLoading: Boolean = false,
    val tmdbPolling: Boolean = false,
    val tmdbAuthed: Boolean = false,
    val tmdbAuthUrl: String? = null,
    val tmdbError: String? = null,
    val tmdbUsername: String = "",
    // Trakt
    val traktLoading: Boolean = false,
    val traktPolling: Boolean = false,
    val traktAuthed: Boolean = false,
    val traktUserCode: String = "",
    val traktError: String? = null,
    val traktUsername: String = "",
    // Real Debrid
    val rdLoading: Boolean = false,
    val rdPolling: Boolean = false,
    val rdAuthed: Boolean = false,
    val rdUserCode: String = "",
    val rdDirectVerificationUrl: String = "",
    val rdError: String? = null,
    val rdUsername: String = "",
    val setupComplete: Boolean = false,
)

class SetupViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as FenLightApp
    private val prefs = app.prefs

    private val _state = MutableStateFlow(SetupState())
    val state: StateFlow<SetupState> = _state.asStateFlow()

    private var traktPollJob: Job? = null
    private var rdPollJob: Job? = null
    private var scanJob: Job? = null

    init {
        viewModelScope.launch {
            combine(
                prefs.kodiHost,
                prefs.kodiPort,
                prefs.kodiUser,
                prefs.kodiPass,
            ) { host, port, user, pass -> listOf(host, port.toString(), user, pass) }.collect { (host, port, user, pass) ->
                _state.update { it.copy(kodiHost = host, kodiPort = port, kodiUser = user, kodiPass = pass, kodiConnected = host.isNotBlank()) }
            }
        }
        viewModelScope.launch {
            prefs.tmdbAccessToken.collect { token ->
                _state.update { it.copy(tmdbAuthed = token.isNotBlank()) }
            }
        }
        viewModelScope.launch {
            prefs.traktAccessToken.collect { token ->
                _state.update { it.copy(traktAuthed = token.isNotBlank()) }
            }
        }
        viewModelScope.launch {
            prefs.rdAccessToken.collect { token ->
                _state.update { it.copy(rdAuthed = token.isNotBlank()) }
            }
        }
        viewModelScope.launch {
            prefs.tmdbUsername.collect { u -> _state.update { it.copy(tmdbUsername = u) } }
        }
        viewModelScope.launch {
            prefs.traktUsername.collect { u -> _state.update { it.copy(traktUsername = u) } }
        }
        viewModelScope.launch {
            prefs.rdUsername.collect { u -> _state.update { it.copy(rdUsername = u) } }
        }
    }

    // ── Kodi discovery ────────────────────────────────────────────────────────

    fun startKodiScan() {
        scanJob?.cancel()
        _state.update { it.copy(kodiScanning = true, kodiDiscovered = emptyList(), showDiscoverySheet = true) }
        scanJob = viewModelScope.launch {
            KodiDiscovery.scan { found ->
                _state.update { it.copy(kodiDiscovered = it.kodiDiscovered + found) }
            }
            _state.update { it.copy(kodiScanning = false) }
        }
    }

    fun selectDiscoveredKodi(found: DiscoveredKodi) {
        _state.update {
            it.copy(
                kodiHost = found.host,
                kodiPort = found.port.toString(),
                kodiConnected = false,
                kodiError = null,
                showDiscoverySheet = false,
            )
        }
    }

    fun dismissDiscoverySheet() {
        scanJob?.cancel()
        _state.update { it.copy(showDiscoverySheet = false, kodiScanning = false) }
    }

    fun onKodiHostChange(v: String) {
        if (v == "testing") { _state.update { it.copy(kodiHost = v, kodiConnected = true, setupComplete = true) }; return }
        _state.update { it.copy(kodiHost = v, kodiConnected = false, kodiError = null) }
    }
    fun onKodiPortChange(v: String) = _state.update { it.copy(kodiPort = v, kodiConnected = false) }
    fun onKodiUserChange(v: String) = _state.update { it.copy(kodiUser = v) }
    fun onKodiPassChange(v: String) = _state.update { it.copy(kodiPass = v) }

    fun testKodiConnection() {
        viewModelScope.launch {
            _state.update { it.copy(kodiTesting = true, kodiError = null) }
            val port = _state.value.kodiPort.toIntOrNull() ?: 8080
            val kodi = KodiRpc(_state.value.kodiHost, port, _state.value.kodiUser, _state.value.kodiPass)
            val ok = kodi.ping()
            if (ok) {
                prefs.saveKodiConnection(_state.value.kodiHost, port, _state.value.kodiUser, _state.value.kodiPass)
                _state.update { it.copy(kodiConnected = true, kodiTesting = false) }
            } else {
                _state.update { it.copy(kodiTesting = false, kodiError = "Could not reach Kodi. Check IP/port and that HTTP control is enabled.") }
            }
        }
    }

    // ── TMDB OAuth ────────────────────────────────────────────────────────────

    fun startTmdbAuth() {
        viewModelScope.launch {
            _state.update { it.copy(tmdbLoading = true, tmdbError = null) }
            try {
                val tokenResp = app.tmdbV4Api.createRequestToken(emptyMap())
                val requestToken = tokenResp.requestToken
                prefs.saveTmdbRequestToken(requestToken)
                val authUrl = "https://www.themoviedb.org/auth/access?request_token=$requestToken"
                _state.update { it.copy(tmdbLoading = false, tmdbPolling = true, tmdbAuthUrl = authUrl) }
                // Clear the URL so LaunchedEffect only triggers once
                delay(500)
                _state.update { it.copy(tmdbAuthUrl = null) }
            } catch (e: Exception) {
                _state.update { it.copy(tmdbLoading = false, tmdbError = "Failed to start TMDB auth: ${e.message}") }
            }
        }
    }

    fun completeTmdbAuth() {
        viewModelScope.launch {
            _state.update { it.copy(tmdbLoading = true, tmdbError = null) }
            try {
                val requestToken = prefs.tmdbRequestToken.first()
                val resp = app.tmdbV4Api.createAccessToken(mapOf("request_token" to requestToken))
                if (resp.success) {
                    prefs.saveTmdbSession(resp.accessToken, resp.accountId)
                    _state.update { it.copy(tmdbLoading = false, tmdbPolling = false, tmdbAuthed = true) }
                    runCatching {
                        // Session was saved above, so the shared client picks up the new token
                        val acctInfo = app.tmdbV4Api.accountDetails(resp.accountId)
                        val name = acctInfo.username ?: acctInfo.name ?: ""
                        if (name.isNotBlank()) prefs.saveTmdbUsername(name)
                    }
                } else {
                    _state.update { it.copy(tmdbLoading = false, tmdbError = "TMDB authorisation not approved yet. Please approve in your browser first.") }
                }
            } catch (e: Exception) {
                _state.update { it.copy(tmdbLoading = false, tmdbError = "TMDB auth failed: ${e.message}") }
            }
        }
    }

    fun cancelTmdbAuth() = _state.update { it.copy(tmdbPolling = false, tmdbLoading = false) }

    fun signOutTmdb() {
        viewModelScope.launch { prefs.clearTmdbSession() }
    }

    // ── Trakt OAuth (device code) ─────────────────────────────────────────────

    fun startTraktAuth() {
        viewModelScope.launch {
            _state.update { it.copy(traktLoading = true, traktError = null) }
            try {
                val code = app.traktApi.deviceCode(mapOf("client_id" to app.traktClientId))
                _state.update { it.copy(traktLoading = false, traktPolling = true, traktUserCode = code.userCode) }
                pollTrakt(code.deviceCode, code.interval.toLong())
            } catch (e: Exception) {
                _state.update { it.copy(traktLoading = false, traktError = "Failed to start Trakt auth: ${e.message}") }
            }
        }
    }

    private fun pollTrakt(deviceCode: String, interval: Long) {
        traktPollJob?.cancel()
        traktPollJob = viewModelScope.launch {
            repeat(120) {
                delay(interval * 1000L)
                if (!_state.value.traktPolling) return@launch
                try {
                    val token = app.traktApi.deviceToken(
                        mapOf(
                            "code" to deviceCode,
                            "client_id" to app.traktClientId,
                            "client_secret" to app.traktClientSecret,
                        )
                    )
                    prefs.saveTraktTokens(token.accessToken, token.refreshToken, token.expiresIn)
                    _state.update { it.copy(traktPolling = false, traktAuthed = true) }
                    runCatching {
                        val username = app.authedTraktApi.userSettings().user.username
                        prefs.saveTraktUsername(username)
                    }
                    return@launch
                } catch (_: Exception) {
                    // 400/404 means not approved yet — keep polling
                }
            }
            _state.update { it.copy(traktPolling = false, traktError = "Timed out waiting for Trakt approval.") }
        }
    }

    fun cancelTraktAuth() {
        traktPollJob?.cancel()
        _state.update { it.copy(traktPolling = false, traktLoading = false) }
    }

    fun signOutTrakt() {
        viewModelScope.launch { prefs.clearTraktTokens() }
    }

    // ── Real Debrid OAuth (device code) ───────────────────────────────────────

    fun startRdAuth() {
        viewModelScope.launch {
            _state.update { it.copy(rdLoading = true, rdError = null) }
            try {
                val code = app.rdBaseApi.deviceCode(app.rdClientId)
                _state.update { it.copy(rdLoading = false, rdPolling = true, rdUserCode = code.userCode, rdDirectVerificationUrl = code.directVerificationUrl ?: "") }
                pollRd(code.deviceCode, code.interval.toLong())
            } catch (e: Exception) {
                _state.update { it.copy(rdLoading = false, rdError = "Failed to start Real Debrid auth: ${e.message}") }
            }
        }
    }

    private fun pollRd(deviceCode: String, interval: Long) {
        rdPollJob?.cancel()
        rdPollJob = viewModelScope.launch {
            repeat(120) {
                delay(interval * 1000L)
                if (!_state.value.rdPolling) return@launch
                try {
                    val creds = app.rdBaseApi.deviceCredentials(app.rdClientId, deviceCode)
                    // Exchange credentials for token
                    val token = app.rdBaseApi.token(creds.clientId, creds.clientSecret, deviceCode)
                    prefs.saveRdTokens(
                        token.accessToken, token.refreshToken,
                        creds.clientId, creds.clientSecret, token.expiresIn,
                    )
                    _state.update { it.copy(rdPolling = false, rdAuthed = true) }
                    runCatching {
                        val rdUser = app.authedRdApi.user()
                        prefs.saveRdUsername(rdUser.username)
                    }
                    return@launch
                } catch (_: Exception) {
                    // Not approved yet
                }
            }
            _state.update { it.copy(rdPolling = false, rdError = "Timed out waiting for Real Debrid approval.") }
        }
    }

    fun cancelRdAuth() {
        rdPollJob?.cancel()
        _state.update { it.copy(rdPolling = false, rdLoading = false) }
    }

    fun signOutRd() {
        viewModelScope.launch { prefs.clearRdTokens() }
    }

    override fun onCleared() {
        traktPollJob?.cancel()
        rdPollJob?.cancel()
        scanJob?.cancel()
    }
}
