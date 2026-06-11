package com.fenlight.companion.ui.setup

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.util.concurrent.TimeUnit

data class DiscoveredKodi(val host: String, val port: Int, val name: String)

object KodiDiscovery {

    private val scanClient = OkHttpClient.Builder()
        .connectTimeout(1, TimeUnit.SECONDS)
        .readTimeout(2, TimeUnit.SECONDS)
        .build()

    private val jsonType = "application/json".toMediaType()

    suspend fun scan(
        localIp: String? = localIpv4(),
        client: OkHttpClient = scanClient,
        onFound: suspend (DiscoveredKodi) -> Unit,
    ) = coroutineScope {
        if (localIp == null) return@coroutineScope
        val base = localIp.substringBeforeLast(".")
        (1..254).map { n ->
            async(Dispatchers.IO) {
                val host = "$base.$n"
                if (host == localIp) return@async
                if (isPortOpen(host, 8080) && pingKodi(host, 8080, client)) {
                    val deviceName = fetchDeviceName(host, 8080, client)
                    val label = deviceName?.takeIf { it.isNotBlank() } ?: "Kodi @ $host"
                    onFound(DiscoveredKodi(host, 8080, label))
                }
            }
        }.awaitAll()
    }

    internal fun isPortOpen(host: String, port: Int): Boolean = try {
        Socket().use { s -> s.connect(InetSocketAddress(host, port), 400); true }
    } catch (_: Exception) {
        false
    }

    internal fun pingKodi(host: String, port: Int, client: OkHttpClient = scanClient): Boolean = try {
        val body = """{"jsonrpc":"2.0","method":"JSONRPC.Ping","id":1}"""
        val req = Request.Builder()
            .url("http://$host:$port/jsonrpc")
            .post(body.toRequestBody(jsonType))
            .build()
        client.newCall(req).execute().use { resp ->
            when {
                // Open (anonymous) Kodi: confirm via the pong response.
                resp.isSuccessful -> JSONObject(resp.body?.string() ?: "{}").optString("result") == "pong"
                // Password-protected Kodi: /jsonrpc requires Basic auth and returns 401.
                // Many non-Kodi web servers on :8080 also return 401, so only accept it
                // when the Basic-auth realm identifies Kodi (its web server uses "XBMC").
                resp.code == 401 -> {
                    val auth = resp.header("WWW-Authenticate").orEmpty().lowercase()
                    auth.contains("xbmc") || auth.contains("kodi")
                }
                else -> false
            }
        }
    } catch (_: Exception) {
        false
    }

    /**
     * The friendly name configured in Kodi → Settings → Services → Device name.
     * Returns null when the name can't be read — e.g. Kodi requires a password
     * (HTTP 401) — so discovery falls back to the "Kodi @ <ip>" label.
     */
    internal fun fetchDeviceName(host: String, port: Int, client: OkHttpClient = scanClient): String? = try {
        val body = """{"jsonrpc":"2.0","method":"Settings.GetSettingValue","params":{"setting":"services.devicename"},"id":1}"""
        val req = Request.Builder()
            .url("http://$host:$port/jsonrpc")
            .post(body.toRequestBody(jsonType))
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) null
            else JSONObject(resp.body?.string() ?: "{}")
                .optJSONObject("result")?.optString("value")?.takeIf { it.isNotBlank() }
        }
    } catch (_: Exception) {
        null
    }

    private fun localIpv4(): String? = try {
        val ifaces = NetworkInterface.getNetworkInterfaces()?.toList() ?: return null
        // Prefer wlan/eth interfaces over cellular (rmnet_*) to ensure we scan the local network
        val preferred = ifaces.filter { it.name.startsWith("wlan") || it.name.startsWith("eth") }
        val candidates = (if (preferred.isNotEmpty()) preferred else ifaces)
        candidates
            .flatMap { it.inetAddresses.toList() }
            .firstOrNull { !it.isLoopbackAddress && it is Inet4Address }
            ?.hostAddress
    } catch (_: Exception) {
        null
    }
}
