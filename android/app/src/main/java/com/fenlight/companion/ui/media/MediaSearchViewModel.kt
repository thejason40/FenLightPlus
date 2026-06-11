package com.fenlight.companion.ui.media

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fenlight.companion.FenLightApp
import com.fenlight.companion.ui.components.PaginatedItem
import com.fenlight.companion.util.Pagination
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class MediaSearchUiState(
    val query: String = "",
    val items: List<PaginatedItem> = emptyList(),
    val isLoading: Boolean = false,
    val hasMore: Boolean = true,
    val page: Int = 0,
    val error: String? = null,
)

abstract class MediaSearchViewModel(
    application: Application,
    makeCatalog: (FenLightApp) -> MediaCatalog,
) : AndroidViewModel(application) {

    private val app = application as FenLightApp
    private val catalog = makeCatalog(app)

    private val _state = MutableStateFlow(MediaSearchUiState())
    val state: StateFlow<MediaSearchUiState> = _state.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChange(q: String) {
        _state.update { MediaSearchUiState(query = q) }
        searchJob?.cancel()
        if (q.length >= 2) {
            searchJob = viewModelScope.launch {
                delay(1000L)
                loadNextPage()
            }
        }
    }

    fun loadNextPage() {
        val s = _state.value
        if (s.isLoading || !s.hasMore || s.query.length < 2) return
        val nextPage = s.page + 1
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val excludeAdult = app.prefs.excludeAdult.first()
                val result = catalog.searchPage(s.query, nextPage, excludeAdult)
                _state.update {
                    it.copy(
                        isLoading = false,
                        items = (it.items + result.items).distinctBy { i -> i.id },
                        page = nextPage,
                        hasMore = Pagination.hasMoreByPageCount(nextPage, result.totalPages),
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
