package com.example.comiclibrary.ui.components

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Legacy compatibility alias delegating to [TonalFloatingBar].
 * Blur / RenderEffect has been removed in favor of Material Design 3 Tonal Elevation.
 */
@Composable
fun TranslucentFloatingBar(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 28.dp,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
    contentPadding: Dp = 8.dp,
    content: @Composable BoxScope.() -> Unit
) {
    TonalFloatingBar(
        modifier = modifier,
        cornerRadius = cornerRadius,
        elevation = 6.dp,
        containerColor = backgroundColor,
        borderColor = borderColor,
        contentPadding = contentPadding,
        content = content
    )
}
