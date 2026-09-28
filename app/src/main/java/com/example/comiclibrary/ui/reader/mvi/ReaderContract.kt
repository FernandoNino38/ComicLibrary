package com.example.comiclibrary.ui.reader.mvi

import com.example.comiclibrary.data.model.ComicBook

/**
 * Concrete error taxonomy for the Reader (Technical Report Section 3.1).
 */
sealed interface ReaderError {
    data object MissingPermission : ReaderError
    data object CorruptArchive : ReaderError
    data object OutOfMemory : ReaderError
    data class Unknown(val message: String) : ReaderError
}

/**
 * MVI State for the CBZ Comic Reader.
 * Adheres to strict Unidirectional Data Flow (UDF).
 */
sealed interface ReaderState {
    data class Loading(
        val progress: Float = 0f,
        val message: String = "Preparando arquivo..."
    ) : ReaderState

    data class Ready(
        val comic: ComicBook,
        val currentPage: Int,
        val totalPages: Int,
        val zoomLevel: Float = 1f,
        val isChromeVisible: Boolean = true,
        val isManga: Boolean = false,
        val isDualPage: Boolean = false
    ) : ReaderState

    data class Error(val errorType: ReaderError) : ReaderState
}

/**
 * MVI Intents for the Comic Reader.
 */
sealed interface ReaderIntent {
    data class LoadComic(val comic: ComicBook) : ReaderIntent
    data class ChangePage(val pageIndex: Int) : ReaderIntent
    data object ToggleChrome : ReaderIntent
    data class SetChromeVisible(val visible: Boolean) : ReaderIntent
    data object ToggleReadingDirection : ReaderIntent
    data object ToggleDualPage : ReaderIntent
    data class UpdateZoom(val scale: Float) : ReaderIntent
    data object Retry : ReaderIntent
}
