package com.example.comiclibrary.image.tiling

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.InputStream

/**
 * High-performance tiling engine leveraging Android's BitmapRegionDecoder.
 * Selectively decodes only the visible viewport sub-rectangles when zoomed in,
 * bypassing GPU texture limit bottlenecks (2048x2048 / 4096x4096) and preventing OOM errors.
 */
class TiledBitmapEngine(private val imageBytes: ByteArray) {

    private var regionDecoder: BitmapRegionDecoder? = null
    var originalWidth: Int = 0
        private set
    var originalHeight: Int = 0
        private set

    init {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options)
        originalWidth = options.outWidth
        originalHeight = options.outHeight

        try {
            val stream: InputStream = ByteArrayInputStream(imageBytes)
            regionDecoder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                BitmapRegionDecoder.newInstance(stream)
            } else {
                @Suppress("DEPRECATION")
                BitmapRegionDecoder.newInstance(stream, false)
            }
        } catch (e: Exception) {
            Log.e("TiledBitmapEngine", "Failed to initialize BitmapRegionDecoder", e)
        }
    }

    /**
     * Decodes a specific rectangular region of the image for the current viewport.
     */
    suspend fun decodeRegion(rect: Rect, sampleSize: Int = 1): Bitmap? = withContext(Dispatchers.Default) {
        val decoder = regionDecoder ?: return@withContext null
        if (rect.isEmpty || rect.left >= originalWidth || rect.top >= originalHeight) return@withContext null

        val clampedRect = Rect(
            rect.left.coerceIn(0, originalWidth),
            rect.top.coerceIn(0, originalHeight),
            rect.right.coerceIn(0, originalWidth),
            rect.bottom.coerceIn(0, originalHeight)
        )

        if (clampedRect.width() <= 0 || clampedRect.height() <= 0) return@withContext null

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize.coerceAtLeast(1)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        try {
            decoder.decodeRegion(clampedRect, options)
        } catch (e: Exception) {
            Log.e("TiledBitmapEngine", "Error decoding region: $clampedRect", e)
            null
        }
    }

    /**
     * Decodes the full image downsampled to fit container dimensions (baseline image).
     */
    suspend fun decodeBaseSampled(maxWidth: Int, maxHeight: Int): Bitmap? = withContext(Dispatchers.Default) {
        if (originalWidth <= 0 || originalHeight <= 0) return@withContext null

        var sampleSize = 1
        while ((originalWidth / (sampleSize * 2) >= maxWidth) && (originalHeight / (sampleSize * 2) >= maxHeight)) {
            sampleSize *= 2
        }

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options)
    }

    /**
     * Releases decoder resources to prevent memory leaks and file descriptor leaks.
     */
    fun recycle() {
        try {
            regionDecoder?.recycle()
            regionDecoder = null
        } catch (_: Exception) {}
    }
}
