package com.fenlight.companion

import android.app.Application
import com.fenlight.companion.data.api.RealDebridApi
import kotlinx.coroutines.flow.first
import com.fenlight.companion.data.api.TmdbApi
import com.fenlight.companion.data.api.TmdbV4Api
import com.fenlight.companion.data.api.TraktApi
import com.fenlight.companion.data.prefs.AppPreferences
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class FenLightApp : Application() {

    val prefs by lazy { AppPreferences(this) }

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val baseOkHttp = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                })
            }
        }
        .build()

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

    fun buildTmdbV4Api(userAccessToken: String): TmdbV4Api {
        val token = userAccessToken.ifBlank { tmdbReadAccessToken }
        return Retrofit.Builder()
            .baseUrl("https://api.themoviedb.org/4/")
            .client(
                baseOkHttp.newBuilder()
                    .addInterceptor { chain ->
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

    fun buildAuthedTraktApi(accessToken: String): TraktApi {
        return Retrofit.Builder()
            .baseUrl("https://api.trakt.tv/")
            .client(
                baseOkHttp.newBuilder()
                    .addInterceptor { chain ->
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

    fun buildAuthedRdApi(accessToken: String): RealDebridApi {
        return Retrofit.Builder()
            .baseUrl("https://api.real-debrid.com/rest/1.0/")
            .client(
                baseOkHttp.newBuilder()
                    .addInterceptor { chain ->
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

    suspend fun getValidTraktAccessToken(): String {
        val expiresAt = prefs.traktExpiresAt.first()
        val accessToken = prefs.traktAccessToken.first()
        if (System.currentTimeMillis() < expiresAt - 5 * 60 * 1000L) return accessToken
        val refreshToken = prefs.traktRefreshToken.first()
        val newToken = traktApi.refreshToken(mapOf(
            "refresh_token" to refreshToken,
            "client_id" to traktClientId,
            "client_secret" to traktClientSecret,
            "grant_type" to "refresh_token",
        ))
        prefs.saveTraktTokens(newToken.accessToken, newToken.refreshToken, newToken.expiresIn)
        return newToken.accessToken
    }

    suspend fun getValidRdAccessToken(): String {
        val expiresAt = prefs.rdExpiresAt.first()
        val accessToken = prefs.rdAccessToken.first()
        if (System.currentTimeMillis() < expiresAt - 5 * 60 * 1000L) return accessToken
        val clientId = prefs.rdClientId.first()
        val clientSecret = prefs.rdClientSecret.first()
        val refreshToken = prefs.rdRefreshToken.first()
        val newToken = rdBaseApi.refreshToken(clientId, clientSecret, refreshToken)
        prefs.saveRdTokens(newToken.accessToken, newToken.refreshToken, clientId, clientSecret, newToken.expiresIn)
        return newToken.accessToken
    }

    companion object {
        const val TMDB_IMAGE_BASE = "https://image.tmdb.org/t/p/"
        fun posterUrl(path: String?, size: String = "w342") =
            if (path != null) "$TMDB_IMAGE_BASE$size$path" else null
        fun backdropUrl(path: String?, size: String = "w780") =
            if (path != null) "$TMDB_IMAGE_BASE$size$path" else null
    }
}
