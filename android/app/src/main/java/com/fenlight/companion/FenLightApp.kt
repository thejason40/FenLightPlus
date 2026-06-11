package com.fenlight.companion

import android.app.Application
import com.fenlight.companion.data.api.RealDebridApi
import com.fenlight.companion.data.auth.RefreshedTokens
import com.fenlight.companion.data.auth.StoredTokens
import com.fenlight.companion.data.auth.TokenRefresher
import kotlinx.coroutines.flow.first
import com.fenlight.companion.data.api.TmdbApi
import com.fenlight.companion.data.api.TmdbV4Api
import com.fenlight.companion.data.api.TraktApi
import com.fenlight.companion.data.prefs.AppPreferences
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.runBlocking
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class FenLightApp : Application() {

    val prefs by lazy { AppPreferences(this) }

    val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val baseOkHttp by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .cache(Cache(File(cacheDir, "http_cache"), 50L * 1024 * 1024))
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BASIC
                    })
                }
            }
            .build()
    }

	val tmdbReadAccessToken = BuildConfig.TMDB_READ_ACCESS_TOKEN
	val traktClientId = BuildConfig.TRAKT_CLIENT_ID
	val traktClientSecret = BuildConfig.TRAKT_CLIENT_SECRET
	val rdClientId = BuildConfig.RD_CLIENT_ID

    val tmdbApi: TmdbApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.themoviedb.org/3/")
            .client(
                baseOkHttp.newBuilder()
                    .addInterceptor { chain ->
                        chain.proceed(
                            chain.request().newBuilder()
                                .header("Authorization", "Bearer $tmdbReadAccessToken")
                                .build()
                        )
                    }
                    .build()
            )
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(TmdbApi::class.java)
    }

    // Shared v4 client; resolves the user token at request time, falling back to the read token.
    val tmdbV4Api: TmdbV4Api by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.themoviedb.org/4/")
            .client(
                baseOkHttp.newBuilder()
                    .addInterceptor { chain ->
                        val userToken = runBlocking { prefs.tmdbAccessToken.first() }
                        val token = userToken.ifBlank { tmdbReadAccessToken }
                        val req = chain.request().newBuilder()
                            .header("Authorization", "Bearer $token")
                            .header("Content-Type", "application/json;charset=utf-8")
                            .build()
                        chain.proceed(req)
                    }
                    .build()
            )
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(TmdbV4Api::class.java)
    }

    val traktApi: TraktApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.trakt.tv/")
            .client(
                baseOkHttp.newBuilder()
                    .addInterceptor { chain ->
                        chain.proceed(
                            chain.request().newBuilder()
                                .header("Content-Type", "application/json")
                                .header("trakt-api-version", "2")
                                .header("trakt-api-key", traktClientId)
                                .build()
                        )
                    }
                    .build()
            )
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(TraktApi::class.java)
    }

    // Shared authed client; refreshes the token at request time when close to expiry.
    val authedTraktApi: TraktApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.trakt.tv/")
            .client(
                baseOkHttp.newBuilder()
                    // Own dispatcher: the token refresh below runs on the base client, which must
                    // not queue behind authed calls blocked in this interceptor (per-host limit).
                    .dispatcher(okhttp3.Dispatcher())
                    .addInterceptor { chain ->
                        val accessToken = fetchTokenForRequest { getValidTraktAccessToken() }
                        chain.proceed(
                            chain.request().newBuilder()
                                .header("Content-Type", "application/json")
                                .header("trakt-api-version", "2")
                                .header("trakt-api-key", traktClientId)
                                .header("Authorization", "Bearer $accessToken")
                                .build()
                        )
                    }
                    .build()
            )
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(TraktApi::class.java)
    }

    // RD base (no auth) for device code / credential exchange
    val rdBaseApi: RealDebridApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.real-debrid.com/")
            .client(baseOkHttp)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(RealDebridApi::class.java)
    }

    // Shared authed client; refreshes the token at request time when close to expiry.
    val authedRdApi: RealDebridApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.real-debrid.com/rest/1.0/")
            .client(
                baseOkHttp.newBuilder()
                    // Own dispatcher: keeps the refresh call (base client) out of this client's queue.
                    .dispatcher(okhttp3.Dispatcher())
                    .addInterceptor { chain ->
                        val accessToken = fetchTokenForRequest { getValidRdAccessToken() }
                        chain.proceed(
                            chain.request().newBuilder()
                                .header("Authorization", "Bearer $accessToken")
                                .build()
                        )
                    }
                    .build()
            )
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(RealDebridApi::class.java)
    }

    // Interceptors run on OkHttp threads; surface refresh failures as IOException so
    // OkHttp delivers them to the caller instead of crashing the dispatcher thread.
    private fun fetchTokenForRequest(fetch: suspend () -> String): String = try {
        runBlocking { fetch() }
    } catch (e: IOException) {
        throw e
    } catch (e: Exception) {
        throw IOException("Token refresh failed: ${e.message}", e)
    }

    private val traktRefresher by lazy {
        TokenRefresher(
            readTokens = {
                StoredTokens(
                    accessToken = prefs.traktAccessToken.first(),
                    refreshToken = prefs.traktRefreshToken.first(),
                    expiresAtMs = prefs.traktExpiresAt.first(),
                )
            },
            refresh = { refreshToken ->
                val token = traktApi.refreshToken(mapOf(
                    "refresh_token" to refreshToken,
                    "client_id" to traktClientId,
                    "client_secret" to traktClientSecret,
                    "grant_type" to "refresh_token",
                ))
                RefreshedTokens(token.accessToken, token.refreshToken, token.expiresIn)
            },
            saveTokens = { prefs.saveTraktTokens(it.accessToken, it.refreshToken, it.expiresInSec) },
        )
    }

    private val rdRefresher by lazy {
        TokenRefresher(
            readTokens = {
                StoredTokens(
                    accessToken = prefs.rdAccessToken.first(),
                    refreshToken = prefs.rdRefreshToken.first(),
                    expiresAtMs = prefs.rdExpiresAt.first(),
                )
            },
            refresh = { refreshToken ->
                val token = rdBaseApi.refreshToken(
                    prefs.rdClientId.first(),
                    prefs.rdClientSecret.first(),
                    refreshToken,
                )
                RefreshedTokens(token.accessToken, token.refreshToken, token.expiresIn)
            },
            saveTokens = {
                prefs.saveRdTokens(
                    it.accessToken, it.refreshToken,
                    prefs.rdClientId.first(), prefs.rdClientSecret.first(), it.expiresInSec,
                )
            },
        )
    }

    suspend fun getValidTraktAccessToken(): String = traktRefresher.validAccessToken()

    suspend fun getValidRdAccessToken(): String = rdRefresher.validAccessToken()

    companion object {
        const val TMDB_IMAGE_BASE = "https://image.tmdb.org/t/p/"
        fun posterUrl(path: String?, size: String = "w342") =
            if (path != null) "$TMDB_IMAGE_BASE$size$path" else null
        fun backdropUrl(path: String?, size: String = "w780") =
            if (path != null) "$TMDB_IMAGE_BASE$size$path" else null
    }
}
