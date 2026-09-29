package com.example.comiclibrary.ui.reader

import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.comiclibrary.R
import com.example.comiclibrary.data.model.ComicBook
import com.example.comiclibrary.theme.ComicCyan
import com.example.comiclibrary.theme.ComicPanelBorder
import com.example.comiclibrary.theme.ComicTitleFontFamily
import com.example.comiclibrary.theme.ComicYellow
import com.example.comiclibrary.ui.components.FocusBlock
import com.example.comiclibrary.ui.components.PageScrubber
import com.example.comiclibrary.ui.components.SquircleShape
import com.example.comiclibrary.ui.components.TonalFloatingBar
import com.example.comiclibrary.ui.reader.mvi.ReaderError
import com.example.comiclibrary.ui.reader.mvi.ReaderIntent
import com.example.comiclibrary.ui.reader.mvi.ReaderState
import com.example.comiclibrary.ui.theme.MotionTokens
import java.io.File
import kotlin.math.abs
import kotlinx.coroutines.launch

/**
 * Native High-Performance CBZ Reader Screen (v0.6).
 * Refinements:
 * - On-screen Zoom option controls: Zoom in, Zoom out, percentage pill toggle, fit/reset.
 * - Synchronized gesture zoom: double-tap, pinch-to-zoom, and on-screen buttons.
 * - Direct cover display during initialization for instant visual continuity.
 * - 3-zone touch controls: Tap Right (turn forward), Tap Left (turn backward), Tap Center (toggle UI).
 * - Smooth 3D paper leaf-curl animation running purely in draw/graphicsLayer phase.
 * - Full-width docked bottom bar (edge-to-edge).
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
                    comic = comic,
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
    comic: ComicBook,
    progress: Float,
    message: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // Display actual comic cover from the start so opening animation has visual substance
        if (comic.coverPath != null && File(comic.coverPath).exists()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(File(comic.coverPath))
                    .crossfade(true)
                    .build(),
                contentDescription = comic.metadata.displayTitle,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Subtle bottom progress bar
        LinearProgressIndicator(
            progress = { if (progress > 0f) progress else 0.35f },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(3.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = Color.Transparent
        )
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

    // Current page zoom level state
    var currentZoomScale by remember { mutableFloatStateOf(1f) }

    // Reset zoom and notify repository on page change
    LaunchedEffect(pagerState.currentPage) {
        currentZoomScale = 1f
        onIntent(ReaderIntent.ChangePage(pagerState.currentPage))
        onProgressUpdate(pagerState.currentPage)
    }

    val readingDirection = if (state.isManga) LayoutDirection.Rtl else LayoutDirection.Ltr
    val isManga = state.isManga

    // Smooth page flip animations on tap
    val onTapLeft: () -> Unit = {
        if (!isManga) {
            // Western (LTR): Tap left turns to PREVIOUS page with smooth leaf-curl animation
            if (pagerState.currentPage > 0) {
                scope.launch {
                    pagerState.animateScrollToPage(
                        page = pagerState.currentPage - 1,
                        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing)
                    )
                }
            }
        } else {
            // Manga (RTL): Tap left turns to NEXT page with smooth leaf-curl animation
            if (pagerState.currentPage < totalPages - 1) {
                scope.launch {
                    pagerState.animateScrollToPage(
                        page = pagerState.currentPage + 1,
                        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing)
                    )
                }
            }
        }
    }

    val onTapRight: () -> Unit = {
        if (!isManga) {
            // Western (LTR): Tap right turns to NEXT page with smooth leaf-curl animation
            if (pagerState.currentPage < totalPages - 1) {
                scope.launch {
                    pagerState.animateScrollToPage(
                        page = pagerState.currentPage + 1,
                        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing)
                    )
                }
            }
        } else {
            // Manga (RTL): Tap right turns to PREVIOUS page with smooth leaf-curl animation
            if (pagerState.currentPage > 0) {
                scope.launch {
                    pagerState.animateScrollToPage(
                        page = pagerState.currentPage - 1,
                        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing)
                    )
                }
            }
        }
    }

    val onTapCenter: () -> Unit = {
        onIntent(ReaderIntent.ToggleChrome)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Paged Comic Content with 3D Leaf/Page-Curl animation following finger gestures and taps
        CompositionLocalProvider(LocalLayoutDirection provides readingDirection) {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = (currentZoomScale <= 1.05f),
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds(),
                beyondViewportPageCount = 1
            ) { pageIndex ->
                val isCurrentPage = (pageIndex == pagerState.currentPage)
                val bitmap = cachedBitmaps[pageIndex]

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clipToBounds()
                        .graphicsLayer {
                            val pageOffset = ((pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction)
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
                        zoomScale = if (isCurrentPage) currentZoomScale else 1f,
                        onZoomScaleChange = { newScale ->
                            if (isCurrentPage) {
                                currentZoomScale = newScale
                            }
                        },
                        onTapLeft = onTapLeft,
                        onTapCenter = onTapCenter,
                        onTapRight = onTapRight
                    )

                    // Dynamic paper leaf shadow that animates purely in graphicsLayer draw phase
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                val pageOffset = ((pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction)
                                val absOffset = abs(pageOffset).coerceIn(0f, 1f)
                                alpha = (absOffset * 0.45f).coerceIn(0f, 0.6f)
                            }
                            .background(Color.Black)
                    )
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
                                contentDescription = stringResource(R.string.reader_back),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column {
                            Text(
                                text = state.comic.metadata.displayTitle.uppercase(),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = ComicTitleFontFamily,
                                    fontSize = 17.sp,
                                    letterSpacing = 0.8.sp
                                ),
                                color = ComicYellow,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (state.isManga) {
                                    stringResource(R.string.reader_mode_manga_title).uppercase()
                                } else {
                                    stringResource(R.string.reader_mode_western_title).uppercase()
                                },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = ComicTitleFontFamily,
                                    fontSize = 11.sp,
                                    letterSpacing = 0.5.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Quick Zoom Option Chip in Top Bar
                        SuggestionChip(
                            onClick = {
                                currentZoomScale = if (currentZoomScale > 1.05f) 1f else 2f
                            },
                            label = {
                                Text(
                                    "${(currentZoomScale * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = ComicTitleFontFamily,
                                        fontSize = 12.sp,
                                        letterSpacing = 0.5.sp
                                    )
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = if (currentZoomScale > 1.05f) Icons.Default.ZoomOut else Icons.Default.ZoomIn,
                                    contentDescription = stringResource(R.string.reader_zoom_label),
                                    tint = ComicYellow,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            ),
                            border = BorderStroke(1.dp, ComicYellow.copy(alpha = 0.5f))
                        )

                        // Reading Direction Toggle Chip
                        SuggestionChip(
                            onClick = { onIntent(ReaderIntent.ToggleReadingDirection) },
                            label = {
                                Text(
                                    if (state.isManga) "RTL" else "LTR",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = ComicTitleFontFamily,
                                        fontSize = 12.sp,
                                        letterSpacing = 0.5.sp
                                    )
                                )
                            },
                            icon = {
                                Icon(
                                    Icons.Default.SwapHoriz,
                                    contentDescription = stringResource(R.string.reader_toggle_reading_direction),
                                    tint = ComicYellow,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            ),
                            border = BorderStroke(1.dp, ComicYellow.copy(alpha = 0.5f))
                        )
                    }
                }
            }
        }

        // 3. Fading Chrome: Bottom Edge-to-Edge Docked Bar with Dedicated On-Screen Zoom Controls
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
                shape = RectangleShape,
                border = BorderStroke(1.dp, ComicPanelBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // On-Screen Zoom Controls Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { currentZoomScale = (currentZoomScale - 0.5f).coerceAtLeast(1f) },
                            enabled = currentZoomScale > 1.05f
                        ) {
                            Icon(
                                Icons.Default.ZoomOut,
                                contentDescription = stringResource(R.string.reader_zoom_out),
                                tint = if (currentZoomScale > 1.05f) ComicYellow else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            )
                        }

                        Surface(
                            onClick = { currentZoomScale = if (currentZoomScale > 1.05f) 1f else 2f },
                            shape = SquircleShape(cornerRadiusDp = 10.dp, smoothing = 0.5f),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            border = BorderStroke(1.5.dp, ComicYellow.copy(alpha = 0.7f)),
                            modifier = Modifier.padding(horizontal = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ZoomIn,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                    tint = ComicYellow
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${(currentZoomScale * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontFamily = ComicTitleFontFamily,
                                        fontSize = 14.sp,
                                        letterSpacing = 0.8.sp
                                    ),
                                    color = ComicYellow
                                )
                            }
                        }

                        IconButton(
                            onClick = { currentZoomScale = (currentZoomScale + 0.5f).coerceAtMost(4f) },
                            enabled = currentZoomScale < 4f
                        ) {
                            Icon(
                                Icons.Default.ZoomIn,
                                contentDescription = stringResource(R.string.reader_zoom_in),
                                tint = if (currentZoomScale < 4f) ComicYellow else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            )
                        }

                        if (currentZoomScale > 1.05f) {
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedButton(
                                onClick = { currentZoomScale = 1f },
                                shape = SquircleShape(cornerRadiusDp = 10.dp, smoothing = 0.5f),
                                border = BorderStroke(1.dp, ComicYellow),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    Icons.Default.FitScreen,
                                    contentDescription = null,
                                    tint = ComicYellow,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    stringResource(R.string.reader_zoom_fit).uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = ComicTitleFontFamily,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = ComicYellow
                                )
                            }
                        }
                    }

                    PageScrubber(
                        currentPage = pagerState.currentPage,
                        totalPages = totalPages,
                        onPageSelected = { targetPage ->
                            scope.launch {
                                pagerState.animateScrollToPage(
                                    page = targetPage,
                                    animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
                                )
                            }
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
                            scope.launch {
                                pagerState.animateScrollToPage(
                                    page = 0,
                                    animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing)
                                )
                            }
                        }) {
                            Icon(
                                Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = stringResource(R.string.reader_first_page),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = stringResource(
                                R.string.reader_page_indicator_format,
                                pagerState.currentPage + 1,
                                totalPages
                            ).uppercase(),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = ComicTitleFontFamily,
                                fontSize = 14.sp,
                                letterSpacing = 1.sp
                            ),
                            color = ComicYellow
                        )

                        IconButton(onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(
                                    page = totalPages - 1,
                                    animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing)
                                )
                            }
                        }) {
                            Icon(
                                Icons.Default.AutoStories,
                                contentDescription = stringResource(R.string.reader_last_page),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
