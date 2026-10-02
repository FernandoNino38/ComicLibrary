package com.example.comiclibrary.ui.library

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.comiclibrary.R
import com.example.comiclibrary.data.model.ComicBook
import com.example.comiclibrary.theme.ComicCyan
import com.example.comiclibrary.theme.ComicInkBlack
import com.example.comiclibrary.theme.ComicPanelBorder
import com.example.comiclibrary.theme.ComicRed
import com.example.comiclibrary.theme.ComicRedDark
import com.example.comiclibrary.theme.ComicTitleFontFamily
import com.example.comiclibrary.theme.ComicYellow
import com.example.comiclibrary.ui.components.FocusBlock
import com.example.comiclibrary.ui.components.SquircleShape
import java.io.File
import java.util.Locale

/**
 * Comic Library Screen.
 * Internationalized (English / Portuguese based on system locale),
 * Pure AMOLED Black Theme (#000000), and Comic/Onomatopoeia Font (Bangers) for Title.
 */
@Composable
fun ComicLibraryScreen(
    comics: List<ComicBook>,
    isLoading: Boolean,
    importProgress: Pair<Int, Int>?,
    selectedComicId: String?,
    onComicClick: (ComicBook) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var isSearchExpanded by remember { mutableStateOf(false) }
    var selectedFilterKey by remember { mutableStateOf("all") }
    var comicForDetailDialog by remember { mutableStateOf<ComicBook?>(null) }
    var selectedSeries by remember { mutableStateOf<String?>(null) }
    var displayLimit by remember { mutableStateOf(30) }
    val isDark = isSystemInDarkTheme()

    val filterOptions = listOf(
        "all" to stringResource(R.string.filter_all),
        "favorites" to stringResource(R.string.filter_favorites),
        "reading" to stringResource(R.string.filter_reading),
        "completed" to stringResource(R.string.filter_completed),
        "manga" to stringResource(R.string.filter_manga)
    )

    val availableSeries = remember(comics) {
        comics.map { it.metadata.series }.filter { it.isNotBlank() }.distinct().sorted()
    }

    val filteredComics = comics.filter { comic ->
        val matchesQuery = searchQuery.isBlank() ||
                comic.metadata.displayTitle.contains(searchQuery, ignoreCase = true) ||
                comic.metadata.writer.contains(searchQuery, ignoreCase = true) ||
                comic.metadata.genre.contains(searchQuery, ignoreCase = true)

        val matchesFilter = when (selectedFilterKey) {
            "favorites" -> comic.isFavorite
            "reading" -> comic.lastReadPage > 0 && !comic.isFinished
            "completed" -> comic.isFinished
            "manga" -> comic.metadata.isManga
            else -> true
        }

        val matchesSeries = selectedSeries == null || comic.metadata.series.equals(selectedSeries, ignoreCase = true)

        matchesQuery && matchesFilter && matchesSeries
    }

    val pagedComics = filteredComics.take(displayLimit)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // TOP HEADER: Expandable Search Bar & Comic Title
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            AnimatedContent(
                targetState = isSearchExpanded,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "SearchMorphTransition"
            ) { expanded ->
                if (!expanded) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(R.drawable.ic_comic_library_logo),
                                contentDescription = null,
                                modifier = Modifier.size(46.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                // Authentic Comic/Onomatopoeia Font Title
                                Text(
                                    text = stringResource(R.string.nav_library).uppercase(),
                                    style = MaterialTheme.typography.displaySmall.copy(
                                        fontFamily = ComicTitleFontFamily,
                                        fontSize = 38.sp,
                                        letterSpacing = 2.sp
                                    ),
                                    color = if (isDark) ComicYellow else ComicInkBlack
                                )

                                Text(
                                    text = if (comics.isEmpty()) {
                                        stringResource(R.string.library_empty_count).uppercase()
                                    } else {
                                        stringResource(
                                            R.string.library_stats_format,
                                            comics.size,
                                            comics.count { it.lastReadPage > 0 }
                                        ).uppercase()
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = ComicTitleFontFamily,
                                        fontSize = 12.sp,
                                        letterSpacing = 0.8.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(
                            onClick = { isSearchExpanded = true },
                            modifier = Modifier
                                .clip(SquircleShape(cornerRadiusDp = 14.dp, smoothing = 0.5f))
                                .background(MaterialTheme.colorScheme.surfaceContainer)
                                .border(BorderStroke(1.5.dp, if (isDark) ComicYellow.copy(alpha = 0.6f) else ComicInkBlack.copy(alpha = 0.6f)), SquircleShape(cornerRadiusDp = 14.dp, smoothing = 0.5f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = stringResource(R.string.search_comics),
                                tint = if (isDark) ComicYellow else ComicInkBlack
                            )
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                stringResource(R.string.search_placeholder),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = if (isDark) ComicYellow else ComicInkBlack
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = {
                                searchQuery = ""
                                isSearchExpanded = false
                            }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = stringResource(R.string.close_search),
                                    tint = if (isDark) ComicYellow else ComicInkBlack
                                )
                            }
                        },
                        singleLine = true,
                        shape = SquircleShape(cornerRadiusDp = 16.dp, smoothing = 0.5f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            focusedBorderColor = if (isDark) ComicYellow else ComicInkBlack,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Background import progress indicator
            if (importProgress != null) {
                Spacer(modifier = Modifier.height(10.dp))
                val (current, total) = importProgress
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(SquircleShape(cornerRadiusDp = 12.dp, smoothing = 0.6f))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .padding(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.importing_progress_format, current, total),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { if (total > 0) current.toFloat() / total.toFloat() else 0f },
                        modifier = Modifier.fillMaxWidth().height(6.dp)
                    )
                }
            }
        }

        // INTERACTION AREA: Filters & Comic Grid
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 24.dp)
        ) {
            // Horizontally Scrollable Filter Chips
            if (comics.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filterOptions.forEach { (key, label) ->
                        val isSelected = selectedFilterKey == key
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedFilterKey = key },
                            label = {
                                Text(
                                    text = label.uppercase(),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontFamily = ComicTitleFontFamily,
                                        fontSize = 13.sp,
                                        letterSpacing = 1.sp
                                    )
                                )
                            },
                            shape = SquircleShape(cornerRadiusDp = 10.dp, smoothing = 0.5f),
                            border = BorderStroke(
                                1.5.dp,
                                if (isSelected) (if (isDark) ComicYellow else ComicInkBlack) else MaterialTheme.colorScheme.outlineVariant
                            ),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ComicYellow,
                                selectedLabelColor = ComicInkBlack,
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                labelColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            if (availableSeries.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedSeries == null,
                        onClick = { selectedSeries = null; displayLimit = 30 },
                        label = {
                            Text(
                                text = "TODAS AS SÉRIES",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = ComicTitleFontFamily,
                                    fontSize = 11.sp,
                                    letterSpacing = 1.sp
                                )
                            )
                        },
                        shape = SquircleShape(cornerRadiusDp = 10.dp, smoothing = 0.5f),
                        border = BorderStroke(1.5.dp, if (selectedSeries == null) (if (isDark) ComicYellow else ComicInkBlack) else MaterialTheme.colorScheme.outlineVariant),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ComicYellow,
                            selectedLabelColor = ComicInkBlack,
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            labelColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    availableSeries.forEach { series ->
                        val isSelected = selectedSeries == series
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedSeries = series; displayLimit = 30 },
                            label = {
                                Text(
                                    text = series.uppercase(),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontFamily = ComicTitleFontFamily,
                                        fontSize = 11.sp,
                                        letterSpacing = 1.sp
                                    )
                                )
                            },
                            shape = SquircleShape(cornerRadiusDp = 10.dp, smoothing = 0.5f),
                            border = BorderStroke(1.5.dp, if (isSelected) (if (isDark) ComicYellow else ComicInkBlack) else MaterialTheme.colorScheme.outlineVariant),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ComicYellow,
                                selectedLabelColor = ComicInkBlack,
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                labelColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            if (isLoading && comics.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = if (isDark) ComicYellow else ComicRedDark)
                }
            } else if (comics.isEmpty()) {
                // Empty state with navigation to Settings for importing
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    FocusBlock(
                        cornerRadius = 24.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp, horizontal = 16.dp)
                        ) {
                            Image(
                                painter = painterResource(R.drawable.ic_comic_library_logo),
                                contentDescription = null,
                                modifier = Modifier.size(80.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = stringResource(R.string.empty_library_title).uppercase(),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontFamily = ComicTitleFontFamily,
                                    fontSize = 24.sp,
                                    letterSpacing = 1.sp
                                ),
                                color = if (isDark) ComicYellow else ComicInkBlack
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = stringResource(R.string.empty_library_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = onNavigateToSettings,
                                shape = SquircleShape(cornerRadiusDp = 14.dp, smoothing = 0.5f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ComicYellow,
                                    contentColor = ComicInkBlack
                                )
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = null, tint = ComicInkBlack)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    stringResource(R.string.btn_go_to_settings).uppercase(),
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontFamily = ComicTitleFontFamily,
                                        fontSize = 15.sp,
                                        letterSpacing = 1.sp
                                    ),
                                    color = ComicInkBlack
                                )
                            }
                        }
                    }
                }
            } else if (filteredComics.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_comics_matched, searchQuery),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Adaptive Grid of Comic Covers
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(pagedComics, key = { it.id }) { comic ->
                        ComicFocusBlockItem(
                            comic = comic,
                            isSelected = comic.id == selectedComicId,
                            onClick = { onComicClick(comic) },
                            onLongClick = { comicForDetailDialog = comic },
                            onToggleFavorite = { onToggleFavorite(comic.id) }
                        )
                    }
                    if (filteredComics.size > displayLimit) {
                        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(this.maxLineSpan) }) {
                            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                                Button(
                                    onClick = { displayLimit += 30 },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                                ) {
                                    Text("CARREGAR MAIS (+30)", fontFamily = ComicTitleFontFamily, letterSpacing = 1.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Sheet / Dialog on Long Press with full metadata
    comicForDetailDialog?.let { comic ->
        ComicDetailModalDialog(
            comic = comic,
            onDismiss = { comicForDetailDialog = null },
            onReadClick = {
                comicForDetailDialog = null
                onComicClick(comic)
            },
            onToggleFavorite = { onToggleFavorite(comic.id) }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ComicFocusBlockItem(
    comic: ComicBook,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val isDark = isSystemInDarkTheme()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )
    ) {
        // Comic Cover Preview - Completely square, no rounded corners, no external border
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.72f)
                .clip(RectangleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            if (comic.coverPath != null && File(comic.coverPath).exists()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(File(comic.coverPath))
                        .crossfade(true)
                        .build(),
                    contentDescription = comic.metadata.displayTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = comic.metadata.displayTitle.take(30).uppercase(),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = ComicTitleFontFamily,
                            letterSpacing = 0.8.sp
                        ),
                        color = if (isDark) ComicYellow else ComicInkBlack,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Favorite icon button on top right of cover
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = if (comic.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = stringResource(R.string.favorite_cd),
                    tint = if (comic.isFavorite) ComicRed else Color.White.copy(alpha = 0.85f)
                )
            }

            // Manga badge if RTL - Comic Caption Box Style
            if (comic.metadata.isManga) {
                Surface(
                    color = ComicRed,
                    shape = RectangleShape,
                    border = BorderStroke(1.dp, ComicInkBlack),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.badge_manga).uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = ComicTitleFontFamily,
                            fontSize = 11.sp,
                            letterSpacing = 0.8.sp
                        ),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Reading Progress Bar in Comic Cyan / Comic Yellow
        if (comic.totalPages > 0) {
            LinearProgressIndicator(
                progress = { comic.progressPercent },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.5.dp)
                    .clip(RectangleShape),
                color = if (comic.isFinished) (if (isDark) ComicYellow else ComicRedDark) else ComicCyan,
                trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )
            Spacer(modifier = Modifier.height(6.dp))
        }

        // Title & Series in Bangers Comic Font
        Text(
            text = comic.metadata.displayTitle.uppercase(),
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = ComicTitleFontFamily,
                letterSpacing = 0.5.sp
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Subtitle in Comic Caption Style
        val subtitle = buildString {
            if (comic.metadata.year != null) append("${comic.metadata.year} • ")
            append(comic.totalPages.toString() + " " + stringResource(R.string.pages_count_format, comic.totalPages).replace(comic.totalPages.toString(), "").trim())
        }
        Text(
            text = subtitle.uppercase(),
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = ComicTitleFontFamily,
                fontSize = 11.sp,
                letterSpacing = 0.6.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Center Modal Dialog displaying all possible comic details upon long-press.
 * Styled as an authentic comic book dossier.
 */
@Composable
fun ComicDetailModalDialog(
    comic: ComicBook,
    onDismiss: () -> Unit,
    onReadClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val fileSizeFormatted = remember(comic.fileSizeBytes) {
        val mb = comic.fileSizeBytes / (1024.0 * 1024.0)
        String.format(Locale.getDefault(), "%.2f MB", mb)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = SquircleShape(cornerRadiusDp = 20.dp, smoothing = 0.5f),
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 8.dp,
            shadowElevation = 16.dp,
            border = BorderStroke(2.dp, if (isDark) ComicYellow else ComicInkBlack),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header: Cover + Title + Fast Stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .width(84.dp)
                            .aspectRatio(0.72f)
                            .clip(RectangleShape)
                            .border(BorderStroke(1.5.dp, if (isDark) ComicYellow.copy(alpha = 0.7f) else ComicInkBlack.copy(alpha = 0.7f)), RectangleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    ) {
                        if (comic.coverPath != null && File(comic.coverPath).exists()) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(File(comic.coverPath))
                                    .crossfade(true)
                                    .build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = if (isDark) ComicYellow else ComicInkBlack)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = comic.metadata.displayTitle.uppercase(),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontFamily = ComicTitleFontFamily,
                                    fontSize = 20.sp,
                                    letterSpacing = 0.8.sp
                                ),
                                color = if (isDark) ComicYellow else ComicInkBlack,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )

                            IconButton(
                                onClick = onToggleFavorite,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (comic.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = stringResource(R.string.favorite_cd),
                                    tint = if (comic.isFavorite) ComicRed else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (comic.metadata.series.isNotBlank()) {
                            Text(
                                text = stringResource(R.string.dialog_series, comic.metadata.series).uppercase(),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = ComicTitleFontFamily,
                                    letterSpacing = 0.5.sp
                                ),
                                color = ComicCyan
                            )
                        }

                        if (comic.metadata.publisher.isNotBlank()) {
                            Text(
                                text = stringResource(R.string.dialog_publisher, comic.metadata.publisher).uppercase(),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = ComicTitleFontFamily,
                                    letterSpacing = 0.5.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = stringResource(
                                R.string.dialog_stats_format,
                                comic.totalPages,
                                (comic.progressPercent * 100).toInt()
                            ).uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = ComicTitleFontFamily,
                                fontSize = 12.sp,
                                letterSpacing = 0.8.sp
                            ),
                            color = if (isDark) ComicYellow else ComicRedDark
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable Detailed Info
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DetailInfoRow(
                        label = stringResource(R.string.dialog_storage_location),
                        value = java.net.URLDecoder.decode(comic.uriString, "UTF-8"),
                        isMonospace = true
                    )

                    DetailInfoRow(
                        label = stringResource(R.string.dialog_filename),
                        value = comic.fileName
                    )

                    DetailInfoRow(
                        label = stringResource(R.string.dialog_file_size),
                        value = "$fileSizeFormatted (${comic.fileSizeBytes} bytes)"
                    )

                    DetailInfoRow(
                        label = stringResource(R.string.dialog_current_page),
                        value = stringResource(
                            R.string.dialog_page_progress_format,
                            comic.lastReadPage + 1,
                            comic.totalPages
                        )
                    )

                    DetailInfoRow(
                        label = stringResource(R.string.dialog_status),
                        value = when {
                            comic.isFinished -> stringResource(R.string.status_completed)
                            comic.lastReadPage > 0 -> stringResource(R.string.status_reading)
                            else -> stringResource(R.string.status_unread)
                        }
                    )

                    DetailInfoRow(
                        label = stringResource(R.string.dialog_reading_mode),
                        value = if (comic.metadata.isManga) {
                            stringResource(R.string.reading_mode_manga_desc)
                        } else {
                            stringResource(R.string.reading_mode_western_desc)
                        }
                    )

                    if (comic.metadata.writer.isNotBlank()) {
                        DetailInfoRow(label = stringResource(R.string.dialog_writer), value = comic.metadata.writer)
                    }

                    if (comic.metadata.penciller.isNotBlank()) {
                        DetailInfoRow(label = stringResource(R.string.dialog_penciller), value = comic.metadata.penciller)
                    }

                    if (comic.metadata.genre.isNotBlank()) {
                        DetailInfoRow(label = stringResource(R.string.dialog_genre), value = comic.metadata.genre)
                    }

                    if (comic.metadata.year != null) {
                        DetailInfoRow(label = stringResource(R.string.dialog_release_year), value = comic.metadata.year.toString())
                    }

                    if (comic.metadata.summary.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.dialog_synopsis).uppercase(),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = ComicTitleFontFamily,
                                letterSpacing = 0.8.sp
                            ),
                            color = if (isDark) ComicYellow else ComicInkBlack
                        )
                        Text(
                            text = comic.metadata.summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons: Read Now and Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = SquircleShape(cornerRadiusDp = 12.dp, smoothing = 0.5f),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Text(
                            stringResource(R.string.btn_close).uppercase(),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = ComicTitleFontFamily,
                                letterSpacing = 1.sp
                            )
                        )
                    }

                    Button(
                        onClick = onReadClick,
                        modifier = Modifier.weight(1.5f),
                        shape = SquircleShape(cornerRadiusDp = 12.dp, smoothing = 0.5f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ComicYellow,
                            contentColor = ComicInkBlack
                        )
                    ) {
                        Icon(Icons.Default.AutoStories, contentDescription = null, modifier = Modifier.size(18.dp), tint = ComicInkBlack)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            stringResource(R.string.btn_read_now).uppercase(),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontFamily = ComicTitleFontFamily,
                                fontSize = 15.sp,
                                letterSpacing = 1.sp
                            ),
                            color = ComicInkBlack
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailInfoRow(
    label: String,
    value: String,
    isMonospace: Boolean = false
) {
    val isDark = isSystemInDarkTheme()
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = ComicTitleFontFamily,
                fontSize = 12.sp,
                letterSpacing = 0.8.sp
            ),
            color = if (isDark) ComicYellow else ComicInkBlack
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
