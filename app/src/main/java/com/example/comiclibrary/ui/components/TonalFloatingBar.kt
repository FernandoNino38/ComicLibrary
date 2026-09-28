package com.example.comiclibrary.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Material Design 3 Tonal Floating Bar.
 * Replaces GPU-heavy frosted acrylic blur with MD3 Tonal Elevation.
 *
 * Characteristics:
 * - Uses [MaterialTheme.colorScheme.surfaceContainerHigh] for elevated visual prominence.
 * - Hardware-accelerated native Android elevation shadow.
 * - Subtle outline variant border stroke for crisp separation on OLED displays.
 * - Zero blur overhead: preserves battery, eliminates GPU fill-rate throttling and render jank.
 */
@Composable
fun TonalFloatingBar(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 28.dp,
    elevation: Dp = 6.dp,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
    contentPadding: Dp = 8.dp,
    shape: Shape = SquircleShape(cornerRadiusDp = cornerRadius, smoothing = 0.6f),
    content: @Composable BoxScope.() -> Unit
) {
    Surface(
        modifier = modifier.shadow(
            elevation = elevation,
            shape = shape,
            ambientColor = Color.Black.copy(alpha = 0.4f),
            spotColor = Color.Black.copy(alpha = 0.6f)
        ),
        shape = shape,
        color = containerColor,
        tonalElevation = elevation,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Box(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}
