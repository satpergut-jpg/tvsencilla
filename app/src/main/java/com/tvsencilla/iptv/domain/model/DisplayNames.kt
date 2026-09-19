package com.tvsencilla.iptv.domain.model

/**
 * Las listas IPTV suelen anteponer el país a cada nombre ("|ES| La 1", "ES: Antena 3", "[ES] 24h").
 * En pantalla eso solo ocupa el sitio del nombre de verdad, así que se quita para mostrarlo.
 *
 * Solo se reconocen dos letras mayúsculas seguidas de un separador, para no recortar nombres
 * legítimos como "TV3" o "CMM".
 */
private val COUNTRY_PREFIX = Regex("""^\s*[|\[(]?\s*[A-Z]{2}\s*[|\]):]\s*""")

fun displayName(rawName: String): String {
    var name = rawName
    // Algunas listas repiten la etiqueta: "|ES| |ES| La 1".
    repeat(2) { name = name.replace(COUNTRY_PREFIX, "") }
    return name.trim().ifEmpty { rawName.trim() }
}

val Channel.displayName: String get() = displayName(name)
