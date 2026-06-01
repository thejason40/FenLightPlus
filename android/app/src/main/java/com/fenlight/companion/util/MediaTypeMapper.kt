package com.fenlight.companion.util

/**
 * Maps the caller-supplied media type (which may be "movie", "show", or "tv" depending on
 * the source screen) to the exact strings each upstream API expects. Centralised here so
 * the Trakt/TMDB differences are defined once and unit-testable.
 */
object MediaTypeMapper {

    /** Trakt request bodies use the plural collection name. */
    fun traktKey(mediaType: String): String =
        if (mediaType == "show" || mediaType == "tv") "shows" else "movies"

    /** TMDB v4 uses the singular "tv"/"movie". */
    fun tmdbType(mediaType: String): String =
        if (mediaType == "show" || mediaType == "tv") "tv" else "movie"
}
