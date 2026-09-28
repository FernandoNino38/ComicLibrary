package com.example.comiclibrary.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Metadata extracted from ComicInfo.xml adhering to the ComicRack specification,
 * or deduced through filename regex parsing fallback.
 */
@Parcelize
data class ComicMetadata(
    val title: String = "",
    val series: String = "",
    val number: String = "",
    val volume: String = "",
    val summary: String = "",
    val year: Int? = null,
    val month: Int? = null,
    val pageCount: Int = 0,
    val writer: String = "",
    val penciller: String = "",
    val inker: String = "",
    val colorist: String = "",
    val letterer: String = "",
    val coverArtist: String = "",
    val editor: String = "",
    val publisher: String = "",
    val genre: String = "",
    val ageRating: String = "",
    val isManga: Boolean = false, // When true, reading order is Right-To-Left (RTL)
    val web: String = ""
) : Parcelable {
    val displayTitle: String
        get() = when {
            title.isNotBlank() && series.isNotBlank() && title != series -> "$series - $title"
            title.isNotBlank() -> title
            series.isNotBlank() && number.isNotBlank() -> "$series #$number"
            series.isNotBlank() -> series
            else -> "Untitled Comic"
        }

    val creatorsSummary: String
        get() {
            val list = mutableListOf<String>()
            if (writer.isNotBlank()) list.add("Writer: $writer")
            if (penciller.isNotBlank()) list.add("Pencils: $penciller")
            if (inker.isNotBlank()) list.add("Inks: $inker")
            return list.joinToString(" • ")
        }
}

/**
 * Represents a comic book page entry within a CBZ archive.
 */
@Parcelize
data class ComicPage(
    val index: Int,
    val entryName: String,
    val width: Int = 0,
    val height: Int = 0
) : Parcelable

/**
 * Represents an imported or scanned CBZ file in the library.
 */
@Parcelize
data class ComicBook(
    val id: String,
    val uriString: String,
    val fileName: String,
    val fileSizeBytes: Long = 0L,
    val coverPath: String? = null,
    val pages: List<ComicPage> = emptyList(),
    val metadata: ComicMetadata = ComicMetadata(),
    val lastReadPage: Int = 0,
    val isFinished: Boolean = false,
    val lastOpenedTimestamp: Long = 0L,
    val isFavorite: Boolean = false
) : Parcelable {
    val totalPages: Int
        get() = if (pages.isNotEmpty()) pages.size else metadata.pageCount

    val progressPercent: Float
        get() = if (totalPages > 0) (lastReadPage.toFloat() / totalPages.toFloat()).coerceIn(0f, 1f) else 0f
}
