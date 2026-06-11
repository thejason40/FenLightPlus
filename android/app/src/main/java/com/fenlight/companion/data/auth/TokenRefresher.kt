package com.fenlight.companion.data.auth

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class StoredTokens(
    val accessToken: String,
    val refreshToken: String,
    val expiresAtMs: Long,
)

data class RefreshedTokens(
    val accessToken: String,
    val refreshToken: String,
    val expiresInSec: Long,
)

/**
 * Serializes "give me a valid access token" across concurrent callers: returns the stored
 * token while it is comfortably inside its lifetime, otherwise refreshes and persists once.
 * Storage and the refresh call are supplied as lambdas because the Trakt and Real-Debrid
 * preference shapes differ.
 */
class TokenRefresher(
    private val readTokens: suspend () -> StoredTokens,
    private val refresh: suspend (refreshToken: String) -> RefreshedTokens,
    private val saveTokens: suspend (RefreshedTokens) -> Unit,
    private val clock: () -> Long = System::currentTimeMillis,
    private val expiryBufferMs: Long = 5 * 60 * 1000L,
) {
    private val mutex = Mutex()

    suspend fun validAccessToken(): String = mutex.withLock {
        val stored = readTokens()
        if (clock() < stored.expiresAtMs - expiryBufferMs) return@withLock stored.accessToken
        val fresh = refresh(stored.refreshToken)
        saveTokens(fresh)
        fresh.accessToken
    }
}
