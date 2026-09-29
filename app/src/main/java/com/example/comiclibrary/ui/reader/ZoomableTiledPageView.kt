package com.example.comiclibrary.ui.reader

import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.max
import kotlinx.coroutines.launch

/**
 * High-performance Page View with responsive pinch-to-zoom, pan, spring double-tap,
 * and zero-leak clipping for the parent HorizontalPager.
 */
@Composable
fun ZoomableTiledPageView(
    bitmap: Bitmap?,
    isLoading: Boolean,
    pageIndex: Int,
    zoomScale: Float,
    onZoomScaleChange: (Float) -> Unit,
    onTapLeft: () -> Unit,
    onTapCenter: () -> Unit,
    onTapRight: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val scaleAnim = remember(pageIndex) { Animatable(zoomScale) }
    val offsetXAnim = remember(pageIndex) { Animatable(0f) }
    val offsetYAnim = remember(pageIndex) { Animatable(0f) }
    var isGestureActive by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        val viewWidthPx = with(density) { maxWidth.toPx() }
        val viewHeightPx = with(density) { maxHeight.toPx() }

        // Synchronize external zoom changes (e.g. from on-screen buttons)
        LaunchedEffect(zoomScale) {
            if (!isGestureActive && abs(scaleAnim.value - zoomScale) > 0.02f) {
                val targetS = zoomScale.coerceIn(1f, 4f)
                val currentMaxX = max(0f, (targetS - 1f) * viewWidthPx / 2f)
                val currentMaxY = max(0f, (targetS - 1f) * viewHeightPx / 2f)
                val targetOx = if (targetS <= 1.05f) 0f else offsetXAnim.value.coerceIn(-currentMaxX, currentMaxX)
                val targetOy = if (targetS <= 1.05f) 0f else offsetYAnim.value.coerceIn(-currentMaxY, currentMaxY)

                coroutineScope.launch {
                    launch {
                        scaleAnim.animateTo(
                            targetS,
                            spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)
                        )
                    }
                    launch {
                        offsetXAnim.animateTo(
                            targetOx,
                            spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)
                        )
                    }
                    launch {
                        offsetYAnim.animateTo(
                            targetOy,
                            spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)
                        )
                    }
                }
            }
        }

        // 1. Taps and double-taps
        val tapModifier = Modifier.pointerInput(pageIndex) {
            detectTapGestures(
                onDoubleTap = { tapOffset ->
                    coroutineScope.launch {
                        if (scaleAnim.value > 1.05f) {
                            // Reset to 1x
                            onZoomScaleChange(1f)
                            launch { scaleAnim.animateTo(1f, spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)) }
                            launch { offsetXAnim.animateTo(0f, spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)) }
                            launch { offsetYAnim.animateTo(0f, spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)) }
                        } else {
                            // Zoom to 2.5x centered on tap
                            val targetS = 2.5f
                            val cx = tapOffset.x - viewWidthPx / 2f
                            val cy = tapOffset.y - viewHeightPx / 2f
                            val maxOx = max(0f, (targetS - 1f) * viewWidthPx / 2f)
                            val maxOy = max(0f, (targetS - 1f) * viewHeightPx / 2f)
                            val targetOx = (-cx * (targetS - 1f)).coerceIn(-maxOx, maxOx)
                            val targetOy = (-cy * (targetS - 1f)).coerceIn(-maxOy, maxOy)

                            onZoomScaleChange(targetS)
                            launch { scaleAnim.animateTo(targetS, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)) }
                            launch { offsetXAnim.animateTo(targetOx, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)) }
                            launch { offsetYAnim.animateTo(targetOy, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)) }
                        }
                    }
                },
                onTap = { offset ->
                    if (scaleAnim.value > 1.05f) {
                        onTapCenter()
                    } else {
                        val screenWidth = size.width
                        when {
                            offset.x < screenWidth * 0.30f -> onTapLeft()
                            offset.x > screenWidth * 0.70f -> onTapRight()
                            else -> onTapCenter()
                        }
                    }
                }
            )
        }

        // 2. Multitouch Pinch-to-zoom & Pan
        val transformModifier = Modifier.pointerInput(pageIndex) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                var zoom = 1f
                var pan = Offset.Zero
                var pastTouchSlop = false
                val touchSlop = viewConfiguration.touchSlop

                do {
                    val event = awaitPointerEvent()
                    val pointerCount = event.changes.count { it.pressed }
                    val isCurrentlyZoomed = scaleAnim.value > 1.05f

                    if (pointerCount >= 2 || isCurrentlyZoomed) {
                        val zoomChange = event.calculateZoom()
                        val panChange = event.calculatePan()
                        val centroid = event.calculateCentroid(useCurrent = false)

                        if (!pastTouchSlop) {
                            zoom *= zoomChange
                            pan += panChange

                            val centroidSize = event.calculateCentroidSize(useCurrent = false)
                            val zoomMotion = abs(1f - zoom) * centroidSize
                            val panMotion = pan.getDistance()

                            // Very responsive pinch detection for 2 fingers
                            val slop = if (pointerCount >= 2) touchSlop * 0.25f else touchSlop
                            if (zoomMotion > slop || (isCurrentlyZoomed && panMotion > slop)) {
                                pastTouchSlop = true
                                isGestureActive = true
                            }
                        }

                        if (pastTouchSlop) {
                            val oldScale = scaleAnim.value
                            val newScale = (oldScale * zoomChange).coerceIn(0.75f, 5.0f)

                            val currentMaxX = max(0f, (newScale - 1f) * viewWidthPx / 2f)
                            val currentMaxY = max(0f, (newScale - 1f) * viewHeightPx / 2f)

                            val cx = centroid.x - viewWidthPx / 2f
                            val cy = centroid.y - viewHeightPx / 2f

                            val newOffsetX = if (oldScale > 0.001f) {
                                ((offsetXAnim.value - cx) * (newScale / oldScale) + cx + panChange.x).coerceIn(-currentMaxX, currentMaxX)
                            } else 0f

                            val newOffsetY = if (oldScale > 0.001f) {
                                ((offsetYAnim.value - cy) * (newScale / oldScale) + cy + panChange.y).coerceIn(-currentMaxY, currentMaxY)
                            } else 0f

                            coroutineScope.launch {
                                scaleAnim.snapTo(newScale)
                                offsetXAnim.snapTo(newOffsetX)
                                offsetYAnim.snapTo(newOffsetY)
                            }

                            onZoomScaleChange(newScale.coerceIn(1f, 4f))

                            event.changes.forEach { change ->
                                if (change.positionChange() != Offset.Zero) {
                                    change.consume()
                                }
                            }
                        }
                    }
                } while (event.changes.any { it.pressed })

                if (pastTouchSlop) {
                    val targetScaleSettled = scaleAnim.value.coerceIn(1f, 4f)
                    val settledMaxX = max(0f, (targetScaleSettled - 1f) * viewWidthPx / 2f)
                    val settledMaxY = max(0f, (targetScaleSettled - 1f) * viewHeightPx / 2f)
                    val targetOffsetX = if (targetScaleSettled <= 1.05f) 0f else offsetXAnim.value.coerceIn(-settledMaxX, settledMaxX)
                    val targetOffsetY = if (targetScaleSettled <= 1.05f) 0f else offsetYAnim.value.coerceIn(-settledMaxY, settledMaxY)

                    coroutineScope.launch {
                        launch {
                            scaleAnim.animateTo(
                                targetScaleSettled,
                                spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)
                            )
                        }
                        launch {
                            offsetXAnim.animateTo(
                                targetOffsetX,
                                spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)
                            )
                        }
                        launch {
                            offsetYAnim.animateTo(
                                targetOffsetY,
                                spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)
                            )
                        }
                        onZoomScaleChange(targetScaleSettled)
                        isGestureActive = false
                    }
                } else {
                    isGestureActive = false
                }
            }
        }

        when {
            bitmap != null && !bitmap.isRecycled -> {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Página ${pageIndex + 1}",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(tapModifier)
                        .then(transformModifier)
                        .graphicsLayer {
                            scaleX = scaleAnim.value
                            scaleY = scaleAnim.value
                            translationX = offsetXAnim.value
                            translationY = offsetYAnim.value
                        }
                )
            }
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.dp
                    )
                }
            }
            else -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Carregando página ${pageIndex + 1}...",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
