package com.fenlight.companion.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

enum class RowType {
    POPULAR, TRENDING, NOW_PLAYING, UPCOMING, TOP_RATED,
    ON_THE_AIR, AIRING_TODAY, CUSTOM,
    TMDB_LIST, TRAKT_LIST,
}

@JsonClass(generateAdapter = true)
data class DiscoverFilters(
    val genreId: String = "",
    val year: String = "",           // movies: primary_release_year; TV: first_air_date_year
    val sortBy: String = "popularity.desc",
    val minRating: String = "",
    val language: String = "",
    val watchProviderId: String = "",     // TMDB provider_id; requires watch_region
    val tvStatus: String = "",            // TV only: with_status (0-4)
    val tvType: String = "",              // TV only: with_type (0-6)
)

@JsonClass(generateAdapter = true)
data class BrowseRowConfig(
    val id: String,
    val label: String,
    val type: RowType,
    val filters: DiscoverFilters? = null,
    val listId: Int? = null,          // TMDB_LIST: v4 list id
    val traktSlug: String? = null,    // TRAKT_LIST: list slug
    val traktUser: String? = null,    // TRAKT_LIST: "me" or owner username (for liked lists)
)

@JsonClass(generateAdapter = true)
data class WatchProvider(
    @Json(name = "provider_id") val providerId: Int,
    @Json(name = "provider_name") val providerName: String,
    @Json(name = "logo_path") val logoPath: String? = null,
    @Json(name = "display_priority") val displayPriority: Int = 999,
)

@JsonClass(generateAdapter = true)
data class WatchProviderResults(@Json(name = "results") val results: List<WatchProvider>)
