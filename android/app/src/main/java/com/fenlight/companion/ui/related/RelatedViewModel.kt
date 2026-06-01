package com.fenlight.companion.ui.related

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fenlight.companion.FenLightApp
import com.fenlight.companion.ui.components.PaginatedItem
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class RelatedUiState(
    val title: String = "",
    val items: List<PaginatedItem> = emptyList(),
    val isLoading: Boolean = false,
    val hasMore: Boolean = true,
    val page: Int = 0,
    val error: String? = null,
)

class RelatedViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as FenLightApp
    private val _state = MutableStateFlow(RelatedUiState())
    val state: StateFlow<RelatedUiState> = _state.asStateFlow()

    private var mediaType: String = "movie"
    private var mediaId: Int = 0
    private var kind: String = "recommendations"
    private var initialised = false

    // mediaType: "movie" or "tv"; kind: "recommendations" or "similar"
    fun init(mediaType: String, mediaId: Int, kind: String) {
        if (initialised) return
        initialised = true
        this.mediaType = mediaType
        this.mediaId = mediaId
        this.kind = kind
        _state.update { it.copy(title = if (kind == "similar") "Similar" else "Recommendations") }
        loadNextPage()
    }

    fun loadNextPage() {
        val s = _state.value
        if (s.isLoading || !s.hasMore) return
        val nextPage = s.page + 1
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val excludeAdult = app.prefs.excludeAdult.first()
                val newItems: List<PaginatedItem>
                val totalPages: Int
                if (mediaType == "tv") {
                    val result = if (kind == "similar") app.tmdbApi.similarTv(mediaId, nextPage)
                    else app.tmdbApi.tvRecommendations(mediaId, nextPage)
                    totalPages = result.totalPages
                    newItems = result.results.map { show ->
                        PaginatedItem(
                            id = show.id,
                            title = show.name,
                            posterUrl = FenLightApp.posterUrl(show.posterPath),
                            rating = show.voteAverage.takeIf { it > 0 },
                            backdropUrl = FenLightApp.backdropUrl(show.backdropPath),
                        )
                    }
                } else {
                    val result = if (kind == "similar") app.tmdbApi.similarMovies(mediaId, nextPage)
                    else app.tmdbApi.movieRecommendations(mediaId, nextPage)
                    totalPages = result.totalPages
                    newItems = result.results
                        .filter { !excludeAdult || !it.adult }
                        .map { m ->
                            PaginatedItem(
                                id = m.id,
                                title = m.title,
                                posterUrl = FenLightApp.posterUrl(m.posterPath),
                                rating = m.voteAverage.takeIf { it > 0 },
                                backdropUrl = FenLightApp.backdropUrl(m.backdropPath),
                            )
                        }
                }
                _state.update {
                    it.copy(
                        isLoading = false,
                        items = (it.items + newItems).distinctBy { item -> item.id },
                        page = nextPage,
                        hasMore = nextPage < totalPages,
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
