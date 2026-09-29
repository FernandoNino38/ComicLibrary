package com.example.comiclibrary.ui.library

import androidx.compose.foundation.background
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
                // Cover Thumbnail with squircle shape
                Box(
                    modifier = Modifier
                        .width(130.dp)
                        .aspectRatio(0.72f)
                        .clip(RectangleShape)
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
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = comic.metadata.displayTitle.take(30),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(18.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = comic.metadata.displayTitle,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (comic.metadata.series.isNotBlank() && comic.metadata.series != comic.metadata.title) {
                        Text(
                            text = "Série: ${comic.metadata.series}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (comic.metadata.genre.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        SuggestionChip(
                            onClick = {},
                            label = { Text(comic.metadata.genre, fontSize = 11.sp) },
                            shape = SquircleShape(cornerRadiusDp = 10.dp, smoothing = 0.6f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "${comic.totalPages} páginas  •  ${if (comic.metadata.isManga) "Mangá (RTL)" else "Ocidental (LTR)"}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
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
                shape = SquircleShape(cornerRadiusDp = 18.dp, smoothing = 0.6f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                val actionText = if (comic.lastReadPage > 0) {
                    "Continuar (Pág. ${comic.lastReadPage + 1})"
                } else {
                    "Iniciar Leitura"
                }
                Text(actionText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Favorite button
            IconButton(
                onClick = { onToggleFavorite(comic.id) },
                modifier = Modifier
                    .size(54.dp)
                    .clip(SquircleShape(cornerRadiusDp = 18.dp, smoothing = 0.6f))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = if (comic.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorito",
                    tint = if (comic.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Delete button
            IconButton(
                onClick = { onDeleteComic(comic.id) },
                modifier = Modifier
                    .size(54.dp)
                    .clip(SquircleShape(cornerRadiusDp = 18.dp, smoothing = 0.6f))
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
                text = "Sinopse",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
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
                text = "Ficha Técnica",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            FocusBlock(
                cornerRadius = 18.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (comic.metadata.writer.isNotBlank()) {
                        CreditRow(label = "Roteirista", value = comic.metadata.writer)
                    }
                    if (comic.metadata.penciller.isNotBlank()) {
                        CreditRow(label = "Desenhista", value = comic.metadata.penciller)
                    }
                    if (comic.metadata.inker.isNotBlank()) {
                        CreditRow(label = "Arte-finalista", value = comic.metadata.inker)
                    }
                    if (comic.metadata.publisher.isNotBlank()) {
                        CreditRow(label = "Editora", value = comic.metadata.publisher)
                    }
                    CreditRow(label = "Arquivo", value = comic.fileName)
                }
            }
        }
    }
}

@Composable
private fun CreditRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
