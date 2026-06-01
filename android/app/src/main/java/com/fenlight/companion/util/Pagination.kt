package com.fenlight.companion.util

/** Small, unit-testable pagination predicates shared across paged screens. */
object Pagination {

    /** When the API returns a full page, assume there may be more (Real-Debrid style). */
    fun hasMoreByPageSize(resultCount: Int, pageSize: Int): Boolean = resultCount == pageSize

    /** When the API reports a total page count (TMDB / Trakt header style). */
    fun hasMoreByPageCount(currentPage: Int, totalPages: Int): Boolean = currentPage < totalPages
}
