package com.fenlight.companion.ui.movies

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fenlight.companion.FenLightApp
import com.fenlight.companion.ui.components.PaginatedItem
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class MovieSearchUiState(
    val query: String = "",
    val items: List<PaginatedItem> = emptyList(),
    val isLoading: Boolean = false,
    val hasMore: Boolean = true,
    val page: Int = 0,
    val error: String? = null,
)

class MovieSearchViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as FenLightApp
    private val _state = MutableStateFlow(MovieSearchUiState())
    val state: StateFlow<MovieSearchUiState> = _state.asStateFlow()

    fun onQueryChange(q: String) {
        _state.update { MovieSearchUiState(query = q) }
        if (q.length >= 2) loadNextPage()
    }

    fun loadNextPage() {
        val s = _state.value
        if (s.isLoading || !s.hasMore || s.query.length < 2) return
        val nextPage = s.page + 1
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val excludeAdult = app.prefs.excludeAdult.first()
                val result = app.tmdbApi.searchMovies(s.query, nextPage, includeAdult = !excludeAdult)
                val newItems = result.results
                    .filter { !excludeAdult || !it.adult }
                    .map { m ->
                        PaginatedItem(
                            id = m.id, title = m.title,
                            posterUrl = FenLightApp.posterUrl(m.posterPath),
                            rating = m.voteAverage.takeIf { it > 0 },
                            backdropUrl = FenLightApp.backdropUrl(m.backdropPath),
                        )
                    }
                _state.update {
                    it.copy(
                        isLoading = false,
                        items = (it.items + newItems).distinctBy { i -> i.id },
                        page = nextPage,
                        hasMore = nextPage < result.totalPages,
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
