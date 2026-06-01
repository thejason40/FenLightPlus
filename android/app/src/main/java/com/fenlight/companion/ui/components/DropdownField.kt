package com.fenlight.companion.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownField(
    label: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val currentLabel = options.firstOrNull { it.first == selected }?.second ?: options.firstOrNull()?.second ?: ""
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = currentLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (key, name) ->
                DropdownMenuItem(text = { Text(name) }, onClick = { onSelect(key); expanded = false })
            }
        }
    }
}

// Variant that shows a logo image next to each item label (for watch providers).
// logoUrlForKey returns the logo URL for a given key, or null if none.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageDropdownField(
    label: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
    logoUrlForKey: (String) -> String?,
) {
    var expanded by remember { mutableStateOf(false) }
    val currentLabel = options.firstOrNull { it.first == selected }?.second ?: options.firstOrNull()?.second ?: ""
    val currentLogoUrl = if (selected.isNotBlank()) logoUrlForKey(selected) else null
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = currentLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            leadingIcon = if (currentLogoUrl != null) {
                {
                    AsyncImage(
                        model = currentLogoUrl,
                        contentDescription = currentLabel,
                        modifier = Modifier.size(24.dp),
                    )
                }
            } else null,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (key, name) ->
                val logoUrl = if (key.isNotBlank()) logoUrlForKey(key) else null
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (logoUrl != null) {
                                AsyncImage(
                                    model = logoUrl,
                                    contentDescription = name,
                                    modifier = Modifier.size(24.dp).padding(end = 8.dp),
                                )
                            }
                            Text(name)
                        }
                    },
                    onClick = { onSelect(key); expanded = false },
                )
            }
        }
    }
}
