package com.example.comiclibrary.data.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FilenameMetadataParserTest {

    @Test
    fun parse_standardComicFilename_extractsSeriesNumberAndYear() {
        val fileName = "Spider-Man (2022) #01.cbz"
        val metadata = FilenameMetadataParser.parse(fileName, pageCount = 24)

        assertEquals("Spider-Man #01", metadata.title)
        assertEquals("Spider-Man", metadata.series)
        assertEquals("01", metadata.number)
        assertEquals(2022, metadata.year)
        assertEquals(24, metadata.pageCount)
    }

    @Test
    fun parse_mangaChapterFilename_extractsVolumeAndIssue() {
        val fileName = "One Piece Vol. 10 #101.cbz"
        val metadata = FilenameMetadataParser.parse(fileName, pageCount = 18)

        assertEquals("10", metadata.volume)
        assertEquals("101", metadata.number)
        assertEquals("One Piece", metadata.series)
    }

    @Test
    fun parse_simpleFilenameWithoutMetadata_preservesCleanName() {
        val fileName = "Watchmen.cbz"
        val metadata = FilenameMetadataParser.parse(fileName, pageCount = 32)

        assertEquals("Watchmen", metadata.title)
        assertEquals("Watchmen", metadata.series)
        assertEquals("", metadata.number)
        assertNull(metadata.year)
        assertEquals(32, metadata.pageCount)
    }
}
