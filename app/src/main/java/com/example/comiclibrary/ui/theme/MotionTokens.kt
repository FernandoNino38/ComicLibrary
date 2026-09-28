package com.example.comiclibrary.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.ui.unit.IntOffset

/**
 * Master Spring Specification Calibration Table (Technical Report Section 7.2).
 * Replaces static linear tweens with harmonic spring physics (F = -kx - c dx/dt).
 */
object MotionTokens {

    /**
     * DampingRatioNoBouncy:
     * Critically damped terminal stabilization without oscillation.
     * Primary invocation for heavy informative components and tablet panel navigation.
     */
    val PanelTransition: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )

    /**
     * Panel offset transition for slideIn / slideOut animations.
     */
    val PanelOffsetTransition: SpringSpec<IntOffset> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )

    /**
     * DampingRatioLowBouncy:
     * Slight subtle rebound and flexible overshoot.
     * Used for library shelf overscroll and interactive gesture resets.
     */
    val ShelfOverscroll: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    /**
     * DampingRatioMediumBouncy:
     * Expressive charismatic ripple.
     * Reserved for micro-reactions, interactive icons, and floating FAB morphs.
     */
    val MicroInteraction: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )

    /**
     * StiffnessMediumLow:
     * Gentle kinetic deceleration.
     * Employed in panoramic cover sharedElement transitions to grant cinematic legibility.
     */
    val SharedElementCover: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )
}
