package com.tvsencilla.iptv.domain.search

import java.text.Normalizer

/**
 * El buscador de toda la app: canales, películas, series y la guía de programación.
 *
 * Tiene que entender igual lo que se dice en voz alta que lo que se teclea con prisa, así que:
 *  - ignora mayúsculas, tildes y palabras de relleno ("quiero ver el…"),
 *  - trata los números en cifra y en letra como lo mismo: "la uno" es "La 1", "antena tres" es
 *    "Antena 3", y al revés, porque el reconocimiento de voz escribe unas veces de una forma y
 *    otras de la otra,
 *  - prueba la frase exacta, para que un nombre corto como "La 1" funcione,
 *  - y busca por palabras clave, todas obligatorias, tanto separadas como escritas juntas.
 */
object TextSearch {

    /** Palabras que se dicen al pedir algo pero que no sirven para encontrarlo. */
    private val STOPWORDS = setOf(
        "el", "la", "los", "las", "lo", "de", "del", "al", "un", "una", "unos", "unas",
        "y", "o", "en", "con", "por", "para", "que", "quiero", "quisiera", "ver", "pon",
        "ponme", "poner", "busca", "buscar", "partido", "canal", "canales", "donde", "echan",
        "hoy", "ahora", "esta", "este", "hay", "dan", "sale", "juega",
    )

    /**
     * La parte de [STOPWORDS] que sirve para pedir, no para nombrar. Se quita incluso de la frase
     * exacta, para que "quiero ver la uno" busque "la 1". Los artículos no están aquí porque
     * forman parte de nombres como "La 1".
     */
    private val REQUEST_WORDS = setOf(
        "quiero", "quisiera", "ver", "pon", "ponme", "poner", "busca", "buscar",
        "donde", "echan", "hoy", "ahora", "hay", "dan", "sale",
    )

    /**
     * Números que aparecen en nombres de canales. "Un" y "una" no están a propósito: son artículos
     * casi siempre, y "La 1" se dice "la uno".
     */
    private val NUMBER_WORDS = mapOf(
        "cero" to "0", "uno" to "1", "dos" to "2", "tres" to "3", "cuatro" to "4",
        "cinco" to "5", "seis" to "6", "siete" to "7", "ocho" to "8", "nueve" to "9",
        "diez" to "10", "once" to "11", "doce" to "12", "trece" to "13", "catorce" to "14",
        "quince" to "15", "dieciseis" to "16", "diecisiete" to "17", "dieciocho" to "18",
        "diecinueve" to "19", "veinte" to "20", "veinticuatro" to "24",
    )
    private val DIGIT_WORDS = NUMBER_WORDS.entries.associate { (word, digits) -> digits to word }

    private val DIACRITICS = Regex("\\p{Mn}+")
    private val SEPARATORS = Regex("[^a-z0-9]+")
    private const val MIN_WORD_LENGTH = 3

    /** Los primeros caracteres bastan para el filtro de respaldo en la base de datos. */
    private const val PREFIX_LENGTH = 4

    /**
     * NFKD, no NFD: además de separar las tildes, convierte las letras en superíndice que usan
     * muchas listas ("Antena 3 ᵁᴴᴰ") en letras normales, para que "antena 3 uhd" las encuentre.
     */
    fun normalize(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFKD).replace(DIACRITICS, "").lowercase()

    /**
     * La forma con la que se compara todo: "¡Antena  Tres!" pasa a "antena 3". Sin tildes, sin
     * signos, un solo espacio y los números en cifra.
     */
    fun phrase(text: String): String =
        normalize(text).split(SEPARATORS)
            .filter { it.isNotEmpty() }
            .joinToString(" ") { NUMBER_WORDS[it] ?: it }

    /**
     * Lo que se ha pedido, sin la forma de pedirlo: "quiero ver la uno" pasa a "la 1". Si no queda
     * nada, se devuelve la frase entera.
     */
    fun requestedPhrase(query: String): String {
        val full = phrase(query)
        return full.split(' ').filterNot { it in REQUEST_WORDS }.joinToString(" ").ifEmpty { full }
    }

    /** La misma frase con los números en letra: "antena 3" pasa a "antena tres". */
    private fun spelledOut(phrase: String): String =
        phrase.split(' ').joinToString(" ") { DIGIT_WORDS[it] ?: it }

    fun keywords(query: String): List<String> {
        val tokens = phrase(query).split(' ').filter { it.isNotEmpty() }.distinct()
        val words = tokens.filter { it.length >= MIN_WORD_LENGTH }
        val meaningful = words.filterNot { it in STOPWORDS }
        // Si todo eran palabras de relleno ("el partido"), mejor buscar con ellas que no buscar,
        // salvo las de petición: "quiero" o "ver" nunca están en el título de nada.
        val chosen = meaningful.ifEmpty { words.filterNot { it in REQUEST_WORDS } }
        if (chosen.isEmpty()) return emptyList()

        // Un número solo ("1") encontraría cualquier cosa, así que únicamente cuenta cuando
        // acompaña a otra palabra: en "antena tres" hace que no salga también "Antena Nova".
        val numbers = tokens.filter { it.length < MIN_WORD_LENGTH && it.all(Char::isDigit) }
        return chosen + numbers
    }

    /**
     * Todas las palabras tienen que aparecer. Cada una vale tanto separada como pegada a las demás,
     * así que "realmadrid" encuentra "Real Madrid" y "real madrid" encuentra "RealMadrid TV".
     */
    fun matches(text: String, keywords: List<String>): Boolean {
        if (keywords.isEmpty()) return false
        val normalized = phrase(text)
        val compact = normalized.replace(" ", "")
        val padded = " $normalized "
        return keywords.all { keyword ->
            if (keyword.all(Char::isDigit)) {
                // Los números van enteros: "antena tres" no debe encontrar "Antena 30".
                padded.contains(" $keyword ")
            } else {
                normalized.contains(keyword) || compact.contains(keyword)
            }
        }
    }

    /**
     * Patrón LIKE para el primer filtro en la base de datos. SQLite no ignora tildes, así que cada
     * vocal pasa a ser comodín: "atletico" encuentra también "Atlético". El filtro exacto se hace
     * después, en Kotlin.
     */
    fun likePattern(fragment: String): String =
        "%" + fragment.map { if (it in "aeiou") '_' else it }.joinToString("") + "%"

    /**
     * Busca en la base de datos con [fetch], que recibe un patrón LIKE, y se queda con lo que
     * encaja de verdad según [text].
     *
     * Se juntan los resultados de todas las formas de buscar en lugar de parar en la primera que
     * encuentre algo: así "real madrid", "realmadrid" y "quiero ver el Real Madrid" devuelven lo
     * mismo. El orden lo decide quien llama.
     */
    suspend fun <T> find(
        query: String,
        fetch: suspend (likePattern: String) -> List<T>,
        text: (T) -> String,
    ): List<T> {
        val exact = requestedPhrase(query)
        if (exact.isEmpty()) return emptyList()
        val found = LinkedHashSet<T>()

        // 1. La frase tal cual, con palabras completas: "La 1" encuentra "La 1 HD" pero no
        //    "La 10". En la base de datos el canal puede estar escrito con cifra o con letra, así
        //    que se buscan las dos formas.
        for (form in listOf(exact, spelledOut(exact)).distinct()) {
            fetch(likePattern(form)).filterTo(found) { " ${phrase(text(it))} ".contains(" $exact ") }
        }

        // 2. Las palabras clave: "quiero ver el Real Madrid" -> real, madrid. El filtro de la base
        //    de datos usa la palabra más larga, nunca un número, para no depender de si el título
        //    lo escribe en cifra o en letra.
        val keywords = keywords(query)
        if (keywords.isEmpty()) return found.toList()
        val longest = keywords.filter { it.length >= MIN_WORD_LENGTH }.maxBy { it.length }
        fetch(likePattern(longest)).filterTo(found) { matches(text(it), keywords) }

        // 3. Solo el comienzo de la palabra, para lo escrito todo junto: "realmadrid" no aparece
        //    tal cual en "Real Madrid", pero "real" sí.
        if (longest.length > PREFIX_LENGTH) {
            fetch(likePattern(longest.take(PREFIX_LENGTH))).filterTo(found) { matches(text(it), keywords) }
        }
        return found.toList()
    }
}
