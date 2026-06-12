package com.fenlight.companion.data.update

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

// Host this file at https://thejason40.github.io/apk/version.json
// Example content:
// {
//   "versionName": "1.0.1",
//   "versionCode": 2,
//   "apkUrl": "https://thejason40.github.io/apk/FenLightCompanion.apk",
//   "releaseNotes": "Bug fixes and improvements",
//   "sha256": "<hex sha-256 of the APK>"
// }
data class UpdateInfo(
    val versionName: String = "",
    val versionCode: Int = 0,
    val apkUrl: String = "",
    val releaseNotes: String = "",
    // Optional: when present, the downloaded APK is verified against it before install
    val sha256: String? = null,
)

sealed class UpdateResult {
    object UpToDate : UpdateResult()
    data class Available(val info: UpdateInfo) : UpdateResult()
    data class Error(val message: String) : UpdateResult()
}

class UpdateChecker(
    private val versionUrl: String = DEFAULT_VERSION_URL,
    private val client: OkHttpClient = defaultClient(),
) {
    companion object {
        const val DEFAULT_VERSION_URL = "https://thejason40.github.io/apk/version.json"

        private fun defaultClient() = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    suspend fun check(currentVersionCode: Int): UpdateResult = withContext(Dispatchers.IO) {
        try {
            val response = client.newCall(
                Request.Builder()
                    .url(versionUrl)
                    .build()
            ).execute()
            if (!response.isSuccessful) return@withContext UpdateResult.Error("HTTP ${response.code}")
            val body = response.body?.string()
                ?: return@withContext UpdateResult.Error("Empty response")
            val info = moshi.adapter(UpdateInfo::class.java).fromJson(body)
                ?: return@withContext UpdateResult.Error("Invalid response format")
            if (info.versionCode == 0 || info.versionName.isBlank() || info.apkUrl.isBlank())
                return@withContext UpdateResult.Error("Incomplete update manifest")
            if (info.versionCode > currentVersionCode) UpdateResult.Available(info)
            else UpdateResult.UpToDate
        } catch (e: Exception) {
            UpdateResult.Error(e.message ?: "Network error")
        }
    }
}
