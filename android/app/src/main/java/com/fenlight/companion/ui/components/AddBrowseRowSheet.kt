package com.fenlight.companion.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fenlight.companion.FenLightApp
import com.fenlight.companion.data.model.DiscoverFilters
import com.fenlight.companion.data.model.Genre
import com.fenlight.companion.data.model.RowType
import com.fenlight.companion.data.model.TmdbList
import com.fenlight.companion.data.model.WatchProvider
import com.fenlight.companion.ui.media.TraktListEntry
import com.fenlight.companion.ui.media.displayName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBrowseRowSheet(
    mediaType: String,   // "movie" or "tv"
    pendingType: RowType,
    pendingLabel: String,
    pendingFilters: DiscoverFilters,
    pendingListId: Int? = null,
    pendingTraktSlug: String? = null,
    pendingTraktUser: String? = null,
    genres: List<Genre>,
    watchProviders: List<WatchProvider>,
    availableTmdbLists: List<TmdbList> = emptyList(),
    availableTraktLists: List<TraktListEntry> = emptyList(),
    onTypeChange: (RowType) -> Unit,
    onLabelChange: (String) -> Unit,
    onFiltersChange: (DiscoverFilters) -> Unit,
    onListIdChange: (Int?) -> Unit = {},
    onTraktListChange: (slug: String?, user: String?) -> Unit = { _, _ -> },
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    addRowError: String? = null,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Add Row", style = MaterialTheme.typography.titleLarge)

            // Row type dropdown — show type options appropriate for the media type
            val typeOptions: List<Pair<String, String>> = buildList {
                if (mediaType == "tv") {
                    add(RowType.ON_THE_AIR.name to RowType.ON_THE_AIR.displayName())
                    add(RowType.AIRING_TODAY.name to RowType.AIRING_TODAY.displayName())
                } else {
                    add(RowType.NOW_PLAYING.name to RowType.NOW_PLAYING.displayName())
                    add(RowType.UPCOMING.name to RowType.UPCOMING.displayName())
                }
                add(RowType.TOP_RATED.name to RowType.TOP_RATED.displayName())
                add(RowType.CUSTOM.name to RowType.CUSTOM.displayName())
                if (availableTmdbLists.isNotEmpty()) add(RowType.TMDB_LIST.name to RowType.TMDB_LIST.displayName())
                if (availableTraktLists.isNotEmpty()) add(RowType.TRAKT_LIST.name to RowType.TRAKT_LIST.displayName())
            }

            DropdownField(
                label = "Row Type",
                options = typeOptions,
                selected = pendingType.name,
                onSelect = { key ->
                    val rowType = RowType.values().firstOrNull { it.name == key } ?: RowType.CUSTOM
                    onTypeChange(rowType)
                },
            )

            // Label text field
            OutlinedTextField(
                value = pendingLabel,
                onValueChange = onLabelChange,
                label = { Text("Label (optional)") },
                placeholder = { Text("Auto-generated if blank") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            // TMDB list picker
            if (pendingType == RowType.TMDB_LIST) {
                HorizontalDivider()
                Text("TMDB List", style = MaterialTheme.typography.titleSmall)
                val listOptions = listOf("" to "Select a list…") + availableTmdbLists.map { it.id.toString() to it.name }
                DropdownField(
                    label = "List",
                    options = listOptions,
                    selected = pendingListId?.toString() ?: "",
                    onSelect = { key -> onListIdChange(key.toIntOrNull()) },
                )
            }

            // Trakt list picker
            if (pendingType == RowType.TRAKT_LIST) {
                HorizontalDivider()
                Text("Trakt List", style = MaterialTheme.typography.titleSmall)
                val traktOptions = listOf("" to "Select a list…") + availableTraktLists.map {
                    "${it.user}/${it.slug}" to it.name
                }
                val selectedKey = if (pendingTraktSlug != null) "${pendingTraktUser ?: "me"}/$pendingTraktSlug" else ""
                DropdownField(
                    label = "List",
                    options = traktOptions,
                    selected = selectedKey,
                    onSelect = { key ->
                        if (key.isBlank()) {
                            onTraktListChange(null, null)
                        } else {
                            val parts = key.split("/", limit = 2)
                            onTraktListChange(parts.getOrNull(1), parts.getOrNull(0))
                        }
                    },
                )
            }

            // Custom filters section — only when type == CUSTOM
            if (pendingType == RowType.CUSTOM) {
                HorizontalDivider()
                Text("Filters", style = MaterialTheme.typography.titleSmall)

                // Genre
                DropdownField(
                    label = "Genre",
                    options = listOf("" to "Any Genre") + genres.map { it.id.toString() to it.name },
                    selected = pendingFilters.genreId,
                    onSelect = { onFiltersChange(pendingFilters.copy(genreId = it)) },
                )

                // Service / Watch Provider
                val providerLogoMap = remember(watchProviders) {
                    watchProviders.associate {
                        it.providerId.toString() to (it.logoPath?.let { p -> FenLightApp.posterUrl(p, "w92") } ?: "")
                    }
                }
                val providerOptions = remember(watchProviders) {
                    listOf("" to "Any Service") + watchProviders.map {
                        it.providerId.toString() to it.providerName
                    }
                }
                ImageDropdownField(
                    label = "Service / Channel",
                    options = providerOptions,
                    selected = pendingFilters.watchProviderId,
                    onSelect = { onFiltersChange(pendingFilters.copy(watchProviderId = it)) },
                    logoUrlForKey = { key -> providerLogoMap[key]?.takeIf { it.isNotBlank() } },
                )

                // Year
                val yearLabel = if (mediaType == "tv") "First Air Year" else "Release Year"
                OutlinedTextField(
                    value = pendingFilters.year,
                    onValueChange = { onFiltersChange(pendingFilters.copy(year = it)) },
                    label = { Text(yearLabel) },
                    placeholder = { Text("e.g. 2023") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )

                // Sort by
                val sortOptions = if (mediaType == "tv") {
                    listOf(
                        "popularity.desc" to "Most Popular",
                        "vote_average.desc" to "Highest Rated",
                        "first_air_date.desc" to "Newest Release",
                        "vote_count.desc" to "Most Voted",
                    )
                } else {
                    listOf(
                        "popularity.desc" to "Most Popular",
                        "vote_average.desc" to "Highest Rated",
                        "release_date.desc" to "Newest Release",
                        "revenue.desc" to "Box Office",
                        "vote_count.desc" to "Most Voted",
                    )
                }
                DropdownField(
                    label = "Sort By",
                    options = sortOptions,
                    selected = pendingFilters.sortBy,
                    onSelect = { onFiltersChange(pendingFilters.copy(sortBy = it)) },
                )

                // Min rating
                DropdownField(
                    label = "Minimum Rating",
                    options = listOf(
                        "" to "Any Rating", "5" to "5+", "6" to "6+",
                        "7" to "7+", "7.5" to "7.5+", "8" to "8+", "9" to "9+",
                    ),
                    selected = pendingFilters.minRating,
                    onSelect = { onFiltersChange(pendingFilters.copy(minRating = it)) },
                )

                // Language
                DropdownField(
                    label = "Language",
                    options = listOf(
                        "" to "Any Language", "en" to "English", "es" to "Spanish",
                        "fr" to "French", "de" to "German", "it" to "Italian",
                        "pt" to "Portuguese", "ja" to "Japanese", "ko" to "Korean",
                        "zh" to "Chinese", "hi" to "Hindi",
                    ),
                    selected = pendingFilters.language,
                    onSelect = { onFiltersChange(pendingFilters.copy(language = it)) },
                )

                // TV-only filters
                if (mediaType == "tv") {
                    // Show Status (TMDB: 0=Returning Series, 1=Planned, 2=In Production, 3=Ended, 4=Cancelled, 5=Pilot)
                    DropdownField(
                        label = "Show Status",
                        options = listOf(
                            "" to "Any Status",
                            "0" to "Returning Series",
                            "1" to "Planned",
                            "2" to "In Production",
                            "3" to "Ended",
                            "4" to "Cancelled",
                            "5" to "Pilot",
                        ),
                        selected = pendingFilters.tvStatus,
                        onSelect = { onFiltersChange(pendingFilters.copy(tvStatus = it)) },
                    )

                    // Show Type (TMDB: 0=Documentary, 1=News, 2=Miniseries, 3=Reality, 4=Scripted, 5=Talk Show, 6=Video)
                    DropdownField(
                        label = "Show Type",
                        options = listOf(
                            "" to "Any Type",
                            "0" to "Documentary",
                            "1" to "News",
                            "2" to "Miniseries",
                            "3" to "Reality",
                            "4" to "Scripted",
                            "5" to "Talk Show",
                            "6" to "Video",
                        ),
                        selected = pendingFilters.tvType,
                        onSelect = { onFiltersChange(pendingFilters.copy(tvType = it)) },
                    )
                }
            }

            // Error
            if (addRowError != null) {
                Text(
                    text = addRowError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Button(
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Add Row")
            }
        }
    }
}
