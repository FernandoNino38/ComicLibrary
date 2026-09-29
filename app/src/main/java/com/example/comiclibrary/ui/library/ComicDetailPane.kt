package com.example.comiclibrary.ui.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.comiclibrary.data.model.ComicBook
import com.example.comiclibrary.theme.ComicCyan
import com.example.comiclibrary.theme.ComicInkBlack
import com.example.comiclibrary.theme.ComicRed
import com.example.comiclibrary.theme.ComicRedDark
import com.example.comiclibrary.theme.ComicTitleFontFamily
import com.example.comiclibrary.theme.ComicYellow
import com.example.comiclibrary.ui.components.FocusBlock
import com.example.comiclibrary.ui.components.SquircleShape
import java.io.File

/**
 * Comic Detail Pane adhering to Material 3 Canonical Layouts (ListDetailPaneScaffold).
 * Formats ComicInfo.xml metadata into Viewing Area (synopsis, cover) and Interaction Area (actions, credits).
 */
@Composable
fun ComicDetailPane(
    comic: ComicBook?,
    onReadClick: (ComicBook) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onDeleteComic: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (comic == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Selecione um quadrinho na estante para visualizar os detalhes",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val isDark = isSystemInDarkTheme()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(24.dp)
    ) {
        // VIEWING AREA: Cover Focus Block & Title Header
        FocusBlock(
            cornerRadius = 24.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cover Thumbnail with comic border
                Box(
                    modifier = Modifier
                        .width(130.dp)
                        .aspectRatio(0.72f)
                        .clip(RectangleShape)
                        .border(BorderStroke(2.dp, if (isDark) ComicYellow.copy(alpha = 0.8f) else ComicInkBlack.copy(alpha = 0.8f)), RectangleShape)
                        .background(MaterialTheme.colorScheme.surface)
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
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = ComicTitleFontFamily,
                                    letterSpacing = 0.8.sp
                                ),
                                color = if (isDark) ComicYellow else ComicInkBlack,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(18.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = comic.metadata.displayTitle.uppercase(),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontFamily = ComicTitleFontFamily,
                            fontSize = 24.sp,
                            letterSpacing = 1.sp
                        ),
                        color = if (isDark) ComicYellow else ComicInkBlack
                    )

                    if (comic.metadata.series.isNotBlank() && comic.metadata.series != comic.metadata.title) {
                        Text(
                            text = "Série: ${comic.metadata.series}".uppercase(),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = ComicTitleFontFamily,
                                letterSpacing = 0.5.sp
                            ),
                            color = ComicCyan
                        )
                    }

                    if (comic.metadata.genre.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        SuggestionChip(
                            onClick = {},
                            label = { Text(comic.metadata.genre.uppercase(), fontSize = 11.sp) },
                            shape = SquircleShape(cornerRadiusDp = 10.dp, smoothing = 0.5f),
                            border = BorderStroke(1.dp, if (isDark) ComicYellow.copy(alpha = 0.5f) else ComicInkBlack.copy(alpha = 0.5f))
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "${comic.totalPages} PÁGINAS  •  ${if (comic.metadata.isManga) "MANGÁ (RTL)" else "OCIDENTAL (LTR)"}",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = ComicTitleFontFamily,
                            letterSpacing = 0.8.sp
                        ),
                        color = if (isDark) ComicYellow else ComicRedDark
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // INTERACTION AREA: Primary Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { onReadClick(comic) },
                shape = SquircleShape(cornerRadiusDp = 14.dp, smoothing = 0.5f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ComicYellow,
                    contentColor = ComicInkBlack
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = ComicInkBlack)
                Spacer(modifier = Modifier.width(8.dp))
                val actionText = if (comic.lastReadPage > 0) {
                    "Continuar (Pág. ${comic.lastReadPage + 1})"
                } else {
                    "Iniciar Leitura"
                }
                Text(
                    actionText.uppercase(),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = ComicTitleFontFamily,
                        fontSize = 16.sp,
                        letterSpacing = 1.sp
                    ),
                    color = ComicInkBlack
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Favorite button
            IconButton(
                onClick = { onToggleFavorite(comic.id) },
                modifier = Modifier
                    .size(54.dp)
                    .clip(SquircleShape(cornerRadiusDp = 14.dp, smoothing = 0.5f))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = if (comic.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorito",
                    tint = if (comic.isFavorite) ComicRed else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Delete button
            IconButton(
                onClick = { onDeleteComic(comic.id) },
                modifier = Modifier
                    .size(54.dp)
                    .clip(SquircleShape(cornerRadiusDp = 14.dp, smoothing = 0.5f))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Remover",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // VIEWING AREA: Synopsis / Summary Focus Block
        if (comic.metadata.summary.isNotBlank()) {
            Text(
                text = "SINOPSE",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = ComicTitleFontFamily,
                    letterSpacing = 1.sp
                ),
                color = if (isDark) ComicYellow else ComicInkBlack
            )

            Spacer(modifier = Modifier.height(8.dp))

            FocusBlock(
                cornerRadius = 18.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = comic.metadata.summary,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // VIEWING AREA: Creative Credits (ComicInfo.xml)
        val credits = comic.metadata.creatorsSummary
        if (credits.isNotBlank()) {
            Text(
                text = "FICHA TÉCNICA",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = ComicTitleFontFamily,
                    letterSpacing = 1.sp
                ),
                color = if (isDark) ComicYellow else ComicInkBlack
            )

            Spacer(modifier = Modifier.height(8.dp))

            FocusBlock(
                cornerRadius = 18.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (comic.metadata.writer.isNotBlank()) {
                        CreditRow(label = "ROTEIRISTA", value = comic.metadata.writer)
                    }
                    if (comic.metadata.penciller.isNotBlank()) {
                        CreditRow(label = "DESENHISTA", value = comic.metadata.penciller)
                    }
                    if (comic.metadata.inker.isNotBlank()) {
                        CreditRow(label = "ARTE-FINALISTA", value = comic.metadata.inker)
                    }
                    if (comic.metadata.publisher.isNotBlank()) {
                        CreditRow(label = "EDITORA", value = comic.metadata.publisher)
                    }
                    CreditRow(label = "ARQUIVO", value = comic.fileName)
                }
            }
        }
    }
}

@Composable
private fun CreditRow(label: String, value: String) {
    val isDark = isSystemInDarkTheme()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = ComicTitleFontFamily,
                letterSpacing = 0.6.sp
            ),
            color = if (isDark) ComicYellow else ComicInkBlack
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
