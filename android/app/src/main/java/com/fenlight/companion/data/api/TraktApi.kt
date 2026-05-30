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
    suspend fun addToWatchlist(@Body body: Map<String, Any>): Any

    @POST("sync/watchlist/remove")
    suspend fun removeFromWatchlist(@Body body: Map<String, Any>): Any

    // Add / remove items from a user's custom list
    @POST("users/me/lists/{slug}/items")
    suspend fun addToListItems(
        @Path("slug") slug: String,
        @Body body: Map<String, Any>,
    ): Any

    @POST("users/me/lists/{slug}/items/remove")
    suspend fun removeFromListItems(
        @Path("slug") slug: String,
        @Body body: Map<String, Any>,
    ): Any

    @GET("sync/watched/shows")
    suspend fun watchedShows(): List<TraktWatchedShow>

    @GET("shows/{id}/progress/watched")
    suspend fun showProgress(@Path("id") id: String): TraktShowProgress

    @POST("users/me/lists")
    suspend fun createList(@Body body: Map<String, Any>): TraktList

    @DELETE("users/me/lists/{slug}")
    suspend fun deleteList(@Path("slug") slug: String): Unit
}
