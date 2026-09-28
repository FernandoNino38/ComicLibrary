package com.example.comiclibrary.ui.reader

import android.graphics.Bitmap
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.comiclibrary.ui.theme.MotionTokens
import kotlin.math.abs
import kotlin.math.max

/**
 * High-performance Page View with pinch-to-zoom, pan, spring-based double tap,
 * and intelligent boundary gesture resolution for the parent HorizontalPager.
 * Receives downsampled bitmaps managed by ReaderViewModel's sliding LRU window.
 */
@Composable
fun ZoomableTiledPageView(
    bitmap: Bitmap?,
    isLoading: Boolean,
    pageIndex: Int,
    onToggleChrome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    // Raw gesture states
    var targetScale by remember { mutableFloatStateOf(1f) }
    var rawOffsetX by remember { mutableFloatStateOf(0f) }
    var rawOffsetY by remember { mutableFloatStateOf(0f) }

    // Spring animated scale for smooth harmonic double-tap zoom transitions
    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = MotionTokens.ShelfOverscroll,
        label = "PageZoomSpring"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        val viewWidthPx = with(density) { maxWidth.toPx() }
        val viewHeightPx = with(density) { maxHeight.toPx() }

        // Maximum allowed offsets given the current scale
        val maxOffsetX = max(0f, (animatedScale - 1f) * viewWidthPx / 2f)
        val maxOffsetY = max(0f, (animatedScale - 1f) * viewHeightPx / 2f)

        // Ensure offsets remain clamped inside page bounds
        val clampedOffsetX = rawOffsetX.coerceIn(-maxOffsetX, maxOffsetX)
        val clampedOffsetY = rawOffsetY.coerceIn(-maxOffsetY, maxOffsetY)

        // Reset offsets when zoomed out
        if (targetScale <= 1.05f && (rawOffsetX != 0f || rawOffsetY != 0f)) {
            rawOffsetX = 0f
            rawOffsetY = 0f
        }

        // Gesture handling modifier
        val gestureModifier = Modifier
            // 1. Double tap & Single tap
            .pointerInput(pageIndex) {
                detectTapGestures(
                    onDoubleTap = {
                        if (targetScale > 1.05f) {
                            targetScale = 1f
                            rawOffsetX = 0f
                            rawOffsetY = 0f
                        } else {
                            targetScale = 2.5f
                        }
                    },
                    onTap = {
                        onToggleChrome()
                    }
                )
            }
            // 2. Multitouch transform + Gesture Conflict Resolution with Parent Pager
            .pointerInput(pageIndex, targetScale, maxOffsetX, maxOffsetY) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var zoom = 1f
                    var pan = Offset.Zero
                    var pastTouchSlop = false

                    do {
                        val event = awaitPointerEvent()
                        val canceled = event.changes.any { it.isConsumed }

                        if (!canceled) {
                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()

                            if (!pastTouchSlop) {
                                zoom *= zoomChange
                                pan += panChange

                                val centroid = event.calculateCentroid(useCurrent = false)
                                val hasMoved = (zoom - 1f).let { abs(it) > 0.05f } || pan.getDistance() > 10f
                                if (hasMoved) {
                                    pastTouchSlop = true
                                }
                            }

                            if (pastTouchSlop) {
                                if (zoomChange != 1f) {
                                    targetScale = (targetScale * zoomChange).coerceIn(1f, 4f)
                                }

                                if (targetScale > 1.05f) {
                                    val currentMaxX = max(0f, (targetScale - 1f) * viewWidthPx / 2f)
                                    val newOffsetX = (rawOffsetX + panChange.x).coerceIn(-currentMaxX, currentMaxX)
                                    val newOffsetY = (rawOffsetY + panChange.y).coerceIn(-maxOffsetY, maxOffsetY)

                                    val isAtLeftEdge = (rawOffsetX >= currentMaxX - 6f)
                                    val isAtRightEdge = (rawOffsetX <= -currentMaxX + 6f)

                                    val movingPastLeft = isAtLeftEdge && panChange.x > 0
                                    val movingPastRight = isAtRightEdge && panChange.x < 0

                                    if (movingPastLeft || movingPastRight) {
                                        // DO NOT consume horizontal drag! Delegate to parent HorizontalPager
                                        rawOffsetY = newOffsetY
                                    } else {
                                        // Still panning inside zoomed comic: consume event
                                        rawOffsetX = newOffsetX
                                        rawOffsetY = newOffsetY
                                        event.changes.forEach { change ->
                                            if (change.positionChange() != Offset.Zero) {
                                                change.consume()
                                            }
                                        }
                                    }
                                } else {
                                    rawOffsetX = 0f
                                    rawOffsetY = 0f
                                }
                            }
                        }
                    } while (!canceled && event.changes.any { it.pressed })
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
                        .then(gestureModifier)
                        .graphicsLayer {
                            scaleX = animatedScale
                            scaleY = animatedScale
                            translationX = clampedOffsetX
                            translationY = clampedOffsetY
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
