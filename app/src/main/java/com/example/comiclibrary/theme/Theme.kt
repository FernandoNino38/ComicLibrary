package com.example.comiclibrary.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = ComicYellow,
    onPrimary = ComicInkBlack,
    primaryContainer = Color(0xFF383000),
    onPrimaryContainer = ComicYellowVariant,
    secondary = ComicRed,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF4A0A0A),
    onSecondaryContainer = Color(0xFFFF8A80),
    tertiary = ComicCyan,
    onTertiary = ComicInkBlack,
    tertiaryContainer = Color(0xFF003847),
    onTertiaryContainer = Color(0xFF80EAFF),
    background = AmoledBlack,
    onBackground = ComicPaperWhite,
    surface = DarkSurface,
    onSurface = ComicPaperWhite,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFB8BAC4),
    surfaceContainerLowest = DarkSurfaceContainerLowest,
    surfaceContainerLow = DarkSurfaceContainerLow,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerHighest = DarkSurfaceContainerHighest,
    outline = ComicYellow.copy(alpha = 0.5f),
    outlineVariant = DarkOutlineVariant
)

private val LightColorScheme = lightColorScheme(
    primary = ComicYellow,
    onPrimary = ComicInkBlack,
    secondary = ComicRed,
    onSecondary = Color.White,
    tertiary = ComicCyan,
    onTertiary = ComicInkBlack,
    background = LightBackground,
    onBackground = ComicInkBlack,
    surface = LightSurface,
    onSurface = ComicInkBlack,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Color(0xFF404048),
    surfaceContainerLowest = LightSurfaceContainerLowest,
    surfaceContainerLow = LightSurfaceContainerLow,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    surfaceContainerHighest = LightSurfaceContainerHighest,
    outline = ComicInkBlack.copy(alpha = 0.6f),
    outlineVariant = LightOutlineVariant
)

@Composable
fun ComicLibraryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
