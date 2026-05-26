package com.fenlight.companion.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class KodiRpc(
    private val host: String,
    private val port: Int,
    private val username: String = "",
    private val password: String = "",
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val endpoint get() = "http://$host:$port/jsonrpc"
    private val json = "application/json".toMediaType()

    private suspend fun call(method: String, params: JSONObject? = null): JSONObject = withContext(Dispatchers.IO) {
        val body = JSONObject().apply {
            put("jsonrpc", "2.0")
            put("id", 1)
            put("method", method)
            if (params != null) put("params", params)
        }
        val reqBuilder = Request.Builder()
            .url(endpoint)
            .post(body.toString().toRequestBody(json))
        if (username.isNotBlank()) {
            reqBuilder.header("Authorization", Credentials.basic(username, password))
        }
        val response = client.newCall(reqBuilder.build()).execute()
        if (!response.isSuccessful) throw IOException("Kodi returned ${response.code}")
        JSONObject(response.body?.string() ?: "{}")
    }

    suspend fun ping(): Boolean = try {
        val result = call("JSONRPC.Ping")
        result.optString("result") == "pong"
    } catch (_: Exception) {
        false
    }

    suspend fun playUrl(url: String) {
        val item = JSONObject().put("file", url)
        val params = JSONObject().put("item", item)
        call("Player.Open", params)
    }

    suspend fun playMovieViaFenLight(tmdbId: Int, title: String, year: Int) {
        val pluginUrl = buildString {
            append("plugin://plugin.video.fenlight/?mode=playback.media")
            append("&media_type=movie&autoplay=true")
            append("&tmdb_id=$tmdbId")
            append("&title=${title.encodeUrl()}")
            append("&year=$year")
        }
        playUrl(pluginUrl)
    }

    suspend fun playEpisodeViaFenLight(
        tmdbId: Int,
        title: String,
        year: Int,
        season: Int,
        episode: Int,
    ) {
        val pluginUrl = buildString {
            append("plugin://plugin.video.fenlight/?mode=playback.media")
            append("&media_type=episode&autoplay=true")
            append("&tmdb_id=$tmdbId")
            append("&title=${title.encodeUrl()}")
            append("&year=$year")
            append("&season=$season")
            append("&episode=$episode")
        }
        playUrl(pluginUrl)
    }

    private fun String.encodeUrl() = java.net.URLEncoder.encode(this, "UTF-8")
}
