package com.fenlight.companion.data.api

import com.fenlight.companion.data.model.*
import retrofit2.http.*

interface RealDebridApi {

    // OAuth device flow
    @GET("oauth/v2/device/code")
    suspend fun deviceCode(
        @Query("client_id") clientId: String,
        @Query("new_credentials") newCredentials: String = "yes",
    ): RdDeviceCode

    @GET("oauth/v2/device/credentials")
    suspend fun deviceCredentials(
        @Query("client_id") clientId: String,
        @Query("code") code: String,
    ): RdCredentials

    @FormUrlEncoded
    @POST("oauth/v2/token")
    suspend fun token(
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("code") code: String,
        @Field("grant_type") grantType: String = "http://oauth.net/grant_type/device/1.0",
    ): RdToken

    @FormUrlEncoded
    @POST("oauth/v2/token")
    suspend fun refreshToken(
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("code") refreshToken: String,
        @Field("grant_type") grantType: String = "http://oauth.net/grant_type/device/1.0",
    ): RdToken

    // Cloud browser
    @GET("torrents")
    suspend fun torrents(
        @Query("limit") limit: Int = 50,
        @Query("page") page: Int = 1,
    ): List<RdTorrent>

    @GET("torrents/info/{id}")
    suspend fun torrentInfo(@Path("id") id: String): RdTorrentInfo

    @GET("downloads")
    suspend fun downloads(
        @Query("limit") limit: Int = 50,
        @Query("page") page: Int = 1,
    ): List<RdDownload>

    // Unrestrict a hoster link to a direct download URL
    @FormUrlEncoded
    @POST("unrestrict/link")
    suspend fun unrestrictLink(@Field("link") link: String): RdUnrestrict

    @GET("user")
    suspend fun user(): RdUser
}
