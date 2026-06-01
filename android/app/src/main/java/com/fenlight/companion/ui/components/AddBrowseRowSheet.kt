package com.fenlight.companion.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fenlight.companion.FenLightApp
import com.fenlight.companion.data.model.DiscoverFilters
import com.fenlight.companion.data.model.Genre
import com.fenlight.companion.data.model.RowType
import com.fenlight.companion.data.model.WatchProvider
import com.fenlight.companion.ui.movies.displayName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBrowseRowSheet(
    mediaType: String,   // "movie" or "tv"
    pendingType: RowType,
    pendingLabel: String,
    pendingFilters: DiscoverFilters,
    genres: List<Genre>,
    watchProviders: List<WatchProvider>,
    onTypeChange: (RowType) -> Unit,
    onLabelChange: (String) -> Unit,
    onFiltersChange: (DiscoverFilters) -> Unit,
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
            val typeOptions: List<Pair<String, String>> = if (mediaType == "tv") {
                listOf(
                    RowType.ON_THE_AIR.name to RowType.ON_THE_AIR.displayName(),
                    RowType.AIRING_TODAY.name to RowType.AIRING_TODAY.displayName(),
                    RowType.TOP_RATED.name to RowType.TOP_RATED.displayName(),
                    RowType.CUSTOM.name to RowType.CUSTOM.displayName(),
                )
            } else {
                listOf(
                    RowType.NOW_PLAYING.name to RowType.NOW_PLAYING.displayName(),
                    RowType.UPCOMING.name to RowType.UPCOMING.displayName(),
                    RowType.TOP_RATED.name to RowType.TOP_RATED.displayName(),
                    RowType.CUSTOM.name to RowType.CUSTOM.displayName(),
                )
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
                val providerLogoMap = watchProviders.associate {
                    it.providerId.toString() to (it.logoPath?.let { p -> FenLightApp.posterUrl(p, "w92") } ?: "")
                }
                val providerOptions = listOf("" to "Any Service") + watchProviders.map {
                    it.providerId.toString() to it.providerName
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
