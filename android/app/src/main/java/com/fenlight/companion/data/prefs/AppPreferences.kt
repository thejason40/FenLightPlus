package com.fenlight.companion.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "fenlight_prefs")

class AppPreferences(private val context: Context) {

    companion object {
        private val KODI_HOST = stringPreferencesKey("kodi_host")
        private val KODI_PORT = intPreferencesKey("kodi_port")
        private val KODI_USER = stringPreferencesKey("kodi_user")
        private val KODI_PASS = stringPreferencesKey("kodi_pass")

        private val TMDB_ACCESS_TOKEN = stringPreferencesKey("tmdb_access_token")
        private val TMDB_ACCOUNT_ID = stringPreferencesKey("tmdb_account_id")
        private val TMDB_REQUEST_TOKEN = stringPreferencesKey("tmdb_request_token")

        private val TRAKT_ACCESS_TOKEN = stringPreferencesKey("trakt_access_token")
        private val TRAKT_REFRESH_TOKEN = stringPreferencesKey("trakt_refresh_token")
        private val TRAKT_EXPIRES_AT = longPreferencesKey("trakt_expires_at")

        private val RD_ACCESS_TOKEN = stringPreferencesKey("rd_access_token")
        private val RD_REFRESH_TOKEN = stringPreferencesKey("rd_refresh_token")
        private val RD_CLIENT_ID = stringPreferencesKey("rd_client_id")
        private val RD_CLIENT_SECRET = stringPreferencesKey("rd_client_secret")
        private val RD_EXPIRES_AT = longPreferencesKey("rd_expires_at")

        private val CHECK_UPDATE_ON_STARTUP = booleanPreferencesKey("check_update_on_startup")
    }

    val kodiHost: Flow<String> = context.dataStore.data.map { it[KODI_HOST] ?: "" }
    val kodiPort: Flow<Int> = context.dataStore.data.map { it[KODI_PORT] ?: 8080 }
    val kodiUser: Flow<String> = context.dataStore.data.map { it[KODI_USER] ?: "" }
    val kodiPass: Flow<String> = context.dataStore.data.map { it[KODI_PASS] ?: "" }

    val tmdbAccessToken: Flow<String> = context.dataStore.data.map { it[TMDB_ACCESS_TOKEN] ?: "" }
    val tmdbAccountId: Flow<String> = context.dataStore.data.map { it[TMDB_ACCOUNT_ID] ?: "" }
    val tmdbRequestToken: Flow<String> = context.dataStore.data.map { it[TMDB_REQUEST_TOKEN] ?: "" }

    val traktAccessToken: Flow<String> = context.dataStore.data.map { it[TRAKT_ACCESS_TOKEN] ?: "" }
    val traktRefreshToken: Flow<String> = context.dataStore.data.map { it[TRAKT_REFRESH_TOKEN] ?: "" }
    val traktExpiresAt: Flow<Long> = context.dataStore.data.map { it[TRAKT_EXPIRES_AT] ?: 0L }

    val rdAccessToken: Flow<String> = context.dataStore.data.map { it[RD_ACCESS_TOKEN] ?: "" }
    val rdRefreshToken: Flow<String> = context.dataStore.data.map { it[RD_REFRESH_TOKEN] ?: "" }
    val rdClientId: Flow<String> = context.dataStore.data.map { it[RD_CLIENT_ID] ?: "" }
    val rdClientSecret: Flow<String> = context.dataStore.data.map { it[RD_CLIENT_SECRET] ?: "" }
    val rdExpiresAt: Flow<Long> = context.dataStore.data.map { it[RD_EXPIRES_AT] ?: 0L }

    val checkUpdateOnStartup: Flow<Boolean> = context.dataStore.data.map { it[CHECK_UPDATE_ON_STARTUP] ?: true }

    suspend fun setCheckUpdateOnStartup(enabled: Boolean) {
        context.dataStore.edit { it[CHECK_UPDATE_ON_STARTUP] = enabled }
    }

    suspend fun saveKodiConnection(host: String, port: Int, user: String = "", pass: String = "") {
        context.dataStore.edit {
            it[KODI_HOST] = host
            it[KODI_PORT] = port
            it[KODI_USER] = user
            it[KODI_PASS] = pass
        }
    }

    suspend fun saveTmdbSession(accessToken: String, accountId: String) {
        context.dataStore.edit {
            it[TMDB_ACCESS_TOKEN] = accessToken
            it[TMDB_ACCOUNT_ID] = accountId
        }
    }

    suspend fun saveTmdbRequestToken(token: String) {
        context.dataStore.edit { it[TMDB_REQUEST_TOKEN] = token }
    }

    suspend fun clearTmdbSession() {
        context.dataStore.edit {
            it.remove(TMDB_ACCESS_TOKEN)
            it.remove(TMDB_ACCOUNT_ID)
            it.remove(TMDB_REQUEST_TOKEN)
        }
    }

    suspend fun saveTraktTokens(accessToken: String, refreshToken: String, expiresIn: Long) {
        context.dataStore.edit {
            it[TRAKT_ACCESS_TOKEN] = accessToken
            it[TRAKT_REFRESH_TOKEN] = refreshToken
            it[TRAKT_EXPIRES_AT] = System.currentTimeMillis() + expiresIn * 1000L
        }
    }

    suspend fun clearTraktTokens() {
        context.dataStore.edit {
            it.remove(TRAKT_ACCESS_TOKEN)
            it.remove(TRAKT_REFRESH_TOKEN)
            it.remove(TRAKT_EXPIRES_AT)
        }
    }

    suspend fun saveRdTokens(
        accessToken: String,
        refreshToken: String,
        clientId: String,
        clientSecret: String,
        expiresIn: Long,
    ) {
        context.dataStore.edit {
            it[RD_ACCESS_TOKEN] = accessToken
            it[RD_REFRESH_TOKEN] = refreshToken
            it[RD_CLIENT_ID] = clientId
            it[RD_CLIENT_SECRET] = clientSecret
            it[RD_EXPIRES_AT] = System.currentTimeMillis() + expiresIn * 1000L
        }
    }

    suspend fun clearRdTokens() {
        context.dataStore.edit {
            it.remove(RD_ACCESS_TOKEN)
            it.remove(RD_REFRESH_TOKEN)
            it.remove(RD_CLIENT_ID)
            it.remove(RD_CLIENT_SECRET)
            it.remove(RD_EXPIRES_AT)
        }
    }
}
