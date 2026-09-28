package com.example.comiclibrary.ui.components

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Superellipse Squircle shape adhering to iOS 26/27 continuous curvature and One UI 9 Focus Blocks.
 * Employs androidx.graphics.shapes.RoundedPolygon with a smoothing factor of 0.6f (60%).
 * Dynamically adjusts radius using the trigonometric equation arcHeight = sqrt(radius^2 - (radius - width)^2)
 * when elements are narrow to prevent destructive arc overlaps.
 */
class SquircleShape(
    private val cornerRadiusDp: Dp = 20.dp,
    private val smoothing: Float = 0.6f
) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val width = size.width
        val height = size.height
        if (width <= 0f || height <= 0f) {
            return Outline.Generic(Path())
        }

        val requestedRadius = with(density) { cornerRadiusDp.toPx() }
        val minDim = min(width, height)

        // Dynamic trigonometric arc compensation for narrow widths:
        // arcHeight = sqrt(radius^2 - (radius - width)^2)
        val effectiveRadius = if (width < requestedRadius * 2f && width > requestedRadius) {
            val delta = requestedRadius - width
            val arcHeight = sqrt((requestedRadius * requestedRadius - delta * delta).coerceAtLeast(0f))
            min(arcHeight, minDim / 2f)
        } else {
            min(requestedRadius, minDim / 2f)
        }

        val polygon = RoundedPolygon(
            vertices = floatArrayOf(
                0f, 0f,
                width, 0f,
                width, height,
                0f, height
            ),
            rounding = CornerRounding(radius = effectiveRadius, smoothing = smoothing)
        )

        val composePath = polygon.toPath().asComposePath()
        return Outline.Generic(composePath)
    }
}
