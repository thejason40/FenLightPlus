package com.fenlight.companion.ui.setup

import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class KodiDiscoveryTest {

    private lateinit var server: MockWebServer
    private val client = OkHttpClient()

    private val host: String get() = server.hostName
    private val port: Int get() = server.port

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    // pingKodi

    @Test
    fun pingKodi_pongResponse_isKodi() {
        server.enqueue(MockResponse().setBody("""{"jsonrpc":"2.0","id":1,"result":"pong"}"""))
        assertTrue(KodiDiscovery.pingKodi(host, port, client))
    }

    @Test
    fun pingKodi_nonPongResult_isNotKodi() {
        server.enqueue(MockResponse().setBody("""{"result":"ok"}"""))
        assertFalse(KodiDiscovery.pingKodi(host, port, client))
    }

    @Test
    fun pingKodi_emptyBody_isNotKodi() {
        server.enqueue(MockResponse().setBody(""))
        assertFalse(KodiDiscovery.pingKodi(host, port, client))
    }

    @Test
    fun pingKodi_401WithXbmcRealm_isKodi() {
        server.enqueue(
            MockResponse().setResponseCode(401)
                .setHeader("WWW-Authenticate", """Basic realm="XBMC""""),
        )
        assertTrue(KodiDiscovery.pingKodi(host, port, client))
    }

    @Test
    fun pingKodi_401WithKodiRealm_isKodi() {
        server.enqueue(
            MockResponse().setResponseCode(401)
                .setHeader("WWW-Authenticate", """Basic realm="Kodi""""),
        )
        assertTrue(KodiDiscovery.pingKodi(host, port, client))
    }

    @Test
    fun pingKodi_401WithOtherRealm_isNotKodi() {
        server.enqueue(
            MockResponse().setResponseCode(401)
                .setHeader("WWW-Authenticate", """Basic realm="nginx""""),
        )
        assertFalse(KodiDiscovery.pingKodi(host, port, client))
    }

    @Test
    fun pingKodi_404_isNotKodi() {
        server.enqueue(MockResponse().setResponseCode(404))
        assertFalse(KodiDiscovery.pingKodi(host, port, client))
    }

    // fetchDeviceName

    @Test
    fun fetchDeviceName_parsesValue() {
        server.enqueue(MockResponse().setBody("""{"result":{"value":"Living Room"}}"""))
        assertEquals("Living Room", KodiDiscovery.fetchDeviceName(host, port, client))
    }

    @Test
    fun fetchDeviceName_unauthorized_returnsNull() {
        server.enqueue(MockResponse().setResponseCode(401))
        assertNull(KodiDiscovery.fetchDeviceName(host, port, client))
    }

    @Test
    fun fetchDeviceName_blankValue_returnsNull() {
        server.enqueue(MockResponse().setBody("""{"result":{"value":""}}"""))
        assertNull(KodiDiscovery.fetchDeviceName(host, port, client))
    }

    @Test
    fun fetchDeviceName_malformedJson_returnsNull() {
        server.enqueue(MockResponse().setBody("{not json"))
        assertNull(KodiDiscovery.fetchDeviceName(host, port, client))
    }
}
