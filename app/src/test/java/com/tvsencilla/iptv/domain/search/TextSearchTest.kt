package com.tvsencilla.iptv.domain.search

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextSearchTest {

    // --- Palabras clave y coincidencias ---

    @Test
    fun `una frase hablada se queda en las palabras que importan`() {
        assertEquals(listOf("real", "madrid"), TextSearch.keywords("Quiero ver el Real Madrid"))
        assertEquals(listOf("barcelona"), TextSearch.keywords("¿Dónde echan el partido del Barcelona?"))
    }

    @Test
    fun `mayusculas y tildes no importan`() {
        val keywords = TextSearch.keywords("atletico de madrid")

        assertTrue(TextSearch.matches("LaLiga: Atlético de Madrid - Sevilla", keywords))
        assertTrue(TextSearch.matches("ATLETICO MADRID EN DIRECTO", keywords))
    }

    @Test
    fun `hacen falta todas las palabras, no solo una`() {
        val keywords = TextSearch.keywords("Real Madrid")

        assertTrue(TextSearch.matches("LaLiga EA Sports: Real Madrid - Barcelona", keywords))
        assertFalse("Atlético de Madrid no es el Real Madrid", TextSearch.matches("Atlético de Madrid - Getafe", keywords))
        assertFalse(TextSearch.matches("Real Sociedad - Osasuna", keywords))
    }

    @Test
    fun `escrito todo junto encuentra lo que va separado`() {
        assertTrue(TextSearch.matches("LaLiga: Real Madrid - Barcelona", TextSearch.keywords("realmadrid")))
    }

    @Test
    fun `escrito separado encuentra lo que va todo junto`() {
        assertTrue(TextSearch.matches("RealMadrid TV", TextSearch.keywords("real madrid")))
    }

    @Test
    fun `las palabras de menos de tres letras no son palabra clave`() {
        assertEquals(listOf("24h"), TextSearch.keywords("canal 24h"))
        assertTrue(TextSearch.keywords("La 1").isEmpty())
    }

    @Test
    fun `los numeros en letra pasan a cifra`() {
        assertEquals("la 1", TextSearch.phrase("La Uno"))
        assertEquals("antena 3", TextSearch.phrase("Antena Tres"))
        assertEquals("antena 3", TextSearch.phrase("Antena 3"))
    }

    @Test
    fun `un numero acompanando a una palabra tambien es obligatorio`() {
        assertEquals(listOf("antena", "3"), TextSearch.keywords("antena tres"))
        assertEquals(listOf("antena", "3"), TextSearch.keywords("quiero ver antena 3"))
    }

    @Test
    fun `los numeros coinciden enteros`() {
        val keywords = TextSearch.keywords("antena tres")
        assertTrue(TextSearch.matches("Antena 3 HD", keywords))
        assertTrue(TextSearch.matches("Antena Tres", keywords))
        assertFalse(TextSearch.matches("Antena 30", keywords))
        assertFalse(TextSearch.matches("Antena Nova", keywords))
    }

    @Test
    fun `si todo son palabras de relleno se busca con ellas igualmente`() {
        assertEquals(listOf("partido"), TextSearch.keywords("el partido"))
    }

    @Test
    fun `una busqueda sin palabras utiles no encuentra nada`() {
        assertTrue(TextSearch.keywords("de el").isEmpty())
        assertFalse(TextSearch.matches("Real Madrid", emptyList()))
    }

    @Test
    fun `el patron de la base de datos tolera las vocales con tilde`() {
        assertEquals("%_tl_t_c_%", TextSearch.likePattern("atletico"))
    }

    @Test
    fun `las letras en superindice se leen como letras normales`() {
        assertEquals("antena 3 uhd", TextSearch.phrase("Antena 3 ᵁᴴᴰ"))
        // "la 1 hd" no tiene palabras de tres letras: se busca por la frase entera.
        assertTrue(TextSearch.phrase("|ES| La 1 ᴴᴰ").contains(TextSearch.phrase("la 1 hd")))
    }

    @Test
    fun `la enie se trata como una n`() {
        val keywords = TextSearch.keywords("España")
        assertTrue(TextSearch.matches("Selección: Espana - Italia", keywords))
        assertTrue(TextSearch.matches("Fútbol: España - Francia", keywords))
    }

    // --- Búsqueda completa, con una base de datos simulada ---

    private val titles = listOf(
        "La 1",
        "La 10 Noticias",
        "LaLiga: Real Madrid - Barcelona",
        "Atlético de Madrid - Getafe",
        "Real Sociedad - Osasuna",
        "RealMadrid TV",
        "Antena 3",
        "Antena Nova",
        "Antena 30 Música",
        "Cuatro",
        "Canal Dos Andalucía",
    )

    /** Interpreta LIKE como SQLite: % es cualquier texto, _ un carácter, y sin distinguir mayúsculas. */
    private suspend fun search(query: String): List<String> =
        TextSearch.find(
            query = query,
            fetch = { pattern ->
                val regex = Regex(
                    pattern.map {
                        when (it) {
                            '%' -> ".*"
                            '_' -> "."
                            else -> Regex.escape(it.toString())
                        }
                    }.joinToString(""),
                    RegexOption.IGNORE_CASE,
                )
                titles.filter { regex.matches(it) }
            },
            text = { it },
        ).sorted()

    @Test
    fun `quiero ver realmadrid, realmadrid y real madrid dan lo mismo`() = runTest {
        val expected = listOf("LaLiga: Real Madrid - Barcelona", "RealMadrid TV")

        assertEquals(expected, search("quiero ver realmadrid"))
        assertEquals(expected, search("realmadrid"))
        assertEquals(expected, search("Real Madrid"))
        assertEquals(expected, search("real madrid"))
        assertEquals(expected, search("quiero ver el real madrid"))
    }

    @Test
    fun `la uno y la 1 dan lo mismo, y no se cuela La 10`() = runTest {
        assertEquals(listOf("La 1"), search("La 1"))
        assertEquals(listOf("La 1"), search("la uno"))
        assertEquals(listOf("La 1"), search("quiero ver la uno"))
    }

    @Test
    fun `antena tres y antena 3 dan lo mismo`() = runTest {
        assertEquals(listOf("Antena 3"), search("antena tres"))
        assertEquals(listOf("Antena 3"), search("Antena 3"))
        assertEquals(listOf("Antena 3"), search("quiero ver antena tres"))
    }

    @Test
    fun `funciona tambien cuando el canal esta escrito en letra`() = runTest {
        assertEquals(listOf("Cuatro"), search("cuatro"))
        assertEquals(listOf("Cuatro"), search("4"))
        assertEquals(listOf("Canal Dos Andalucía"), search("canal 2 andalucia"))
    }

    @Test
    fun `una busqueda vacia no devuelve nada`() = runTest {
        assertTrue(search("   ").isEmpty())
        assertTrue(search("¿?").isEmpty())
    }
}
