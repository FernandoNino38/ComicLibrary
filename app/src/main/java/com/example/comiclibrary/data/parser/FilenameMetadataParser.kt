package com.example.comiclibrary.data.parser

import com.example.comiclibrary.data.model.ComicMetadata

/**
 * Regex-based fallback parser for extracting comic metadata directly from raw filenames
 * when ComicInfo.xml is missing or corrupt inside the .cbz archive.
 */
object FilenameMetadataParser {

    private val yearRegex = Regex("""\b(19\d\d|20\d\d)\b""")
    private val volumeRegex = Regex("""(?i)\b(?:v|vol|volume)\.?\s*(\d+)\b""")
    private val issueRegex = Regex("""(?i)(?:#|issue|iss\.?|no\.?)\s*(\d+(?:\.\d+)?)|(?:\s+)(\d{1,4})(?:\.cbz|\.zip|$)""")

    fun parse(fileName: String, pageCount: Int = 0): ComicMetadata {
        // Strip extension
        val baseName = fileName.replace(Regex("""(?i)\.(cbz|zip)$"""), "").trim()

        var year: Int? = null
        yearRegex.find(baseName)?.let { match ->
            year = match.groupValues[1].toIntOrNull()
        }

        var volume = ""
        volumeRegex.find(baseName)?.let { match ->
            volume = match.groupValues[1]
        }

        var issue = ""
        issueRegex.find(baseName)?.let { match ->
            issue = match.groupValues[1].ifEmpty { match.groupValues[2] }
        }

        // Clean series name by removing year, volume, and issue tokens
        var cleanSeries = baseName
        cleanSeries = cleanSeries.replace(Regex("""\((?:19\d\d|20\d\d)\)"""), "")
        cleanSeries = cleanSeries.replace(Regex("""(?i)\b(?:v|vol|volume)\.?\s*\d+\b"""), "")
        if (issue.isNotEmpty()) {
            cleanSeries = cleanSeries.replace(Regex("""(?i)#\s*""" + Regex.escape(issue)), "")
        }
        cleanSeries = cleanSeries.replace(Regex("""[\(\)\[\]_]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim(' ', '-')

        return ComicMetadata(
            title = if (issue.isNotEmpty()) "$cleanSeries #$issue" else cleanSeries,
            series = cleanSeries,
            number = issue,
            volume = volume,
            year = year,
            pageCount = pageCount
        )
    }
}
