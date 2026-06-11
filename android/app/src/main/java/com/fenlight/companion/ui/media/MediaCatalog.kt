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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.supervisorScope

data class CatalogPage(val items: List<PaginatedItem>, val totalPages: Int)

/** Trakt-backed pages carry their paging total in a response header. */
data class TraktPage(val items: List<PaginatedItem>, val pageCountHeader: String?)

/**
 * Everything that differs between browsing movies and TV shows — endpoints, DTO fields,
 * discover params, prefs keys — packed behind one per-medium strategy so the browse,
 * search, and see-all ViewModels can be written once.
 */
abstract class MediaCatalog(protected val app: FenLightApp) {

    abstract val mediaType: MediaType

    /** The discover sort key this medium labels "New" (release vs first-air date). */
    abstract val newSortKey: String

    abstract val browseRowsJson: Flow<String>
    abstract suspend fun saveBrowseRowsJson(json: String)
    abstract suspend fun genres(): List<Genre>
    abstract suspend fun watchProviders(region: String?): List<WatchProvider>

    /** One page of a plain TMDB row (popular/top-rated/discover/…). */
    abstract suspend fun standardPage(
        type: RowType,
        filters: DiscoverFilters?,
        page: Int,
        region: String?,
        excludeAdult: Boolean,
    ): CatalogPage

    /** One page of TMDB search results. */
    abstract suspend fun searchPage(query: String, page: Int, excludeAdult: Boolean): CatalogPage

    /**
     * Full detail for one title, as a grid item. Null when the title is adult-filtered.
     * Throws on fetch failure — callers batch through [enrichTmdbIds], which drops failures.
     */
    abstract suspend fun detailItem(tmdbId: Int, excludeAdult: Boolean): PaginatedItem?

    protected abstract fun tmdbIdOf(item: TraktListItem): Int?

    /** TMDB ids on one page of Trakt trending for this medium, plus the paging header. */
    protected abstract suspend fun trendingIds(page: Int, countries: String?): Pair<List<Int>, String?>

    /** One page of a TMDB v4 list, keeping only this medium's entries. */
    suspend fun tmdbListPage(listId: Int, page: Int): CatalogPage {
        val detail = app.tmdbV4Api.listDetail(listId, page)
        val items = detail.results
            .filter { it.mediaType == mediaType.tmdbName }
            .map { item ->
                PaginatedItem(
                    id = item.id,
                    title = item.title ?: item.name ?: "",
                    posterUrl = FenLightApp.posterUrl(item.posterPath),
                    rating = null,
                    backdropUrl = null,
                )
            }
        return CatalogPage(items, detail.totalPages)
    }

    /** One page of a Trakt list, enriched with TMDB details for this medium's entries. */
    suspend fun traktListPage(slug: String, user: String, page: Int, excludeAdult: Boolean): TraktPage {
        val traktApi = app.authedTraktApi
        val response = if (user == "me") traktApi.myListItems(slug, page = page) else traktApi.listItems(user, slug, page = page)
        val ids = (response.body() ?: emptyList()).mapNotNull { tmdbIdOf(it) }
        return TraktPage(enrichTmdbIds(ids, excludeAdult), response.headers()[Pagination.TRAKT_PAGE_COUNT_HEADER])
    }

    /** One page of Trakt trending for this medium, enriched with TMDB details. */
    suspend fun trendingPage(page: Int, region: String?, excludeAdult: Boolean): TraktPage {
        val (ids, pageCountHeader) = trendingIds(page, countries = region?.lowercase())
        return TraktPage(enrichTmdbIds(ids, excludeAdult), pageCountHeader)
    }

    /** Parallel detail fetch; individual failures and adult-filtered titles are dropped. */
    protected suspend fun enrichTmdbIds(ids: List<Int>, excludeAdult: Boolean): List<PaginatedItem> =
        supervisorScope {
            ids.map { id ->
                async { runCatching { detailItem(id, excludeAdult) }.getOrNull() }
            }.awaitAll().filterNotNull()
        }
}
