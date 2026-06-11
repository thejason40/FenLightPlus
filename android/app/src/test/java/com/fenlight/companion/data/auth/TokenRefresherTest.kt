package com.fenlight.companion.data.auth

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.IOException

class TokenRefresherTest {

    private var now = 1_000_000L
    private var stored = StoredTokens("old-access", "old-refresh", expiresAtMs = 0L)
    private var saved: RefreshedTokens? = null
    private var refreshCalls = 0

    private fun refresher(
        refresh: suspend (String) -> RefreshedTokens = { rt ->
            refreshCalls++
            assertEquals(stored.refreshToken, rt)
            RefreshedTokens("new-access", "new-refresh", expiresInSec = 3600)
        },
    ) = TokenRefresher(
        readTokens = { stored },
        refresh = refresh,
        saveTokens = { fresh ->
            saved = fresh
            stored = StoredTokens(fresh.accessToken, fresh.refreshToken, now + fresh.expiresInSec * 1000L)
        },
        clock = { now },
    )

    private val bufferMs = 5 * 60 * 1000L

    @Test
    fun freshToken_returnedWithoutRefresh() = runTest {
        stored = stored.copy(expiresAtMs = now + bufferMs + 1)

        assertEquals("old-access", refresher().validAccessToken())
        assertEquals(0, refreshCalls)
        assertNull(saved)
    }

    @Test
    fun expiredToken_refreshedAndSaved() = runTest {
        stored = stored.copy(expiresAtMs = now - 1)

        assertEquals("new-access", refresher().validAccessToken())
        assertEquals(1, refreshCalls)
        assertEquals("new-refresh", saved!!.refreshToken)
    }

    @Test
    fun tokenInsideExpiryBuffer_refreshed() = runTest {
        stored = stored.copy(expiresAtMs = now + bufferMs - 1)

        assertEquals("new-access", refresher().validAccessToken())
        assertEquals(1, refreshCalls)
    }

    @Test
    fun tokenExactlyAtBufferBoundary_refreshed() = runTest {
        // now == expiresAt - buffer: the strict `<` comparison means refresh
        stored = stored.copy(expiresAtMs = now + bufferMs)

        assertEquals("new-access", refresher().validAccessToken())
        assertEquals(1, refreshCalls)
    }

    @Test
    fun concurrentCallers_refreshOnlyOnce() = runTest {
        stored = stored.copy(expiresAtMs = now - 1)
        val gate = CompletableDeferred<Unit>()
        val refresher = refresher(refresh = { rt ->
            refreshCalls++
            gate.await()
            RefreshedTokens("new-access", "new-refresh", expiresInSec = 3600)
        })

        val first = async { refresher.validAccessToken() }
        val second = async { refresher.validAccessToken() }
        gate.complete(Unit)

        assertEquals("new-access", first.await())
        // Second caller entered the mutex after the save, saw the fresh token, no second refresh
        assertEquals("new-access", second.await())
        assertEquals(1, refreshCalls)
    }

    @Test
    fun refreshFailure_propagatesAndSavesNothing() = runTest {
        stored = stored.copy(expiresAtMs = now - 1)
        val refresher = refresher(refresh = { throw IOException("boom") })

        assertThrows(IOException::class.java) {
            kotlinx.coroutines.runBlocking { refresher.validAccessToken() }
        }
        assertNull(saved)
    }
}
