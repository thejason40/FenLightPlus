package com.fenlight.companion.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TraktDeviceCode(
    @Json(name = "device_code") val deviceCode: String,
    @Json(name = "user_code") val userCode: String,
    @Json(name = "verification_url") val verificationUrl: String,
    @Json(name = "expires_in") val expiresIn: Int,
    val interval: Int,
)

@JsonClass(generateAdapter = true)
data class TraktToken(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "refresh_token") val refreshToken: String,
    @Json(name = "expires_in") val expiresIn: Long,
    @Json(name = "token_type") val tokenType: String,
    val scope: String,
)

@JsonClass(generateAdapter = true)
data class TraktList(
    val name: String,
    val description: String,
    val slug: String,
    val user: TraktUser?,
    @Json(name = "item_count") val itemCount: Int,
    @Json(name = "likes") val likes: Int,
)

@JsonClass(generateAdapter = true)
data class TraktUser(
    val username: String,
    val name: String?,
)

@JsonClass(generateAdapter = true)
data class TraktListItem(
    val type: String,
    val movie: TraktMovie?,
    val show: TraktShow?,
)

@JsonClass(generateAdapter = true)
data class TraktMovie(
    val title: String,
    val year: Int?,
    val ids: TraktIds,
)

@JsonClass(generateAdapter = true)
data class TraktShow(
    val title: String,
    val year: Int?,
    val ids: TraktIds,
)

@JsonClass(generateAdapter = true)
data class TraktIds(
    val trakt: Int?,
    val slug: String?,
    val tmdb: Int?,
    val imdb: String?,
    val tvdb: Int?,
)

@JsonClass(generateAdapter = true)
data class TraktCalendarEpisode(
    @Json(name = "first_aired") val firstAired: String,
    val episode: TraktEpisode,
    val show: TraktShow,
)

@JsonClass(generateAdapter = true)
data class TraktEpisode(
    val season: Int,
    val number: Int,
    val title: String,
    val ids: TraktIds,
    val overview: String?,
    val rating: Double?,
    @Json(name = "first_aired") val firstAired: String?,
)

@JsonClass(generateAdapter = true)
data class TraktLikedList(
    val list: TraktList,
    @Json(name = "liked_at") val likedAt: String,
)
