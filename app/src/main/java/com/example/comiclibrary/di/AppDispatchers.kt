package com.example.comiclibrary.di

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * Tactical Coroutine Dispatcher Segregation (Technical Report Section 5.2).
 * Prevents the thread-exhaustion trap where unbounded Dispatchers.IO creates up to 64 threads,
 * consuming 128 MB of physical RAM simply for call stacks.
 */
object AppDispatchers {

    /**
     * Bulk Indexing & Cover Extraction:
     * Restricts background I/O to strictly 4 workers. Maintains computational memory stabilized
     * at ~8 MB instead of 128 MB.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val libraryIndexing: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(4)

    /**
     * Main CBZ Reader Paginator:
     * Absolute serialization (1 worker). Guarantees sequential page extractions do not conflict
     * on the storage controller or disc read head, forcing orderly decompression.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val readerPaginator: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(1)

    /**
     * Internal Zoom & Crop Engine / CPU-bound math:
     * Employs CPU-bound work-stealing algorithms synchronized to available cores for matrix transforms.
     */
    val zoomAndCrop: CoroutineDispatcher = Dispatchers.Default
    val mathZoom: CoroutineDispatcher = zoomAndCrop

    /**
     * UI Thread:
     * Reserved strictly for 60/120 FPS rendering.
     */
    val main: CoroutineDispatcher = Dispatchers.Main
}
