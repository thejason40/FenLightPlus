package com.fenlight.companion.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Movie(
    val id: Int,
    val title: String,
    @Json(name = "poster_path") val posterPath: String?,
    @Json(name = "backdrop_path") val backdropPath: String?,
    val overview: String,
    @Json(name = "release_date") val releaseDate: String?,
    @Json(name = "vote_average") val voteAverage: Double,
    @Json(name = "vote_count") val voteCount: Int,
    val genres: List<Genre>?,
    val runtime: Int?,
    val credits: Credits?,
    val videos: VideoResults?,
)

@JsonClass(generateAdapter = true)
data class TvShow(
    val id: Int,
    val name: String,
    @Json(name = "poster_path") val posterPath: String?,
    @Json(name = "backdrop_path") val backdropPath: String?,
    val overview: String,
    @Json(name = "first_air_date") val firstAirDate: String?,
    @Json(name = "vote_average") val voteAverage: Double,
    @Json(name = "vote_count") val voteCount: Int,
    val genres: List<Genre>?,
    @Json(name = "number_of_seasons") val numberOfSeasons: Int?,
    val seasons: List<SeasonSummary>?,
    val credits: Credits?,
    val videos: VideoResults?,
)

@JsonClass(generateAdapter = true)
data class SeasonSummary(
    @Json(name = "season_number") val seasonNumber: Int,
    val name: String,
    @Json(name = "episode_count") val episodeCount: Int,
    @Json(name = "poster_path") val posterPath: String?,
    @Json(name = "air_date") val airDate: String?,
    val overview: String,
)

@JsonClass(generateAdapter = true)
data class Season(
    @Json(name = "season_number") val seasonNumber: Int,
    val name: String,
    val overview: String,
    @Json(name = "poster_path") val posterPath: String?,
    @Json(name = "air_date") val airDate: String?,
    val episodes: List<Episode>,
)

@JsonClass(generateAdapter = true)
data class Episode(
    val id: Int,
    val name: String,
    @Json(name = "episode_number") val episodeNumber: Int,
    @Json(name = "season_number") val seasonNumber: Int,
    val overview: String,
    @Json(name = "still_path") val stillPath: String?,
    @Json(name = "air_date") val airDate: String?,
    @Json(name = "vote_average") val voteAverage: Double,
    val runtime: Int?,
)

@JsonClass(generateAdapter = true)
data class Genre(val id: Int, val name: String)

@JsonClass(generateAdapter = true)
data class Credits(
    val cast: List<CastMember>?,
    val crew: List<CrewMember>?,
)

@JsonClass(generateAdapter = true)
data class CastMember(
    val name: String,
    val character: String,
    @Json(name = "profile_path") val profilePath: String?,
)

@JsonClass(generateAdapter = true)
data class CrewMember(
    val name: String,
    val job: String,
)

@JsonClass(generateAdapter = true)
data class VideoResults(val results: List<Video>?)

@JsonClass(generateAdapter = true)
data class Video(
    val key: String,
    val site: String,
    val type: String,
)

@JsonClass(generateAdapter = true)
data class PagedResult<T>(
    val results: List<T>,
    val page: Int,
    @Json(name = "total_pages") val totalPages: Int,
    @Json(name = "total_results") val totalResults: Int,
)

@JsonClass(generateAdapter = true)
data class TmdbList(
    val id: Int,
    val name: String,
    val description: String = "",
    @Json(name = "poster_path") val posterPath: String? = null,
    @Json(name = "number_of_items") val itemCount: Int = 0,
)

@JsonClass(generateAdapter = true)
data class TmdbListDetail(
    val id: Int,
    val name: String,
    val results: List<TmdbListItem>,
    val page: Int,
    @Json(name = "total_pages") val totalPages: Int,
)

@JsonClass(generateAdapter = true)
data class TmdbListItem(
    val id: Int,
    @Json(name = "media_type") val mediaType: String,
    val title: String?,
    val name: String?,
    @Json(name = "poster_path") val posterPath: String?,
    @Json(name = "release_date") val releaseDate: String?,
    @Json(name = "first_air_date") val firstAirDate: String?,
    val overview: String,
)

@JsonClass(generateAdapter = true)
data class TmdbGenreList(val genres: List<Genre>)

@JsonClass(generateAdapter = true)
data class TmdbRequestToken(
    val success: Boolean,
    @Json(name = "request_token") val requestToken: String,
    @Json(name = "status_message") val statusMessage: String?,
)

@JsonClass(generateAdapter = true)
data class TmdbAccessToken(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "account_id") val accountId: String,
    val success: Boolean,
)

@JsonClass(generateAdapter = true)
data class TmdbV4Lists(
    val results: List<TmdbList>,
    val page: Int,
    @Json(name = "total_pages") val totalPages: Int,
)
