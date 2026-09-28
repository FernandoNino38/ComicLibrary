package com.example.comiclibrary.data.parser

import com.example.comiclibrary.data.model.ComicMetadata
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream

/**
 * Ultra-fast, zero-DOM streaming parser for ComicInfo.xml using XmlPullParser.
 * Avoids allocating in-memory Document Object Model trees, reducing Garbage Collection pauses (jank).
 */
object ComicInfoXmlParser {

    fun parse(inputStream: InputStream): ComicMetadata {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = false
        val parser = factory.newPullParser()
        parser.setInput(inputStream, "UTF-8")

        var title = ""
        var series = ""
        var number = ""
        var volume = ""
        var summary = ""
        var year: Int? = null
        var month: Int? = null
        var pageCount = 0
        var writer = ""
        var penciller = ""
        var inker = ""
        var colorist = ""
        var letterer = ""
        var coverArtist = ""
        var editor = ""
        var publisher = ""
        var genre = ""
        var ageRating = ""
        var isManga = false
        var web = ""

        var eventType = parser.eventType
        var currentTag = ""

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    currentTag = parser.name ?: ""
                }
                XmlPullParser.TEXT -> {
                    val text = parser.text?.trim() ?: ""
                    if (text.isNotEmpty()) {
                        when (currentTag.lowercase()) {
                            "title" -> title = text
                            "series" -> series = text
                            "number" -> number = text
                            "volume" -> volume = text
                            "summary" -> summary = text
                            "year" -> year = text.toIntOrNull()
                            "month" -> month = text.toIntOrNull()
                            "pagecount" -> pageCount = text.toIntOrNull() ?: pageCount
                            "writer" -> writer = text
                            "penciller" -> penciller = text
                            "inker" -> inker = text
                            "colorist" -> colorist = text
                            "letterer" -> letterer = text
                            "coverartist" -> coverArtist = text
                            "editor" -> editor = text
                            "publisher" -> publisher = text
                            "genre" -> genre = text
                            "agerating" -> ageRating = text
                            "manga" -> {
                                val lower = text.lowercase()
                                isManga = lower == "yes" || lower == "yesandrighttoleft" || lower == "true"
                            }
                            "web" -> web = text
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    currentTag = ""
                }
            }
            eventType = parser.next()
        }

        return ComicMetadata(
            title = title,
            series = series,
            number = number,
            volume = volume,
            summary = summary,
            year = year,
            month = month,
            pageCount = pageCount,
            writer = writer,
            penciller = penciller,
            inker = inker,
            colorist = colorist,
            letterer = letterer,
            coverArtist = coverArtist,
            editor = editor,
            publisher = publisher,
            genre = genre,
            ageRating = ageRating,
            isManga = isManga,
            web = web
        )
    }
}
