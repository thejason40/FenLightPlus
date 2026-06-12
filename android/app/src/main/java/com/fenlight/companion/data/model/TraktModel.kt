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
data class TraktListIds(
    val trakt: Int?,
    val slug: String,
)

@JsonClass(generateAdapter = true)
data class TraktList(
    val name: String,
    val description: String = "",
    val ids: TraktListIds,
    val user: TraktUser? = null,
    @Json(name = "item_count") val itemCount: Int = 0,
    val likes: Int = 0,
) {
    // Convenience accessor — Moshi only deserialises constructor params
    val slug: String get() = ids.slug
}

@JsonClass(generateAdapter = true)
data class TraktUser(
    val username: String,
    val name: String? = null,
    val ids: TraktUserIds? = null,
) {
    // Slug is URL-safe; prefer it for path params (usernames may contain spaces)
    val pathId: String get() = ids?.slug ?: username
}

@JsonClass(generateAdapter = true)
data class TraktUserIds(
    val slug: String? = null,
)

@JsonClass(generateAdapter = true)
data class TraktListItem(
    val type: String,
    val movie: TraktMovie? = null,
    val show: TraktShow? = null,
)

@JsonClass(generateAdapter = true)
data class TraktMovie(
    val title: String,
    val year: Int? = null,
    val ids: TraktIds,
)

@JsonClass(generateAdapter = true)
data class TraktShow(
    val title: String,
    val year: Int? = null,
    val ids: TraktIds,
)

@JsonClass(generateAdapter = true)
data class TraktIds(
    val trakt: Int? = null,
    val slug: String? = null,
    val tmdb: Int? = null,
    val imdb: String? = null,
    val tvdb: Int? = null,
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
    val overview: String? = null,
    val rating: Double? = null,
    @Json(name = "first_aired") val firstAired: String? = null,
)

@JsonClass(generateAdapter = true)
data class TraktLikedList(
    val list: TraktList,
    @Json(name = "liked_at") val likedAt: String,
)

// Continue Watching models
@JsonClass(generateAdapter = true)
data class TraktWatchedShow(
    val plays: Int,
    @Json(name = "last_watched_at") val lastWatchedAt: String,
    val show: TraktShow,
    val seasons: List<TraktWatchedSeason> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class TraktShowProgress(
    val aired: Int,
    val completed: Int,
    @Json(name = "next_episode") val nextEpisode: TraktProgressEpisode?,
    @Json(name = "last_episode") val lastEpisode: TraktProgressEpisode?,
)

@JsonClass(generateAdapter = true)
data class TraktProgressEpisode(
    val season: Int,
    val number: Int,
    val title: String? = null,
    @Json(name = "first_aired") val firstAired: String? = null,
)

@JsonClass(generateAdapter = true)
data class TraktWatchedSeason(
    val number: Int,
    val episodes: List<TraktWatchedEpisode> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class TraktWatchedEpisode(
    val number: Int,
    val plays: Int,
    @Json(name = "last_watched_at") val lastWatchedAt: String? = null,
)

@JsonClass(generateAdapter = true)
data class TraktUserSettings(val user: TraktUser)

@JsonClass(generateAdapter = true)
data class TraktTrendingMovie(
    val watchers: Int,
    val movie: TraktMovie,
)

@JsonClass(generateAdapter = true)
data class TraktTrendingShow(
    val watchers: Int,
    val show: TraktShow,
)

// GET /search/list returns [{type:"list", score, list:{...}}]
@JsonClass(generateAdapter = true)
data class TraktListSearchResult(
    val type: String,
    val list: TraktList,
)

@JsonClass(generateAdapter = true)
data class TraktHistoryEntry(
    val id: Long,
    val action: String,
    @Json(name = "watched_at") val watchedAt: String,
    val type: String,
    val movie: TraktMovie? = null,
    val show: TraktShow? = null,
    val episode: TraktEpisode? = null,
)
