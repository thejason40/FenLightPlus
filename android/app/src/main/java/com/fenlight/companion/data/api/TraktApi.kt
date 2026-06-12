package com.fenlight.companion.data.api

import com.fenlight.companion.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface TraktApi {

    @POST("oauth/device/code")
    suspend fun deviceCode(@Body body: Map<String, String>): TraktDeviceCode

    @POST("oauth/device/token")
    suspend fun deviceToken(@Body body: Map<String, String>): TraktToken

    @POST("oauth/token")
    suspend fun refreshToken(@Body body: Map<String, String>): TraktToken

    @GET("calendars/my/shows/{start_date}/{days}")
    suspend fun myCalendar(
        @Path("start_date") startDate: String,
        @Path("days") days: Int = 14,
    ): List<TraktCalendarEpisode>

    @GET("users/me/lists")
    suspend fun myLists(): List<TraktList>

    @GET("users/me/lists/{slug}/items")
    suspend fun myListItems(
        @Path("slug") slug: String,
        @Query("extended") extended: String = "full",
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50,
    ): Response<List<TraktListItem>>

    @GET("users/likes/lists")
    suspend fun likedLists(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50,
    ): Response<List<TraktLikedList>>

    @GET("users/{user}/lists/{slug}/items")
    suspend fun listItems(
        @Path("user") user: String,
        @Path("slug") slug: String,
        @Query("extended") extended: String = "full",
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50,
    ): Response<List<TraktListItem>>

    // Watchlist
    @GET("sync/watchlist/{type}")
    suspend fun getWatchlist(
        @Path("type") type: String,
        @Query("extended") extended: String = "full",
    ): List<TraktListItem>

    @POST("sync/watchlist")
    suspend fun addToWatchlist(@Body body: @JvmSuppressWildcards Map<String, Any>): Any

    @POST("sync/watchlist/remove")
    suspend fun removeFromWatchlist(@Body body: @JvmSuppressWildcards Map<String, Any>): Any

    // Add / remove items from a user's custom list
    @POST("users/me/lists/{slug}/items")
    suspend fun addToListItems(
        @Path("slug") slug: String,
        @Body body: @JvmSuppressWildcards Map<String, Any>,
    ): Any

    @POST("users/me/lists/{slug}/items/remove")
    suspend fun removeFromListItems(
        @Path("slug") slug: String,
        @Body body: @JvmSuppressWildcards Map<String, Any>,
    ): Any

    @GET("sync/watched/shows")
    suspend fun watchedShows(): List<TraktWatchedShow>

    @GET("shows/{id}/progress/watched")
    suspend fun showProgress(@Path("id") id: String, @Query("extended") extended: String = "full"): TraktShowProgress

    @POST("users/me/lists")
    suspend fun createList(@Body body: @JvmSuppressWildcards Map<String, Any>): TraktList

    @DELETE("users/me/lists/{slug}")
    suspend fun deleteList(@Path("slug") slug: String): Unit

    @GET("users/settings")
    suspend fun userSettings(): TraktUserSettings

    @GET("sync/history")
    suspend fun history(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50,
        @Query("extended") extended: String = "full",
    ): Response<List<TraktHistoryEntry>>

    // Search public lists by name/description
    @GET("search/list")
    suspend fun searchLists(
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 30,
    ): Response<List<TraktListSearchResult>>

    // Like / unlike another user's list
    @POST("users/{user}/lists/{id}/like")
    suspend fun likeList(@Path("user") user: String, @Path("id") listId: String): Response<Unit>

    @DELETE("users/{user}/lists/{id}/like")
    suspend fun unlikeList(@Path("user") user: String, @Path("id") listId: String): Response<Unit>

    // Resolve a TMDB id to Trakt ids; type is "movie" or "show".
    // Result rows share TraktListItem's shape: {type, movie?, show?}.
    @GET("search/tmdb/{id}")
    suspend fun lookupByTmdb(
        @Path("id") tmdbId: Int,
        @Query("type") type: String,
    ): List<TraktListItem>

    // Community lists containing an item; {id} is a Trakt id or slug
    @GET("movies/{id}/lists/personal/popular")
    suspend fun movieLists(
        @Path("id") id: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50,
    ): Response<List<TraktList>>

    @GET("shows/{id}/lists/personal/popular")
    suspend fun showLists(
        @Path("id") id: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50,
    ): Response<List<TraktList>>

    @GET("movies/trending")
    suspend fun moviesTrending(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("extended") extended: String = "full",
        @Query("countries") countries: String? = null,
    ): Response<List<TraktTrendingMovie>>

    @GET("shows/trending")
    suspend fun showsTrending(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("extended") extended: String = "full",
        @Query("countries") countries: String? = null,
    ): Response<List<TraktTrendingShow>>
}
