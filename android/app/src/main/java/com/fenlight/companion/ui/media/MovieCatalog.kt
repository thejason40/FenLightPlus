package com.fenlight.companion.ui.media

import com.fenlight.companion.FenLightApp
import com.fenlight.companion.data.model.DiscoverFilters
import com.fenlight.companion.data.model.Genre
import com.fenlight.companion.data.model.MediaType
import com.fenlight.companion.data.model.RowType
import com.fenlight.companion.data.model.TraktListItem
import com.fenlight.companion.data.model.WatchProvider
import com.fenlight.companion.ui.components.PaginatedItem
import com.fenlight.companion.util.Pagination
import kotlinx.coroutines.flow.Flow

class MovieCatalog(app: FenLightApp) : MediaCatalog(app) {

    override val mediaType = MediaType.MOVIE
    override val newSortKey = "release_date.desc"

    override val browseRowsJson: Flow<String> get() = app.prefs.movieBrowseRowsJson
    override suspend fun saveBrowseRowsJson(json: String) = app.prefs.saveMovieBrowseRows(json)

    override suspend fun genres(): List<Genre> = app.tmdbApi.movieGenres().genres

    override suspend fun watchProviders(region: String?): List<WatchProvider> =
        app.tmdbApi.movieWatchProviders(region).results.sortedBy { it.displayPriority }

    override suspend fun standardPage(
        type: RowType,
        filters: DiscoverFilters?,
        page: Int,
        region: String?,
        excludeAdult: Boolean,
    ): CatalogPage {
        val result = when (type) {
            RowType.POPULAR -> app.tmdbApi.popularMovies(page, region)
            RowType.NOW_PLAYING -> app.tmdbApi.nowPlayingMovies(page, region)
            RowType.UPCOMING -> app.tmdbApi.upcomingMovies(page, region)
            RowType.TOP_RATED -> app.tmdbApi.topRatedMovies(page, region)
            RowType.CUSTOM -> {
                val f = filters ?: DiscoverFilters()
                val params = buildMap<String, String> {
                    if (f.genreId.isNotBlank()) put("with_genres", f.genreId)
                    if (f.year.isNotBlank()) put("primary_release_year", f.year)
                    put("sort_by", f.sortBy)
                    if (f.minRating.isNotBlank()) { put("vote_average.gte", f.minRating); put("vote_count.gte", "20") }
                    if (f.language.isNotBlank()) put("with_original_language", f.language)
                    if (f.watchProviderId.isNotBlank()) {
                        put("with_watch_providers", f.watchProviderId)
                        if (region != null) put("watch_region", region)
                    }
                    put("include_adult", (!excludeAdult).toString())
                }
                app.tmdbApi.discoverMovies(params, page, region)
            }
            else -> app.tmdbApi.popularMovies(page, region)  // TV-only / list types already handled
        }
        val items = result.results
            .filter { !excludeAdult || !it.adult }
            .map { m ->
                PaginatedItem(
                    id = m.id, title = m.title,
                    posterUrl = FenLightApp.posterUrl(m.posterPath),
                    rating = m.voteAverage.takeIf { it > 0 },
                    backdropUrl = FenLightApp.backdropUrl(m.backdropPath),
                )
            }
        return CatalogPage(items, result.totalPages)
    }

    override suspend fun searchPage(query: String, page: Int, excludeAdult: Boolean): CatalogPage {
        val result = app.tmdbApi.searchMovies(query, page, includeAdult = !excludeAdult)
        val items = result.results
            .filter { !excludeAdult || !it.adult }
            .map { m ->
                PaginatedItem(
                    id = m.id, title = m.title,
                    posterUrl = FenLightApp.posterUrl(m.posterPath),
                    rating = m.voteAverage.takeIf { it > 0 },
                    backdropUrl = FenLightApp.backdropUrl(m.backdropPath),
                )
            }
        return CatalogPage(items, result.totalPages)
    }

    override suspend fun detailItem(tmdbId: Int, excludeAdult: Boolean): PaginatedItem? {
        val detail = app.tmdbApi.movieDetail(tmdbId, append = "")
        if (excludeAdult && detail.adult) return null
        return PaginatedItem(
            id = detail.id,
            title = detail.title,
            posterUrl = FenLightApp.posterUrl(detail.posterPath),
            rating = detail.voteAverage.takeIf { it > 0 },
            backdropUrl = FenLightApp.backdropUrl(detail.backdropPath),
        )
    }

    override fun tmdbIdOf(item: TraktListItem): Int? = item.movie?.ids?.tmdb

    override suspend fun trendingIds(page: Int, countries: String?): Pair<List<Int>, String?> {
        val response = app.traktApi.moviesTrending(page, countries = countries)
        val ids = (response.body() ?: emptyList()).mapNotNull { it.movie.ids.tmdb }
        return ids to response.headers()[Pagination.TRAKT_PAGE_COUNT_HEADER]
    }
}
