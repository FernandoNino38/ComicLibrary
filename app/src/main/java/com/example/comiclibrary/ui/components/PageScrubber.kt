package com.example.comiclibrary.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.comiclibrary.R
import com.example.comiclibrary.theme.ComicTitleFontFamily
import com.example.comiclibrary.theme.ComicYellow

/**
 * Comic-styled interactive page scrubber bar for continuous and discrete comic page jumping.
 */
@Composable
fun PageScrubber(
    currentPage: Int,
    totalPages: Int,
    onPageSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (totalPages <= 1) return

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${stringResource(R.string.reader_page_prefix).uppercase()} ${currentPage + 1}",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = ComicTitleFontFamily,
                    fontSize = 15.sp,
                    letterSpacing = 1.sp
                ),
                color = ComicYellow
            )
            Text(
                text = "${stringResource(R.string.reader_page_of).uppercase()} $totalPages",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = ComicTitleFontFamily,
                    fontSize = 13.sp,
                    letterSpacing = 0.8.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Slider(
            value = currentPage.toFloat(),
            onValueChange = { value ->
                onPageSelected(value.toInt().coerceIn(0, totalPages - 1))
            },
            valueRange = 0f..(totalPages - 1).toFloat(),
            steps = (totalPages - 2).coerceAtLeast(0),
            colors = SliderDefaults.colors(
                thumbColor = ComicYellow,
                activeTrackColor = ComicYellow,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        )
    }
}
