package com.example.comiclibrary.ui.reader

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.comiclibrary.data.archive.CbzArchiveManager
import com.example.comiclibrary.data.model.ComicBook
import com.example.comiclibrary.di.AppDispatchers
import com.example.comiclibrary.ui.reader.mvi.ReaderError
import com.example.comiclibrary.ui.reader.mvi.ReaderIntent
import com.example.comiclibrary.ui.reader.mvi.ReaderState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Collections
import java.util.zip.ZipException
import java.util.zip.ZipFile
import kotlin.math.max

/**
 * Production MVI ViewModel for the CBZ Reader.
 * Implements Section 4 (Extreme Memory Optimization) and Section 5.2 (Concurrency Segregation):
 * - Serialized disk reads via [AppDispatchers.readerPaginator] (limitedParallelism(1)).
 * - Mathematical zoom calculations on [AppDispatchers.mathZoom].
 * - Sliding window LRU memory cache retaining strictly [currentPage - 3, currentPage + 3].
 * - Instant bitmap recycling for evicted pages to avoid native heap spikes.
 * - Dynamic color space: RGB_565 (2 bytes/px, 50% RAM savings) for manga / grayscale.
 */
class ReaderViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow<ReaderState>(ReaderState.Loading())
    val state: StateFlow<ReaderState> = _state.asStateFlow()

    // Page bitmaps sliding window cache: [currentPage - 3 .. currentPage + 3]
    private val _cachedBitmaps = MutableStateFlow<Map<Int, Bitmap>>(emptyMap())
    val cachedBitmaps: StateFlow<Map<Int, Bitmap>> = _cachedBitmaps.asStateFlow()

    private var activeComic: ComicBook? = null
    private var sessionZipFile: ZipFile? = null
    private var sessionTempFile: File? = null
    private var prefetchJob: Job? = null

    // Target render bounds for downsampling calculations (power of two inSampleSize)
    private val targetMaxWidth = 1600
    private val targetMaxHeight = 2560

    fun processIntent(intent: ReaderIntent) {
        when (intent) {
            is ReaderIntent.LoadComic -> loadComic(intent.comic)
            is ReaderIntent.ChangePage -> onPageChanged(intent.pageIndex)
            is ReaderIntent.ToggleChrome -> toggleChrome()
            is ReaderIntent.SetChromeVisible -> setChromeVisible(intent.visible)
            is ReaderIntent.ToggleReadingDirection -> toggleReadingDirection()
            is ReaderIntent.ToggleDualPage -> toggleDualPage()
            is ReaderIntent.UpdateZoom -> updateZoom(intent.scale)
            is ReaderIntent.Retry -> activeComic?.let { loadComic(it) }
        }
    }

    private fun loadComic(comic: ComicBook) {
        activeComic = comic
        _state.value = ReaderState.Loading(progress = 0.1f, message = "Abrindo ${comic.fileName}...")

        viewModelScope.launch {
            cleanActiveSession()

            try {
                _state.value = ReaderState.Loading(progress = 0.3f, message = "Indexando páginas...")

                val uri = Uri.parse(comic.uriString)
                val tempFile = CbzArchiveManager.prepareReadingSession(getApplication(), uri)
                if (tempFile == null || !tempFile.exists()) {
                    _state.value = ReaderState.Error(ReaderError.CorruptArchive)
                    return@launch
                }

                sessionTempFile = tempFile
                sessionZipFile = ZipFile(tempFile)

                _state.value = ReaderState.Loading(progress = 0.8f, message = "Carregando leitura...")

                val initialPage = comic.lastReadPage.coerceIn(0, (comic.totalPages - 1).coerceAtLeast(0))
                val isManga = comic.metadata.isManga

                _state.value = ReaderState.Ready(
                    comic = comic,
                    currentPage = initialPage,
                    totalPages = comic.totalPages,
                    zoomLevel = 1f,
                    isChromeVisible = true,
                    isManga = isManga,
                    isDualPage = false
                )

                // Prefetch window around initial page
                updatePageWindow(initialPage, comic.totalPages, isManga)

            } catch (se: SecurityException) {
                Log.e("ReaderViewModel", "Security exception opening CBZ: ${se.message}", se)
                _state.value = ReaderState.Error(ReaderError.MissingPermission)
            } catch (ze: ZipException) {
                Log.e("ReaderViewModel", "Corrupted CBZ archive: ${ze.message}", ze)
                _state.value = ReaderState.Error(ReaderError.CorruptArchive)
            } catch (oom: OutOfMemoryError) {
                Log.e("ReaderViewModel", "OOM opening CBZ: ${oom.message}", oom)
                _state.value = ReaderState.Error(ReaderError.OutOfMemory)
            } catch (e: Exception) {
                Log.e("ReaderViewModel", "Unexpected error opening CBZ: ${e.message}", e)
                _state.value = ReaderState.Error(ReaderError.Unknown(e.localizedMessage ?: "Erro ao abrir o arquivo."))
            }
        }
    }

    private fun onPageChanged(newIndex: Int) {
        val currentState = _state.value
        if (currentState is ReaderState.Ready) {
            if (currentState.currentPage == newIndex) return
            _state.value = currentState.copy(currentPage = newIndex)
            updatePageWindow(newIndex, currentState.totalPages, currentState.isManga)
        }
    }

    /**
     * Sliding Window LRU memory management (Section 4):
     * Strictly retains pages in [center - 3, center + 3].
     * Immediately purges and recycles bitmaps outside this window.
     */
    private fun updatePageWindow(center: Int, totalPages: Int, isManga: Boolean) {
        prefetchJob?.cancel()

        val minAllowed = (center - 3).coerceAtLeast(0)
        val maxAllowed = (center + 3).coerceAtMost(totalPages - 1)
        val windowRange = minAllowed..maxAllowed

        // 1. Evict and recycle pages outside window
        val currentBitmaps = _cachedBitmaps.value.toMutableMap()
        val evictedKeys = currentBitmaps.keys.filter { it !in windowRange }
        evictedKeys.forEach { key ->
            val evicted = currentBitmaps.remove(key)
            if (evicted != null && !evicted.isRecycled) {
                evicted.recycle()
            }
        }
        _cachedBitmaps.value = currentBitmaps

        // 2. Prefetch pages within window sequentially on serialized paginator dispatcher
        prefetchJob = viewModelScope.launch(AppDispatchers.readerPaginator) {
            val comic = activeComic ?: return@launch
            val zip = sessionZipFile ?: return@launch
            val comicUri = Uri.parse(comic.uriString)

            // Prioritize current page first, then nearest neighbors
            val pagesToLoad = windowRange.sortedBy { kotlin.math.abs(it - center) }

            for (pageIndex in pagesToLoad) {
                if (_cachedBitmaps.value.containsKey(pageIndex)) continue

                val page = comic.pages.getOrNull(pageIndex) ?: continue
                try {
                    val bytes = CbzArchiveManager.getPageBytes(
                        context = getApplication(),
                        uri = comicUri,
                        entryName = page.entryName,
                        sessionZipFile = zip
                    )

                    if (bytes != null) {
                        val bitmap = decodeSampledBitmap(bytes, isManga)
                        if (bitmap != null) {
                            _cachedBitmaps.update { map ->
                                map + (pageIndex to bitmap)
                            }
                        }
                    }
                } catch (oom: OutOfMemoryError) {
                    Log.e("ReaderViewModel", "OOM decoding page $pageIndex: releasing cache", oom)
                    evictFurthestBitmaps(center)
                } catch (e: Exception) {
                    Log.e("ReaderViewModel", "Error loading page $pageIndex: ${e.message}")
                }
            }
        }
    }

    /**
     * Downsampling and color space pipeline (Section 4):
     * 1. Reads image bounds using inJustDecodeBounds = true.
     * 2. Computes power-of-two inSampleSize.
     * 3. Configures RGB_565 for Manga/grayscale (50% RAM savings: 2 bytes/pixel vs 4 bytes/pixel).
     */
    private suspend fun decodeSampledBitmap(bytes: ByteArray, isManga: Boolean): Bitmap? =
        withContext(AppDispatchers.mathZoom) {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

            val rawWidth = options.outWidth
            val rawHeight = options.outHeight
            if (rawWidth <= 0 || rawHeight <= 0) return@withContext null

            var sampleSize = 1
            while ((rawWidth / (sampleSize * 2) >= targetMaxWidth) && (rawHeight / (sampleSize * 2) >= targetMaxHeight)) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = if (isManga) Bitmap.Config.RGB_565 else Bitmap.Config.ARGB_8888
            }

            try {
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
            } catch (oom: OutOfMemoryError) {
                Log.w("ReaderViewModel", "OOM in decodeSampledBitmap, retrying with sampleSize * 2 in RGB_565")
                decodeOptions.inSampleSize = sampleSize * 2
                decodeOptions.inPreferredConfig = Bitmap.Config.RGB_565
                try {
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
                } catch (_: OutOfMemoryError) {
                    null
                }
            }
        }

    private fun evictFurthestBitmaps(center: Int) {
        val current = _cachedBitmaps.value.toMutableMap()
        val furthest = current.keys.filter { it != center }.maxByOrNull { kotlin.math.abs(it - center) }
        if (furthest != null) {
            val bm = current.remove(furthest)
            if (bm != null && !bm.isRecycled) bm.recycle()
            _cachedBitmaps.value = current
        }
    }

    private fun toggleChrome() {
        val current = _state.value
        if (current is ReaderState.Ready) {
            _state.value = current.copy(isChromeVisible = !current.isChromeVisible)
        }
    }

    private fun setChromeVisible(visible: Boolean) {
        val current = _state.value
        if (current is ReaderState.Ready) {
            _state.value = current.copy(isChromeVisible = visible)
        }
    }

    private fun toggleReadingDirection() {
        val current = _state.value
        if (current is ReaderState.Ready) {
            _state.value = current.copy(isManga = !current.isManga)
        }
    }

    private fun toggleDualPage() {
        val current = _state.value
        if (current is ReaderState.Ready) {
            _state.value = current.copy(isDualPage = !current.isDualPage)
        }
    }

    private fun updateZoom(scale: Float) {
        val current = _state.value
        if (current is ReaderState.Ready) {
            _state.value = current.copy(zoomLevel = scale)
        }
    }

    private fun cleanActiveSession() {
        prefetchJob?.cancel()
        _cachedBitmaps.value.values.forEach { if (!it.isRecycled) it.recycle() }
        _cachedBitmaps.value = emptyMap()

        try {
            sessionZipFile?.close()
        } catch (_: Exception) {}
        sessionZipFile = null

        try {
            sessionTempFile?.delete()
        } catch (_: Exception) {}
        sessionTempFile = null
    }

    override fun onCleared() {
        super.onCleared()
        cleanActiveSession()
    }
}
