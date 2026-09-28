package com.example.comiclibrary.data.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class ComicInfoXmlParserTest {

    @Test
    fun parse_validComicInfoXml_extractsAllAttributesAccurately() {
        val xml = """
            <?xml version="1.0" encoding="utf-8"?>
            <ComicInfo>
                <Title>The Court of Owls</Title>
                <Series>Batman</Series>
                <Number>1</Number>
                <Volume>2</Volume>
                <Summary>Batman investigates a brutal murder in Gotham City.</Summary>
                <Year>2011</Year>
                <Month>9</Month>
                <Writer>Scott Snyder</Writer>
                <Penciller>Greg Capullo</Penciller>
                <Inker>Jonathan Glapion</Inker>
                <Publisher>DC Comics</Publisher>
                <Genre>Superhero / Mystery</Genre>
                <Manga>No</Manga>
                <PageCount>32</PageCount>
            </ComicInfo>
        """.trimIndent()

        val metadata = ComicInfoXmlParser.parse(ByteArrayInputStream(xml.toByteArray()))

        assertEquals("The Court of Owls", metadata.title)
        assertEquals("Batman", metadata.series)
        assertEquals("1", metadata.number)
        assertEquals("2", metadata.volume)
        assertEquals("Batman investigates a brutal murder in Gotham City.", metadata.summary)
        assertEquals(2011, metadata.year)
        assertEquals("Scott Snyder", metadata.writer)
        assertEquals("Greg Capullo", metadata.penciller)
        assertEquals("Jonathan Glapion", metadata.inker)
        assertEquals("DC Comics", metadata.publisher)
        assertEquals("Superhero / Mystery", metadata.genre)
        assertFalse(metadata.isManga)
        assertEquals(32, metadata.pageCount)
    }

    @Test
    fun parse_mangaComicInfo_flagsMangaRtlTrue() {
        val xml = """
            <?xml version="1.0" encoding="utf-8"?>
            <ComicInfo>
                <Title>Enter Naruto</Title>
                <Series>Naruto</Series>
                <Number>1</Number>
                <Manga>YesAndRightToLeft</Manga>
                <PageCount>45</PageCount>
            </ComicInfo>
        """.trimIndent()

        val metadata = ComicInfoXmlParser.parse(ByteArrayInputStream(xml.toByteArray()))

        assertEquals("Naruto", metadata.series)
        assertTrue(metadata.isManga)
        assertEquals(45, metadata.pageCount)
    }
}
