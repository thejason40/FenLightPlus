package com.fenlight.companion.data.update

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
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
//   "releaseNotes": "Bug fixes and improvements"
// }
@JsonClass(generateAdapter = true)
data class UpdateInfo(
    @Json(name = "versionName") val versionName: String,
    @Json(name = "versionCode") val versionCode: Int,
    @Json(name = "apkUrl") val apkUrl: String,
    @Json(name = "releaseNotes") val releaseNotes: String = "",
)

sealed class UpdateResult {
    object UpToDate : UpdateResult()
    data class Available(val info: UpdateInfo) : UpdateResult()
    data class Error(val message: String) : UpdateResult()
}

class UpdateChecker {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    suspend fun check(currentVersionCode: Int): UpdateResult = withContext(Dispatchers.IO) {
        try {
            val response = client.newCall(
                Request.Builder()
                    .url("https://thejason40.github.io/apk/version.json")
                    .build()
            ).execute()
            if (!response.isSuccessful) return@withContext UpdateResult.Error("HTTP ${response.code}")
            val body = response.body?.string()
                ?: return@withContext UpdateResult.Error("Empty response")
            val info = moshi.adapter(UpdateInfo::class.java).fromJson(body)
                ?: return@withContext UpdateResult.Error("Invalid response format")
            if (info.versionCode > currentVersionCode) UpdateResult.Available(info)
            else UpdateResult.UpToDate
        } catch (e: Exception) {
            UpdateResult.Error(e.message ?: "Network error")
        }
    }
}
