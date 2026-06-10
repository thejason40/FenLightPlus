package com.fenlight.companion.data.api

import com.fenlight.companion.data.model.*
import retrofit2.http.*

// TMDB v3 API — unauthenticated browsing uses the default API key as a query param.
// Personal lists use v4 with the user's access token as a Bearer header.
interface TmdbApi {

    // --- Discovery & Lists ---

    @GET("movie/popular")
    suspend fun popularMovies(@Query("page") page: Int = 1, @Query("region") region: String? = null): PagedResult<Movie>

    @GET("movie/now_playing")
    suspend fun nowPlayingMovies(@Query("page") page: Int = 1, @Query("region") region: String? = null): PagedResult<Movie>

    @GET("movie/upcoming")
    suspend fun upcomingMovies(@Query("page") page: Int = 1, @Query("region") region: String? = null): PagedResult<Movie>

    @GET("search/movie")
    suspend fun searchMovies(
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("include_adult") includeAdult: Boolean = false,
    ): PagedResult<Movie>

    @GET("movie/{id}/recommendations")
    suspend fun movieRecommendations(@Path("id") id: Int, @Query("page") page: Int = 1): PagedResult<Movie>

    @GET("movie/{id}/similar")
    suspend fun similarMovies(@Path("id") id: Int, @Query("page") page: Int = 1): PagedResult<Movie>

    @GET("discover/movie")
    suspend fun discoverMovies(
        @QueryMap filters: Map<String, String>,
        @Query("page") page: Int = 1,
        @Query("region") region: String? = null,
    ): PagedResult<Movie>

    @GET("movie/{id}")
    suspend fun movieDetail(
        @Path("id") id: Int,
        @Query("append_to_response") append: String = "credits,videos,images",
    ): Movie

    @GET("collection/{id}")
    suspend fun collectionDetail(@Path("id") id: Int): MovieCollectionDetail

    // --- TV Shows ---

    @GET("tv/popular")
    suspend fun popularTv(@Query("page") page: Int = 1, @Query("region") region: String? = null): PagedResult<TvShow>

    @GET("tv/on_the_air")
    suspend fun onTheAirTv(@Query("page") page: Int = 1, @Query("region") region: String? = null): PagedResult<TvShow>

    @GET("tv/airing_today")
    suspend fun airingTodayTv(@Query("page") page: Int = 1, @Query("region") region: String? = null): PagedResult<TvShow>

    @GET("search/tv")
    suspend fun searchTv(
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("include_adult") includeAdult: Boolean = false,
    ): PagedResult<TvShow>

    @GET("tv/{id}/recommendations")
    suspend fun tvRecommendations(@Path("id") id: Int, @Query("page") page: Int = 1): PagedResult<TvShow>

    @GET("tv/{id}/similar")
    suspend fun similarTv(@Path("id") id: Int, @Query("page") page: Int = 1): PagedResult<TvShow>

    @GET("discover/tv")
    suspend fun discoverTv(
        @QueryMap filters: Map<String, String>,
        @Query("page") page: Int = 1,
        @Query("region") region: String? = null,
    ): PagedResult<TvShow>

    @GET("tv/{id}")
    suspend fun tvDetail(
        @Path("id") id: Int,
        @Query("append_to_response") append: String = "credits,videos,images",
    ): TvShow

    @GET("tv/{id}/season/{season}")
    suspend fun seasonDetail(@Path("id") id: Int, @Path("season") season: Int): Season

    @GET("tv/{id}/season/{season}/episode/{episode}")
    suspend fun episodeDetail(
        @Path("id") showId: Int,
        @Path("season") season: Int,
        @Path("episode") episode: Int,
    ): Episode

    // --- People ---

    @GET("person/{id}")
    suspend fun person(
        @Path("id") id: Int,
        @Query("append_to_response") append: String = "combined_credits",
    ): TmdbPerson

    @GET("movie/top_rated")
    suspend fun topRatedMovies(@Query("page") page: Int = 1, @Query("region") region: String? = null): PagedResult<Movie>

    @GET("tv/top_rated")
    suspend fun topRatedTv(@Query("page") page: Int = 1, @Query("region") region: String? = null): PagedResult<TvShow>

    @GET("watch/providers/movie")
    suspend fun movieWatchProviders(@Query("watch_region") region: String? = null): WatchProviderResults

    @GET("watch/providers/tv")
    suspend fun tvWatchProviders(@Query("watch_region") region: String? = null): WatchProviderResults

    // --- Genres ---

    @GET("genre/movie/list")
    suspend fun movieGenres(): TmdbGenreList

    @GET("genre/tv/list")
    suspend fun tvGenres(): TmdbGenreList
}

// TMDB v4 API — uses Bearer token (user's access token for personal lists)
interface TmdbV4Api {

    @POST("auth/request_token")
    suspend fun createRequestToken(@Body body: Map<String, String>): TmdbRequestToken

    @POST("auth/access_token")
    suspend fun createAccessToken(@Body body: Map<String, String>): TmdbAccessToken

    @GET("account/{account_id}/lists")
    suspend fun accountLists(
        @Path("account_id") accountId: String,
        @Query("page") page: Int = 1,
    ): TmdbV4Lists

    @GET("list/{list_id}")
    suspend fun listDetail(
        @Path("list_id") listId: Int,
        @Query("page") page: Int = 1,
    ): TmdbListDetail

    @POST("list/{list_id}/items")
    suspend fun addItemToList(
        @Path("list_id") listId: Int,
        @Body body: @JvmSuppressWildcards Map<String, Any>,
    ): Any

    @HTTP(method = "DELETE", path = "list/{list_id}/items", hasBody = true)
    suspend fun removeItemFromList(
        @Path("list_id") listId: Int,
        @Body body: @JvmSuppressWildcards Map<String, Any>,
    ): Any

    @POST("list")
    suspend fun createList(@Body body: @JvmSuppressWildcards Map<String, Any>): TmdbCreateListResponse

    @DELETE("list/{list_id}")
    suspend fun deleteList(@Path("list_id") listId: Int): Any

    @GET("account/{account_id}")
    suspend fun accountDetails(@Path("account_id") accountId: String): TmdbAccountInfo
}
