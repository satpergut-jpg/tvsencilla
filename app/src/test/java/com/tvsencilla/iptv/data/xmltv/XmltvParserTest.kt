package com.tvsencilla.iptv.data.xmltv

import com.tvsencilla.iptv.domain.model.EpgProgram
import java.io.ByteArrayInputStream
import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class XmltvParserTest {

    private val parser = XmltvParser()

    private fun parse(xml: String): List<EpgProgram> {
        val collected = mutableListOf<EpgProgram>()
        parser.parse(ByteArrayInputStream(xml.trimIndent().toByteArray()), collected::add)
        return collected
    }

    private fun utcMillis(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
        second: Int = 0,
    ): Long = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(year, month - 1, day, hour, minute, second)
    }.timeInMillis

    @Test
    fun `reads channel, title, times and description`() {
        val programs = parse(
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <tv>
              <channel id="la1.es"><display-name>La 1</display-name></channel>
              <programme start="20240115203000 +0100" stop="20240115213000 +0100" channel="la1.es">
                <title lang="es">Telediario</title>
                <desc lang="es">Las noticias del día</desc>
              </programme>
            </tv>
            """,
        )

        val program = programs.single()
        assertEquals("la1.es", program.epgChannelId)
        assertEquals("Telediario", program.title)
        assertEquals("Las noticias del día", program.description)
        // 20:30 at +0100 is 19:30 UTC.
        assertEquals(utcMillis(2024, 1, 15, 19, 30), program.startMillis)
        assertEquals(utcMillis(2024, 1, 15, 20, 30), program.endMillis)
    }

    @Test
    fun `a timestamp with no offset is read as UTC`() {
        assertEquals(utcMillis(2024, 6, 1, 10, 0), XmltvParser.parseTime("20240601100000"))
    }

    @Test
    fun `negative offsets are applied in the right direction`() {
        assertEquals(utcMillis(2024, 6, 1, 15, 0), XmltvParser.parseTime("20240601100000 -0500"))
    }

    @Test
    fun `short timestamps are padded rather than rejected`() {
        assertEquals(utcMillis(2024, 6, 1, 0, 0), XmltvParser.parseTime("20240601"))
        assertEquals(utcMillis(2024, 6, 1, 10, 30), XmltvParser.parseTime("202406011030"))
    }

    @Test
    fun `unusable timestamps return nothing`() {
        assertNull(XmltvParser.parseTime(null))
        assertNull(XmltvParser.parseTime(""))
        assertNull(XmltvParser.parseTime("no"))
    }

    @Test
    fun `only the first title is kept when a guide repeats it per language`() {
        val programs = parse(
            """
            <tv>
              <programme start="20240115200000 +0000" stop="20240115210000 +0000" channel="c1">
                <title lang="es">Película</title>
                <title lang="en">Movie</title>
              </programme>
            </tv>
            """,
        )

        assertEquals("Película", programs.single().title)
    }

    @Test
    fun `programmes without a usable title or span are dropped`() {
        val programs = parse(
            """
            <tv>
              <programme start="20240115200000 +0000" stop="20240115210000 +0000" channel="c1"/>
              <programme start="20240115210000 +0000" stop="20240115210000 +0000" channel="c1">
                <title>Duración cero</title>
              </programme>
              <programme start="20240115220000 +0000" stop="20240115230000 +0000" channel="c1">
                <title>Buena</title>
              </programme>
            </tv>
            """,
        )

        assertEquals(listOf("Buena"), programs.map { it.title })
    }

    @Test
    fun `programmes are handed over one at a time as they are read`() {
        val seen = mutableListOf<String>()
        val xml = buildString {
            append("<tv>")
            repeat(50) { index ->
                append(
                    """
                    <programme start="2024011520${"%02d".format(index % 60)}00 +0000"
                               stop="2024011521${"%02d".format(index % 60)}00 +0000" channel="c$index">
                      <title>Programa $index</title>
                    </programme>
                    """.trimIndent(),
                )
            }
            append("</tv>")
        }

        parser.parse(ByteArrayInputStream(xml.toByteArray())) { seen += it.title }

        assertEquals(50, seen.size)
        assertTrue(seen.first() == "Programa 0")
    }

    @Test
    fun `entities in titles are decoded`() {
        val programs = parse(
            """
            <tv>
              <programme start="20240115200000 +0000" stop="20240115210000 +0000" channel="c1">
                <title>Tom &amp; Jerry</title>
              </programme>
            </tv>
            """,
        )

        assertEquals("Tom & Jerry", programs.single().title)
    }
}
