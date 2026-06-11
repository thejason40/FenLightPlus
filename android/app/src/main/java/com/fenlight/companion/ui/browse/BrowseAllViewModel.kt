package com.fenlight.companion.ui.browse

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fenlight.companion.FenLightApp
import com.fenlight.companion.data.model.*
import com.fenlight.companion.ui.components.PaginatedItem
import com.fenlight.companion.ui.media.MediaCatalog
import com.fenlight.companion.ui.media.MovieCatalog
import com.fenlight.companion.ui.media.TvCatalog
import com.fenlight.companion.util.Pagination
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class BrowseAllUiState(
    val title: String = "",
    val items: List<PaginatedItem> = emptyList(),
    val isLoading: Boolean = false,
    val hasMore: Boolean = true,
    val page: Int = 0,
    val error: String? = null,
)

class BrowseAllViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as FenLightApp
    private val _state = MutableStateFlow(BrowseAllUiState())
    val state: StateFlow<BrowseAllUiState> = _state.asStateFlow()

    private var config: BrowseRowConfig? = null
    private var catalog: MediaCatalog = MovieCatalog(app)
    private var initialised = false

    fun init(rowConfig: BrowseRowConfig, mediaType: String) {
        if (initialised) return
        initialised = true
        this.config = rowConfig
        this.catalog = when (MediaType.from(mediaType)) {
            MediaType.MOVIE -> MovieCatalog(app)
            MediaType.TV -> TvCatalog(app)
        }
        _state.update { it.copy(title = rowConfig.label) }
        loadNextPage()
    }

    fun loadNextPage() {
        val s = _state.value
        val cfg = config ?: return
        if (s.isLoading || !s.hasMore) return
        val nextPage = s.page + 1
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val region = app.prefs.region.first().takeIf { it.isNotBlank() }
                val excludeAdult = app.prefs.excludeAdult.first()

                val (newItems, hasMore) = when (cfg.type) {
                    RowType.TMDB_LIST -> {
                        val listId = cfg.listId ?: return@launch
                        val result = catalog.tmdbListPage(listId, nextPage)
                        result.items to Pagination.hasMoreByPageCount(nextPage, result.totalPages)
                    }
                    RowType.TRAKT_LIST -> {
                        val slug = cfg.traktSlug ?: return@launch
                        val result = catalog.traktListPage(slug, cfg.traktUser ?: "me", nextPage, excludeAdult)
                        result.items to Pagination.hasMoreByTraktHeader(result.pageCountHeader, nextPage)
                    }
                    RowType.TRENDING -> {
                        val result = catalog.trendingPage(nextPage, region, excludeAdult)
                        result.items to Pagination.hasMoreByTraktHeader(result.pageCountHeader, nextPage)
                    }
                    else -> {
                        val result = catalog.standardPage(cfg.type, cfg.filters, nextPage, region, excludeAdult)
                        result.items to Pagination.hasMoreByPageCount(nextPage, result.totalPages)
                    }
                }
                _state.update {
                    it.copy(
                        isLoading = false,
                        items = (it.items + newItems).distinctBy { i -> i.id },
                        page = nextPage,
                        hasMore = hasMore,
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
