package com.tvsencilla.iptv.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class DisplayNamesTest {

    @Test
    fun `se quita la etiqueta de pais en sus formas habituales`() {
        assertEquals("La 1 UHD", displayName("|ES| La 1 UHD"))
        assertEquals("Antena 3", displayName("ES: Antena 3"))
        assertEquals("24 Horas", displayName("[ES] 24 Horas"))
        assertEquals("Cuatro", displayName("ES | Cuatro"))
        assertEquals("La 1", displayName("|ES| |ES| La 1"))
    }

    @Test
    fun `los nombres normales no se tocan`() {
        assertEquals("CMM", displayName("CMM"))
        assertEquals("TV3", displayName("TV3"))
        assertEquals("La 1", displayName("La 1"))
        assertEquals("Movistar Plus+", displayName("Movistar Plus+"))
    }

    @Test
    fun `si solo habia etiqueta se deja el nombre original`() {
        assertEquals("|ES|", displayName("|ES|"))
    }
}
