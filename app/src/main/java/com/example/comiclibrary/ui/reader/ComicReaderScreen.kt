package com.example.comiclibrary.ui.reader

import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.comiclibrary.data.model.ComicBook
import com.example.comiclibrary.ui.components.FocusBlock
import com.example.comiclibrary.ui.components.PageScrubber
import com.example.comiclibrary.ui.components.TonalFloatingBar
import com.example.comiclibrary.ui.reader.mvi.ReaderError
import com.example.comiclibrary.ui.reader.mvi.ReaderIntent
import com.example.comiclibrary.ui.reader.mvi.ReaderState
import com.example.comiclibrary.ui.theme.MotionTokens
import kotlin.math.abs
import kotlinx.coroutines.launch

/**
 * Native High-Performance CBZ Reader Screen (v0.3).
 * Refinements:
 * - 3D Page-curl leaf animation synchronously tracking user's finger during drag.
 * - Docked edge-to-edge full-width bottom bar (no floating pill).
 * - MVI Architecture, Material Design 3 Tonal Elevation, Predictive Back.
 */
@Composable
fun ComicReaderScreen(
    comic: ComicBook,
    onBack: () -> Unit,
    onProgressUpdate: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReaderViewModel = viewModel()
) {
    val scope = rememberCoroutineScope()
    val state by viewModel.state.collectAsState()
    val cachedBitmaps by viewModel.cachedBitmaps.collectAsState()

    // Trigger comic load on initial composition or comic change
    LaunchedEffect(comic.id) {
        viewModel.processIntent(ReaderIntent.LoadComic(comic))
    }

    // Predictive back progress tracking (Android 14+)
    var backProgress by remember { mutableFloatStateOf(0f) }

    BackHandler(onBack = onBack)

    PredictiveBackHandler { backEventStream ->
        try {
            backEventStream.collect { event ->
                backProgress = event.progress
            }
            onBack()
        } catch (_: Exception) {
            backProgress = 0f
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .graphicsLayer {
                val scale = 1f - (backProgress * 0.12f)
                scaleX = scale
                scaleY = scale
                alpha = 1f - (backProgress * 0.4f)
            }
    ) {
        when (val currentState = state) {
            is ReaderState.Loading -> {
                ReaderLoadingView(
                    progress = currentState.progress,
                    message = currentState.message,
                    onBack = onBack
                )
            }
            is ReaderState.Error -> {
                ReaderErrorView(
                    error = currentState.errorType,
                    onRetry = { viewModel.processIntent(ReaderIntent.Retry) },
                    onBack = onBack
                )
            }
            is ReaderState.Ready -> {
                ReaderReadyView(
                    state = currentState,
                    cachedBitmaps = cachedBitmaps,
                    onIntent = viewModel::processIntent,
                    onBack = onBack,
                    onProgressUpdate = onProgressUpdate
                )
            }
        }
    }
}

@Composable
private fun ReaderLoadingView(
    progress: Float,
    message: String,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        FocusBlock(
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth(0.85f),
            elevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 4.dp
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = message,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(onClick = onBack) {
                    Text("Cancelar")
                }
            }
        }
    }
}

@Composable
private fun ReaderErrorView(
    error: ReaderError,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    val errorDescription = when (error) {
        is ReaderError.MissingPermission ->
            "Permissão negada. O aplicativo necessita de acesso ao arquivo para realizar a leitura."
        is ReaderError.CorruptArchive ->
            "Arquivo corrompido ou formato inválido. Certifique-se de que é um CBZ/ZIP contendo imagens."
        is ReaderError.OutOfMemory ->
            "Memória insuficiente para decodificar esta imagem em alta definição."
        is ReaderError.Unknown ->
            error.message.ifBlank { "Erro inesperado ao processar o quadrinho." }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        FocusBlock(
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth(0.9f),
            elevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = "Erro",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(56.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Falha ao Carregar Leitura",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = errorDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
                ) {
                    OutlinedButton(onClick = onBack) {
                        Text("Voltar")
                    }

                    Button(onClick = onRetry) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Tentar Novamente")
                    }
                }
            }
        }
    }
}

@Composable
private fun ReaderReadyView(
    state: ReaderState.Ready,
    cachedBitmaps: Map<Int, android.graphics.Bitmap>,
    onIntent: (ReaderIntent) -> Unit,
    onBack: () -> Unit,
    onProgressUpdate: (Int) -> Unit
) {
    val scope = rememberCoroutineScope()
    val totalPages = state.totalPages
    val pagerState = rememberPagerState(
        initialPage = state.currentPage.coerceIn(0, (totalPages - 1).coerceAtLeast(0)),
        pageCount = { totalPages }
    )

    // Notify repository on page change
    LaunchedEffect(pagerState.currentPage) {
        onIntent(ReaderIntent.ChangePage(pagerState.currentPage))
        onProgressUpdate(pagerState.currentPage)
    }

    val readingDirection = if (state.isManga) LayoutDirection.Rtl else LayoutDirection.Ltr

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Paged Comic Content with 3D Leaf/Page-Curl animation following finger gesture
        CompositionLocalProvider(LocalLayoutDirection provides readingDirection) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1
            ) { pageIndex ->
                val bitmap = cachedBitmaps[pageIndex]
                val pageOffset = ((pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction)
                val absOffset = abs(pageOffset).coerceIn(0f, 1f)
                val isManga = state.isManga

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            cameraDistance = 18f * density
                            if (pageOffset < -1.1f || pageOffset > 1.1f) {
                                alpha = 0f
                            } else {
                                alpha = 1f
                                if (!isManga) {
                                    // LTR (Western): Spine is pinned on the Left (0f, 0.5f)
                                    transformOrigin = TransformOrigin(if (pageOffset < 0f) 0f else 1f, 0.5f)
                                    rotationY = (pageOffset * 50f).coerceIn(-75f, 75f)
                                } else {
                                    // RTL (Manga): Spine is pinned on the Right (1f, 0.5f)
                                    transformOrigin = TransformOrigin(if (pageOffset > 0f) 1f else 0f, 0.5f)
                                    rotationY = (-pageOffset * 50f).coerceIn(-75f, 75f)
                                }
                            }
                        }
                ) {
                    ZoomableTiledPageView(
                        bitmap = bitmap,
                        isLoading = bitmap == null,
                        pageIndex = pageIndex,
                        onToggleChrome = { onIntent(ReaderIntent.ToggleChrome) }
                    )

                    // Dynamic paper leaf shadow that responds to finger dragging
                    if (absOffset > 0.01f) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = (absOffset * 0.45f).coerceIn(0f, 0.6f)))
                        )
                    }
                }
            }
        }

        // 2. Fading Chrome: Top Tonal Bar with Harmonic Spring Animation
        AnimatedVisibility(
            visible = state.isChromeVisible,
            enter = fadeIn(MotionTokens.MicroInteraction) + slideInVertically(MotionTokens.PanelOffsetTransition) { -it },
            exit = fadeOut(MotionTokens.MicroInteraction) + slideOutVertically(MotionTokens.PanelOffsetTransition) { -it },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            TonalFloatingBar(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Voltar",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column {
                            Text(
                                text = state.comic.metadata.displayTitle,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (state.isManga) "Modo Mangá (Direita para Esquerda)" else "Modo Quadrinho (Esquerda para Direita)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Reading Direction Toggle Chip
                    SuggestionChip(
                        onClick = { onIntent(ReaderIntent.ToggleReadingDirection) },
                        label = { Text(if (state.isManga) "RTL" else "LTR", fontSize = 12.sp) },
                        icon = {
                            Icon(
                                Icons.Default.SwapHoriz,
                                contentDescription = "Alternar Orientação de Leitura",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ),
                        border = null
                    )
                }
            }
        }

        // 3. Fading Chrome: Bottom Edge-to-Edge Docked Bar (Not a pill, covers entire bottom)
        AnimatedVisibility(
            visible = state.isChromeVisible,
            enter = fadeIn(MotionTokens.MicroInteraction) + slideInVertically(MotionTokens.PanelOffsetTransition) { it },
            exit = fadeOut(MotionTokens.MicroInteraction) + slideOutVertically(MotionTokens.PanelOffsetTransition) { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f),
                tonalElevation = 8.dp,
                shadowElevation = 16.dp,
                shape = RectangleShape
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    PageScrubber(
                        currentPage = pagerState.currentPage,
                        totalPages = totalPages,
                        onPageSelected = { targetPage ->
                            scope.launch { pagerState.scrollToPage(targetPage) }
                        }
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            scope.launch { pagerState.animateScrollToPage(0) }
                        }) {
                            Icon(
                                Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = "Primeira Página",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = "${pagerState.currentPage + 1} de $totalPages",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        IconButton(onClick = {
                            scope.launch { pagerState.animateScrollToPage(totalPages - 1) }
                        }) {
                            Icon(
                                Icons.Default.AutoStories,
                                contentDescription = "Última Página",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
