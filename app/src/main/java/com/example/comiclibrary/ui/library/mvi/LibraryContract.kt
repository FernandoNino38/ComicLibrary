package com.example.comiclibrary.ui.library.mvi

import android.net.Uri
import com.example.comiclibrary.data.model.ComicBook

/**
 * Library MVI State containing collection, active filters, search, and progress.
 */
data class LibraryState(
    val comics: List<ComicBook> = emptyList(),
    val isLoading: Boolean = false,
    val importProgress: Pair<Int, Int>? = null,
    val selectedComicId: String? = null,
    val searchQuery: String = "",
    val selectedFilter: String = "Todos",
    val selectedSeries: String? = null,
    val displayLimit: Int = 20,
    val errorMessage: String? = null
) {
    val availableSeries: List<String>
        get() = comics.map { it.metadata.series }.filter { it.isNotBlank() }.distinct().sorted()

    val filteredComics: List<ComicBook>
        get() = comics.filter { comic ->
            val matchesQuery = searchQuery.isBlank() ||
                    comic.metadata.displayTitle.contains(searchQuery, ignoreCase = true) ||
                    comic.metadata.writer.contains(searchQuery, ignoreCase = true) ||
                    comic.metadata.genre.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                "Favoritos" -> comic.isFavorite
                "Lendo" -> comic.lastReadPage > 0 && !comic.isFinished
                "Concluídos" -> comic.isFinished
                "Mangá" -> comic.metadata.isManga
                else -> true
            }

            val matchesSeries = selectedSeries == null || comic.metadata.series.equals(selectedSeries, ignoreCase = true)

            matchesQuery && matchesFilter && matchesSeries
        }.take(displayLimit)

    val hasMoreToLoad: Boolean
        get() = comics.filter { comic ->
            val matchesQuery = searchQuery.isBlank() ||
                    comic.metadata.displayTitle.contains(searchQuery, ignoreCase = true) ||
                    comic.metadata.writer.contains(searchQuery, ignoreCase = true) ||
                    comic.metadata.genre.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (selectedFilter) {
                "Favoritos" -> comic.isFavorite
                "Lendo" -> comic.lastReadPage > 0 && !comic.isFinished
                "Concluídos" -> comic.isFinished
                "Mangá" -> comic.metadata.isManga
                else -> true
            }
            val matchesSeries = selectedSeries == null || comic.metadata.series.equals(selectedSeries, ignoreCase = true)
            matchesQuery && matchesFilter && matchesSeries
        }.size > displayLimit

    val selectedComic: ComicBook?
        get() = comics.firstOrNull { it.id == selectedComicId } ?: comics.firstOrNull()
}

/**
 * Library User Intents.
 */
sealed interface LibraryIntent {
    data class ImportFiles(val uris: List<Uri>) : LibraryIntent
    data class ImportFolder(val treeUri: Uri) : LibraryIntent
    data class SelectComic(val comicId: String?) : LibraryIntent
    data class ToggleFavorite(val comicId: String) : LibraryIntent
    data class DeleteComic(val comicId: String) : LibraryIntent
    data class UpdateProgress(val comicId: String, val pageIndex: Int) : LibraryIntent
    data class UpdateSearch(val query: String) : LibraryIntent
    data class UpdateFilter(val filter: String) : LibraryIntent
    data class UpdateSeries(val series: String?) : LibraryIntent
    data object LoadMore : LibraryIntent
    data object DismissError : LibraryIntent
}
