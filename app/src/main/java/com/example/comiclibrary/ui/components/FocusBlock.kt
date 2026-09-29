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
 * Comic Book Panel Container (FocusBlock).
 * Styled like an authentic comic book panel with inky outlines and solid depth.
 */
@Composable
fun FocusBlock(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    elevation: Dp = 4.dp,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
    borderWidth: Dp = 1.5.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val squircle = SquircleShape(cornerRadiusDp = cornerRadius, smoothing = 0.5f)

    Surface(
        modifier = modifier
            .shadow(elevation, shape = squircle, spotColor = Color.Black.copy(alpha = 0.6f))
            .clip(squircle)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = squircle,
        color = containerColor,
        tonalElevation = elevation,
        border = BorderStroke(borderWidth, borderColor)
    ) {
        Box(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}
