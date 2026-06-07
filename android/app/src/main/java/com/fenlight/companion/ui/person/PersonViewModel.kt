package com.fenlight.companion.ui.person

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fenlight.companion.FenLightApp
import com.fenlight.companion.data.model.TmdbPerson
import com.fenlight.companion.ui.components.PaginatedItem
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class PersonUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val person: TmdbPerson? = null,
    val credits: List<PaginatedItem> = emptyList(),
    val mediaTypeById: Map<Int, String> = emptyMap(),
)

class PersonViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as FenLightApp
    private val _state = MutableStateFlow(PersonUiState())
    val state: StateFlow<PersonUiState> = _state.asStateFlow()

    fun loadPerson(personId: Int) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val excludeAdult = app.prefs.excludeAdult.first()
                val person = app.tmdbApi.person(personId)
                val castCredits = (person.combinedCredits?.cast ?: emptyList())
                    .filter { it.mediaType == "movie" || it.mediaType == "tv" }
                    .filter { !excludeAdult || !it.adult }
                // Merge cast and director/creator/writer crew credits, dedup by id, preferring cast entries
                val crewCredits = (person.combinedCredits?.crew ?: emptyList())
                    .filter { it.job == "Director" || it.job == "Creator" || it.job == "Writer" }
                    .filter { it.mediaType == "movie" || it.mediaType == "tv" }
                    .filter { !excludeAdult || !it.adult }
                val castIds = castCredits.map { it.id }.toSet()
                val combined = castCredits + crewCredits.filter { it.id !in castIds }
                val credits = combined
                    .distinctBy { it.id }
                    .sortedByDescending { it.popularity }
                val items = credits.map { c ->
                    PaginatedItem(
                        id = c.id,
                        title = c.title ?: c.name ?: "Unknown",
                        posterUrl = FenLightApp.posterUrl(c.posterPath),
                        rating = c.voteAverage.takeIf { it > 0 },
                    )
                }
                val typeById = credits.associate { it.id to (it.mediaType ?: "movie") }
                _state.update { it.copy(isLoading = false, person = person, credits = items, mediaTypeById = typeById) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
