package com.example.comiclibrary.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.comiclibrary.R

// Comic Book & Onomatopoeia Title Font (Bangers)
val ComicTitleFontFamily = FontFamily(
    Font(R.font.bangers_regular, FontWeight.Normal)
)

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    ),
    displaySmall = TextStyle(
        fontFamily = ComicTitleFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 38.sp,
        lineHeight = 44.sp,
        letterSpacing = 1.5.sp
    )
)
