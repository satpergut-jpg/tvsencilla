package com.tvsencilla.iptv.domain.search

import com.tvsencilla.iptv.domain.model.Channel
import com.tvsencilla.iptv.domain.model.EpgProgram
import com.tvsencilla.iptv.domain.model.ProgramMatch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VoiceTuningTest {

    private val now = 1_000_000L

    private fun channel(name: String, listNumber: Int, favoriteNumber: Int? = null) = Channel(
        id = name,
        name = name,
        streamUrl = "http://test/$name",
        listNumber = listNumber,
        favoriteNumber = favoriteNumber,
    )

    private fun onAir(channel: Channel, title: String, live: Boolean = true) = ProgramMatch(
        channel = channel,
        program = EpgProgram(
            epgChannelId = channel.id,
            title = title,
            startMillis = if (live) now - 60_000 else now + 3_600_000,
            endMillis = if (live) now + 60_000 else now + 7_200_000,
        ),
    )

    private val antena3 = channel("Antena 3", listNumber = 3)
    private val antena3Hd = channel("Antena 3 HD", listNumber = 103)
    private val antenaNova = channel("Antena Nova", listNumber = 40)
    private val la1 = channel("La 1", listNumber = 1)
    private val movistar = channel("Movistar LaLiga", listNumber = 50)
    private val dazn = channel("DAZN LaLiga", listNumber = 51)
    private val realMadridTv = channel("RealMadrid TV", listNumber = 60)

    @Test
    fun `un unico canal se pone directamente`() {
        assertEquals(la1, VoiceTuning.pick("la uno", listOf(la1), emptyList(), now))
    }

    @Test
    fun `entre variantes del mismo canal se elige el de numero mas bajo`() {
        assertEquals(antena3, VoiceTuning.pick("antena tres", listOf(antena3Hd, antena3), emptyList(), now))
    }

    @Test
    fun `entre variantes gana la que esta en favoritos`() {
        val favoriteHd = antena3Hd.copy(favoriteNumber = 2)
        assertEquals(favoriteHd, VoiceTuning.pick("antena 3", listOf(antena3, favoriteHd), emptyList(), now))
    }

    @Test
    fun `las etiquetas de pais y calidad cuentan como el mismo canal`() {
        val tagged = channel("ES: Antena 3 FHD", listNumber = 203)
        assertEquals(antena3, VoiceTuning.pick("antena tres", listOf(tagged, antena3Hd, antena3), emptyList(), now))
    }

    @Test
    fun `nombres reales de un proveedor con DVB y superindices cuentan como el mismo canal`() {
        val dvb = channel("Antena 3 (DVB)", listNumber = 14)
        val uhd = channel("|ES| Antena 3 ᵁᴴᴰ", listNumber = 15)
        val hd = channel("|ES| Antena 3 ᴴᴰ", listNumber = 16)

        assertEquals(dvb, VoiceTuning.pick("antena tres", listOf(uhd, hd, dvb), emptyList(), now))
    }

    @Test
    fun `si la peticion es ambigua no se adivina`() {
        assertNull(VoiceTuning.pick("antena", listOf(antena3, antenaNova), emptyList(), now))
    }

    @Test
    fun `un partido que se emite ahora en un solo canal se pone`() {
        val programs = listOf(onAir(movistar, "LaLiga: Real Madrid - Barcelona"))
        assertEquals(movistar, VoiceTuning.pick("quiero ver el real madrid", emptyList(), programs, now))
    }

    @Test
    fun `el partido en directo gana a un canal que solo se llama parecido`() {
        val programs = listOf(onAir(movistar, "LaLiga: Real Madrid - Barcelona"))
        assertEquals(movistar, VoiceTuning.pick("real madrid", listOf(realMadridTv), programs, now))
    }

    @Test
    fun `sin partido en directo se pone el unico canal encontrado`() {
        val later = listOf(onAir(movistar, "LaLiga: Real Madrid - Barcelona", live = false))
        assertEquals(realMadridTv, VoiceTuning.pick("real madrid", listOf(realMadridTv), later, now))
    }

    @Test
    fun `si lo emiten varios canales se elige el unico favorito`() {
        val favoriteDazn = dazn.copy(favoriteNumber = 1)
        val programs = listOf(onAir(movistar, "Real Madrid - Barcelona"), onAir(favoriteDazn, "Real Madrid - Barcelona"))
        assertEquals(favoriteDazn, VoiceTuning.pick("real madrid", emptyList(), programs, now))
    }

    @Test
    fun `si lo emiten varios canales y ninguno es favorito se muestra la lista`() {
        val programs = listOf(onAir(movistar, "Real Madrid - Barcelona"), onAir(dazn, "Real Madrid - Barcelona"))
        assertNull(VoiceTuning.pick("real madrid", emptyList(), programs, now))
    }

    @Test
    fun `un partido que aun no ha empezado no se pone solo`() {
        val programs = listOf(onAir(movistar, "Real Madrid - Barcelona", live = false))
        assertNull(VoiceTuning.pick("real madrid", emptyList(), programs, now))
    }

    @Test
    fun `sin resultados no se pone nada`() {
        assertNull(VoiceTuning.pick("canal inventado", emptyList(), emptyList(), now))
    }
}
