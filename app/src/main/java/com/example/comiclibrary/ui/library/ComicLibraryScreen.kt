package com.example.comiclibrary.ui.library

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
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
import com.example.comiclibrary.data.model.ComicBook
import com.example.comiclibrary.ui.components.FocusBlock
import com.example.comiclibrary.ui.components.SquircleShape
import java.io.File
import java.util.Locale

/**
 * Comic Library Screen (v0.3).
 * Refinements:
 * - Top header features an expandable search icon (magnifying glass) that smoothly morphs into an input bar.
 * - Imports (file and folder) moved to Settings (Ajustes).
 * - Single tap on comic card opens the reader directly.
 * - Long press on comic card opens a comprehensive metadata popup dialog in the center.
 * - Filter chips (Todos, Favoritos, Lendo, Concluídos, Mangá) are horizontally scrollable.
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
    var selectedFilter by remember { mutableStateOf("Todos") }
    var comicForDetailDialog by remember { mutableStateOf<ComicBook?>(null) }

    val filteredComics = comics.filter { comic ->
        val matchesQuery = searchQuery.isBlank() ||
                comic.metadata.displayTitle.contains(searchQuery, ignoreCase = true) ||
                comic.metadata.writer.contains(searchQuery, ignoreCase = true) ||
                comic.metadata.genre.contains(searchQuery, ignoreCase = true)

        val matchesFilter = when (selectedFilter) {
            "Favoritos" -> comic.isFavorite
            "Lendo" -> comic.lastReadPage > 0 && !comic.isFinished
            "Concluídos" -> comic.isFinished
            "Mangá" -> comic.metadata.isManga
            else -> true
        }

        matchesQuery && matchesFilter
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // TOP HEADER: Expandable Search Bar & Title
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
                        Column {
                            Text(
                                text = "Biblioteca",
                                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = if (comics.isEmpty()) {
                                    "Nenhum quadrinho na estante"
                                } else {
                                    "${comics.size} volumes  •  ${comics.count { it.lastReadPage > 0 }} em leitura"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = { isSearchExpanded = true },
                            modifier = Modifier
                                .clip(SquircleShape(cornerRadiusDp = 14.dp, smoothing = 0.6f))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Buscar Quadrinhos",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Buscar título, autor ou gênero...") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = {
                                searchQuery = ""
                                isSearchExpanded = false
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Fechar Busca")
                            }
                        },
                        singleLine = true,
                        shape = SquircleShape(cornerRadiusDp = 18.dp, smoothing = 0.6f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Transparent
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
                        text = "Importando ($current de $total)...",
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
                    listOf("Todos", "Favoritos", "Lendo", "Concluídos", "Mangá").forEach { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter, fontSize = 13.sp) },
                            shape = SquircleShape(cornerRadiusDp = 12.dp, smoothing = 0.6f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (isLoading && comics.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
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
                            Icon(
                                imageVector = Icons.Default.AutoStories,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(64.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Sua biblioteca está vazia",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Para adicionar quadrinhos .CBZ ou diretórios inteiros, acesse a aba Ajustes.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = onNavigateToSettings,
                                shape = SquircleShape(cornerRadiusDp = 16.dp, smoothing = 0.6f)
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Acessar Ajustes para Importar")
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
                        text = "Nenhum quadrinho corresponde à pesquisa '$searchQuery'.",
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
                    items(filteredComics, key = { it.id }) { comic ->
                        ComicFocusBlockItem(
                            comic = comic,
                            isSelected = comic.id == selectedComicId,
                            onClick = { onComicClick(comic) },
                            onLongClick = { comicForDetailDialog = comic },
                            onToggleFavorite = { onToggleFavorite(comic.id) }
                        )
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
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = comic.metadata.displayTitle.take(30),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
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
                    contentDescription = "Favorito",
                    tint = if (comic.isFavorite) Color.Red else Color.White.copy(alpha = 0.8f)
                )
            }

            // Manga badge if RTL
            if (comic.metadata.isManga) {
                Surface(
                    color = Color.Black.copy(alpha = 0.85f),
                    shape = RectangleShape,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                ) {
                    Text(
                        text = "MANGÁ",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFFFF4081),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Reading Progress Bar
        if (comic.totalPages > 0) {
            LinearProgressIndicator(
                progress = { comic.progressPercent },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RectangleShape),
                color = if (comic.isFinished) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )
            Spacer(modifier = Modifier.height(6.dp))
        }

        // Title & Series
        Text(
            text = comic.metadata.displayTitle,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Subtitle
        val subtitle = buildString {
            if (comic.metadata.year != null) append("${comic.metadata.year} • ")
            append("${comic.totalPages} páginas")
        }
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Center Modal Dialog displaying all possible comic details upon long-press.
 */
@Composable
fun ComicDetailModalDialog(
    comic: ComicBook,
    onDismiss: () -> Unit,
    onReadClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val context = LocalContext.current
    val fileSizeFormatted = remember(comic.fileSizeBytes) {
        val mb = comic.fileSizeBytes / (1024.0 * 1024.0)
        String.format(Locale.getDefault(), "%.2f MB", mb)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = SquircleShape(cornerRadiusDp = 24.dp, smoothing = 0.6f),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            shadowElevation = 16.dp,
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
                            .clip(SquircleShape(cornerRadiusDp = 10.dp, smoothing = 0.6f))
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
                                Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
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
                                text = comic.metadata.displayTitle,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
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
                                    contentDescription = "Favoritar",
                                    tint = if (comic.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (comic.metadata.series.isNotBlank()) {
                            Text(
                                text = "Série: ${comic.metadata.series}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (comic.metadata.publisher.isNotBlank()) {
                            Text(
                                text = "Editora: ${comic.metadata.publisher}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = "${comic.totalPages} páginas  •  ${(comic.progressPercent * 100).toInt()}% lido",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.secondary
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
                    // Internal Storage Path / Location
                    DetailInfoRow(
                        label = "Armazenamento Interno",
                        value = comic.uriString,
                        isMonospace = true
                    )

                    DetailInfoRow(
                        label = "Nome do Arquivo",
                        value = comic.fileName
                    )

                    DetailInfoRow(
                        label = "Tamanho do Arquivo",
                        value = "$fileSizeFormatted (${comic.fileSizeBytes} bytes)"
                    )

                    DetailInfoRow(
                        label = "Página Atual",
                        value = "${comic.lastReadPage + 1} de ${comic.totalPages}"
                    )

                    DetailInfoRow(
                        label = "Status",
                        value = when {
                            comic.isFinished -> "Concluído"
                            comic.lastReadPage > 0 -> "Em Leitura"
                            else -> "Não Lido"
                        }
                    )

                    DetailInfoRow(
                        label = "Modo de Leitura",
                        value = if (comic.metadata.isManga) "Mangá (Direita para Esquerda - RTL)" else "Ocidental (Esquerda para Direita - LTR)"
                    )

                    if (comic.metadata.writer.isNotBlank()) {
                        DetailInfoRow(label = "Roteirista", value = comic.metadata.writer)
                    }

                    if (comic.metadata.penciller.isNotBlank()) {
                        DetailInfoRow(label = "Desenhista", value = comic.metadata.penciller)
                    }

                    if (comic.metadata.genre.isNotBlank()) {
                        DetailInfoRow(label = "Gênero", value = comic.metadata.genre)
                    }

                    if (comic.metadata.year != null) {
                        DetailInfoRow(label = "Ano de Lançamento", value = comic.metadata.year.toString())
                    }

                    if (comic.metadata.summary.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Sinopse",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = comic.metadata.summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons: Ler Agora e Fechar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = SquircleShape(cornerRadiusDp = 14.dp, smoothing = 0.6f)
                    ) {
                        Text("Fechar")
                    }

                    Button(
                        onClick = onReadClick,
                        modifier = Modifier.weight(1.5f),
                        shape = SquircleShape(cornerRadiusDp = 14.dp, smoothing = 0.6f)
                    ) {
                        Icon(Icons.Default.AutoStories, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ler Agora")
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
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
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
