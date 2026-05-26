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

    suspend fun scan(onFound: suspend (DiscoveredKodi) -> Unit) = coroutineScope {
        val localIp = localIpv4() ?: return@coroutineScope
        val base = localIp.substringBeforeLast(".")
        (1..254).map { n ->
            async(Dispatchers.IO) {
                val host = "$base.$n"
                if (host == localIp) return@async
                if (isPortOpen(host, 8080) && pingKodi(host, 8080)) {
                    onFound(DiscoveredKodi(host, 8080, "Kodi @ $host"))
                }
            }
        }.awaitAll()
    }

    private fun isPortOpen(host: String, port: Int): Boolean = try {
        Socket().use { s -> s.connect(InetSocketAddress(host, port), 400); true }
    } catch (_: Exception) {
        false
    }

    private fun pingKodi(host: String, port: Int): Boolean = try {
        val body = """{"jsonrpc":"2.0","method":"JSONRPC.Ping","id":1}"""
        val req = Request.Builder()
            .url("http://$host:$port/jsonrpc")
            .post(body.toRequestBody(jsonType))
            .build()
        val resp = scanClient.newCall(req).execute()
        resp.isSuccessful && JSONObject(resp.body?.string() ?: "{}").optString("result") == "pong"
    } catch (_: Exception) {
        false
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
