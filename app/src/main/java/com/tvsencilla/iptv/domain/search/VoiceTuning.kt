package com.tvsencilla.iptv.domain.search

import com.tvsencilla.iptv.domain.model.Channel
import com.tvsencilla.iptv.domain.model.ProgramMatch

/**
 * Decide si lo que se ha pedido por voz señala un único canal, para ponerlo directamente.
 *
 * La regla es no adivinar: cambiar a un canal equivocado confunde más que enseñar una lista. Por
 * eso solo se elige canal cuando está claro, y en caso de duda se devuelve null.
 */
object VoiceTuning {

    /**
     * Por orden de prioridad:
     *  1. un canal que se llame como lo pedido ("antena tres" -> Antena 3),
     *  2. lo que se emite ahora ("el Real Madrid" -> el canal que da el partido), que va antes que
     *     un canal cuyo nombre solo se parezca, como "RealMadrid TV",
     *  3. el único canal que haya salido en la búsqueda.
     */
    fun pick(
        query: String,
        channels: List<Channel>,
        programs: List<ProgramMatch>,
        nowMillis: Long,
    ): Channel? =
        pickByName(query, channels)
            ?: pickFromGuide(programs, nowMillis)
            ?: channels.singleOrNull()

    /**
     * Vale cuando todos los canales encontrados son el canal pedido o versiones suyas ("Antena 3",
     * "Antena 3 HD", "ES: Antena 3 FHD"). De esas se elige la que el usuario tiene en favoritos o,
     * si no, la de número más bajo. Si aparece un canal distinto ("Antena Nova" al pedir "antena"),
     * la petición es ambigua.
     */
    private fun pickByName(query: String, channels: List<Channel>): Channel? {
        if (channels.isEmpty()) return null
        val asked = TextSearch.requestedPhrase(query)
        val allAreVersions = channels.all { coreName(it.name) == asked }
        return if (allAreVersions) preferred(channels) else null
    }

    /**
     * El nombre sin las etiquetas con las que las listas IPTV distinguen versiones del mismo
     * canal: calidad al final ("HD", "4K") y país al principio ("ES:", "ESP |").
     */
    private fun coreName(name: String): String {
        val tokens = TextSearch.phrase(name).split(' ').toMutableList()
        while (tokens.size > 1 && tokens.first() in VERSION_TAGS) tokens.removeAt(0)
        while (tokens.size > 1 && tokens.last() in VERSION_TAGS) tokens.removeAt(tokens.lastIndex)
        return tokens.joinToString(" ")
    }

    private val VERSION_TAGS = setOf(
        "hd", "fhd", "uhd", "sd", "4k", "8k", "hq", "lq", "hevc", "h264", "h265",
        "720", "720p", "1080", "1080p", "full", "backup", "alt", "es", "esp", "spain", "espana",
        "dvb", "ultra", "raw", "multi",
    )

    /**
     * Por la guía: "el Real Madrid" se pone solo si ahora mismo lo emite un único canal, o si entre
     * los que lo emiten solo uno es de los favoritos del usuario.
     */
    private fun pickFromGuide(programs: List<ProgramMatch>, nowMillis: Long): Channel? {
        val live = programs
            .filter { it.program.isLiveAt(nowMillis) }
            .map { it.channel }
            .distinctBy { it.id }
        if (live.size == 1) return live.single()
        return live.filter { it.isFavorite }.singleOrNull()
    }

    private fun preferred(channels: List<Channel>): Channel? =
        channels.filter { it.isFavorite }.minByOrNull { it.favoriteNumber ?: Int.MAX_VALUE }
            ?: channels.minByOrNull { it.listNumber }
}
