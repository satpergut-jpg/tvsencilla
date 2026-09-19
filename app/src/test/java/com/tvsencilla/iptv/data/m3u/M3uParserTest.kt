package com.tvsencilla.iptv.data.m3u

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class M3uParserTest {

    private val parser = M3uParser()

    private fun parse(playlist: String): List<M3uEntry> =
        parser.parse(playlist.trimIndent().lineSequence()).toList()

    @Test
    fun `reads name, url and the usual attributes`() {
        val entries = parse(
            """
            #EXTM3U
            #EXTINF:-1 tvg-id="la1.es" tvg-logo="http://logos.test/la1.png" group-title="Noticias",La 1
            http://provider.test/live/la1.m3u8
            """,
        )

        assertEquals(1, entries.size)
        val entry = entries.single()
        assertEquals("La 1", entry.name)
        assertEquals("http://provider.test/live/la1.m3u8", entry.url)
        assertEquals("la1.es", entry.tvgId)
        assertEquals("http://logos.test/la1.png", entry.logoUrl)
        assertEquals("Noticias", entry.groupTitle)
    }

    @Test
    fun `a comma inside an attribute does not cut the name short`() {
        val entries = parse(
            """
            #EXTM3U
            #EXTINF:-1 tvg-name="Canal, uno" group-title="Cine, clásico",Canal Uno HD
            http://provider.test/1
            """,
        )

        assertEquals("Canal Uno HD", entries.single().name)
        assertEquals("Cine, clásico", entries.single().groupTitle)
    }

    @Test
    fun `EXTGRP supplies the group when there is no group-title`() {
        val entries = parse(
            """
            #EXTM3U
            #EXTINF:-1 tvg-id="x",Canal X
            #EXTGRP:Deportes
            http://provider.test/x
            """,
        )

        assertEquals("Deportes", entries.single().groupTitle)
    }

    @Test
    fun `group-title wins over EXTGRP`() {
        val entries = parse(
            """
            #EXTINF:-1 group-title="Infantil",Canal Y
            #EXTGRP:Deportes
            http://provider.test/y
            """,
        )

        assertEquals("Infantil", entries.single().groupTitle)
    }

    @Test
    fun `unknown directives and blank lines are skipped`() {
        val entries = parse(
            """
            #EXTM3U url-tvg="http://guide.test/epg.xml"
            #EXTINF:-1,Canal A

            #EXTVLCOPT:http-user-agent=Mozilla
            #KODIPROP:inputstream=inputstream.adaptive
            http://provider.test/a
            #EXTINF:-1,Canal B
            http://provider.test/b
            """,
        )

        assertEquals(listOf("Canal A", "Canal B"), entries.map { it.name })
        assertEquals(listOf("http://provider.test/a", "http://provider.test/b"), entries.map { it.url })
    }

    @Test
    fun `catch-up is detected from any of the dialects providers use`() {
        val entries = parse(
            """
            #EXTINF:-1 catchup="default" catchup-days="7",Con archivo
            http://provider.test/1
            #EXTINF:-1 timeshift="3",Con timeshift
            http://provider.test/2
            #EXTINF:-1 catchup="0",Sin archivo
            http://provider.test/3
            #EXTINF:-1,Nada declarado
            http://provider.test/4
            """,
        )

        assertTrue(entries[0].supportsCatchUp)
        assertEquals(7, entries[0].catchUpDays)
        assertTrue(entries[1].supportsCatchUp)
        assertEquals(3, entries[1].catchUpDays)
        assertFalse(entries[2].supportsCatchUp)
        assertFalse(entries[3].supportsCatchUp)
        assertNull(entries[3].catchUpDays)
    }

    @Test
    fun `tvg-name is used when the name after the comma is missing`() {
        val entries = parse(
            """
            #EXTINF:-1 tvg-name="Canal Sin Nombre",
            http://provider.test/z
            """,
        )

        assertEquals("Canal Sin Nombre", entries.single().name)
    }

    @Test
    fun `an EXTINF with no url is dropped rather than half parsed`() {
        val entries = parse(
            """
            #EXTINF:-1,Canal Huérfano
            #EXTINF:-1,Canal Bueno
            http://provider.test/ok
            """,
        )

        assertEquals(listOf("Canal Bueno"), entries.map { it.name })
    }

    @Test
    fun `attribute names are matched regardless of case`() {
        val entries = parse(
            """
            #EXTINF:-1 TVG-ID="upper.es" TVG-LOGO="http://logos.test/u.png",Canal Mayúsculas
            http://provider.test/u
            """,
        )

        assertEquals("upper.es", entries.single().tvgId)
        assertEquals("http://logos.test/u.png", entries.single().logoUrl)
    }

    @Test
    fun `parsing stays lazy so huge playlists are never held in memory`() {
        var linesRead = 0
        val lines = sequence {
            repeat(100_000) { index ->
                linesRead++
                yield("#EXTINF:-1,Canal $index")
                linesRead++
                yield("http://provider.test/$index")
            }
        }

        val first = parser.parse(lines).first()

        assertEquals("Canal 0", first.name)
        // Only the lines needed for the first entry were pulled from the source.
        assertTrue("read $linesRead lines, expected a handful", linesRead < 10)
    }
}
