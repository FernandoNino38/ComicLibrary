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
    primary = OneUIAccentBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1E3A66),
    onPrimaryContainer = Color(0xFFD6E4FF),
    secondary = OneUIAccentCyan,
    onSecondary = Color.Black,
    tertiary = OneUIAccentPink,
    background = AmoledBlack,
    onBackground = Color(0xFFF0F2F5),
    surface = DarkSurface,
    onSurface = Color(0xFFF0F2F5),
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFB0B6C3),
    surfaceContainerLowest = DarkSurfaceContainerLowest,
    surfaceContainerLow = DarkSurfaceContainerLow,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerHighest = DarkSurfaceContainerHighest,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant
)

private val LightColorScheme = lightColorScheme(
    primary = OneUIAccentBlue,
    onPrimary = Color.White,
    secondary = OneUIAccentCyan,
    onSecondary = Color.Black,
    tertiary = OneUIAccentPink,
    background = LightBackground,
    onBackground = Color(0xFF1C1E21),
    surface = LightSurface,
    onSurface = Color(0xFF1C1E21),
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Color(0xFF5F6672),
    surfaceContainerLowest = LightSurfaceContainerLowest,
    surfaceContainerLow = LightSurfaceContainerLow,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    surfaceContainerHighest = LightSurfaceContainerHighest,
    outline = LightOutline,
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
