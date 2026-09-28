package com.example.comiclibrary.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Material Design 3 Elevated Focus Block Container.
 * An expansive card container with elevated squircle geometry (0.6f smoothing)
 * that organizes content into clear ergonomic hierarchies using MD3 tonal elevation.
 */
@Composable
fun FocusBlock(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    elevation: Dp = 3.dp,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val squircle = SquircleShape(cornerRadiusDp = cornerRadius, smoothing = 0.6f)

    Surface(
        modifier = modifier
            .shadow(elevation, shape = squircle, spotColor = Color.Black.copy(alpha = 0.35f))
            .clip(squircle)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = squircle,
        color = containerColor,
        tonalElevation = elevation,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Box(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}
