package com.fenlight.companion

import android.app.Application
import com.fenlight.companion.data.api.RealDebridApi
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

    // Bundled TMDB default key (same as FenLightPlus uses)
    private val tmdbDefaultApiKey = "b370b60447737762ca38457bd77579b3"
    // Bundled TMDB v4 read-access token (same as FenLightPlus)
    val tmdbReadAccessToken = "eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJiMzcwYjYwNDQ3NzM3NzYyY2EzODQ1N2JkNzc1NzliMyIsInN1YiI6IjYyN2FmY2E4Y2VhZjE2MDA2MTQ0NDE5MCIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.D0kl9DwnJfkMpFILEKEKSJCMqHX1y_T8sCtDjQBzEHQ"

    // Bundled Trakt credentials (same as FenLightPlus defaults)
    val traktClientId = "1038ef327e86e7f6d39d80d2eb5479bff66dd8394e813c5e0e387af0f84d89fb"
    val traktClientSecret = "8d27a92e1d17334dae4a0590083a4f26401cb8f721f477a79fd3f218f8534fd1"

    // Bundled Real Debrid client ID (same as FenLightPlus)
    val rdClientId = "X245A4XAIBGVM"

    val tmdbApi: TmdbApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.themoviedb.org/3/")
            .client(
                baseOkHttp.newBuilder()
                    .addInterceptor { chain ->
                        val req = chain.request().newBuilder()
                        val url = chain.request().url.newBuilder()
                            .addQueryParameter("api_key", tmdbDefaultApiKey)
                            .build()
                        chain.proceed(req.url(url).build())
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

    companion object {
        const val TMDB_IMAGE_BASE = "https://image.tmdb.org/t/p/"
        fun posterUrl(path: String?, size: String = "w342") =
            if (path != null) "$TMDB_IMAGE_BASE$size$path" else null
        fun backdropUrl(path: String?, size: String = "w780") =
            if (path != null) "$TMDB_IMAGE_BASE$size$path" else null
    }
}
