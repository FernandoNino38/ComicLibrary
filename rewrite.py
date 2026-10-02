import re
import sys

with open('app/src/main/java/com/example/comiclibrary/ui/library/ComicLibraryScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# Add states
state_addition = '''    var selectedSeries by remember { mutableStateOf<String?>(null) }
    var displayLimit by remember { mutableStateOf(30) }
    var isStatusMenuExpanded by remember { mutableStateOf(false) }
    var isSeriesMenuExpanded by remember { mutableStateOf(false) }'''
content = content.replace('    var selectedSeries by remember { mutableStateOf<String?>(null) }\n    var displayLimit by remember { mutableStateOf(30) }', state_addition)

# Add imports for Dropdown
imports = '''import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Text'''
if 'DropdownMenu' not in content:
    content = content.replace('import androidx.compose.material3.Text', imports)

# Replace the two filter rows with a new dropdown row
replacement_ui = '''            // Dropdown Filters
            if (comics.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Status Filter
                    Box {
                        val currentLabel = filterOptions.find { it.first == selectedFilterKey }?.second ?: "STATUS"
                        FilterChip(
                            selected = selectedFilterKey != "all",
                            onClick = { isStatusMenuExpanded = true },
                            label = {
                                Text(
                                    text = currentLabel.uppercase(),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontFamily = ComicTitleFontFamily,
                                        fontSize = 13.sp,
                                        letterSpacing = 1.sp
                                    )
                                )
                            },
                            trailingIcon = {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            },
                            shape = SquircleShape(cornerRadiusDp = 10.dp, smoothing = 0.5f),
                            border = BorderStroke(1.5.dp, if (selectedFilterKey != "all") (if (isDark) ComicYellow else ComicInkBlack) else MaterialTheme.colorScheme.outlineVariant),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ComicYellow,
                                selectedLabelColor = ComicInkBlack,
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                labelColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        DropdownMenu(
                            expanded = isStatusMenuExpanded,
                            onDismissRequest = { isStatusMenuExpanded = false }
                        ) {
                            filterOptions.forEach { (key, label) ->
                                DropdownMenuItem(
                                    text = { Text(label, fontFamily = ComicTitleFontFamily) },
                                    onClick = {
                                        selectedFilterKey = key
                                        isStatusMenuExpanded = false
                                        displayLimit = 30
                                    }
                                )
                            }
                        }
                    }

                    // Series Filter
                    if (availableSeries.isNotEmpty()) {
                        Box {
                            FilterChip(
                                selected = selectedSeries != null,
                                onClick = { isSeriesMenuExpanded = true },
                                label = {
                                    Text(
                                        text = (selectedSeries ?: "SÉRIES (TODAS)").uppercase(),
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontFamily = ComicTitleFontFamily,
                                            fontSize = 13.sp,
                                            letterSpacing = 1.sp
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                trailingIcon = {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                },
                                shape = SquircleShape(cornerRadiusDp = 10.dp, smoothing = 0.5f),
                                border = BorderStroke(1.5.dp, if (selectedSeries != null) (if (isDark) ComicYellow else ComicInkBlack) else MaterialTheme.colorScheme.outlineVariant),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ComicYellow,
                                    selectedLabelColor = ComicInkBlack,
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                    labelColor = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            DropdownMenu(
                                expanded = isSeriesMenuExpanded,
                                onDismissRequest = { isSeriesMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("TODAS AS SÉRIES", fontFamily = ComicTitleFontFamily) },
                                    onClick = {
                                        selectedSeries = null
                                        isSeriesMenuExpanded = false
                                        displayLimit = 30
                                    }
                                )
                                availableSeries.forEach { series ->
                                    DropdownMenuItem(
                                        text = { Text(series, fontFamily = ComicTitleFontFamily) },
                                        onClick = {
                                            selectedSeries = series
                                            isSeriesMenuExpanded = false
                                            displayLimit = 30
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }'''

# Remove old filter UI
regex = r'// Horizontally Scrollable Filter Chips.*?if \(availableSeries\.isNotEmpty\(\)\) \{.*?Spacer\(modifier = Modifier\.height\(14\.dp\)\)\r?\n            \}'
new_content = re.sub(regex, replacement_ui, content, flags=re.DOTALL)

with open('app/src/main/java/com/example/comiclibrary/ui/library/ComicLibraryScreen.kt', 'w', encoding='utf-8') as f:
    f.write(new_content)
