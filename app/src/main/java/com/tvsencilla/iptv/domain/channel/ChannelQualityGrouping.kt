package com.tvsencilla.iptv.domain.channel

import com.tvsencilla.iptv.domain.model.Channel
import com.tvsencilla.iptv.domain.model.ChannelQuality
import com.tvsencilla.iptv.domain.search.TextSearch

/**
 * Une en una sola entrada las versiones de un canal que solo cambian de calidad ("Antena 3 HD",
 * "Antena 3 FHD", "Antena 3 SD"): en la lista aparecen una sola vez y dentro se puede cambiar de
 * calidad. Se deja puesta por defecto la versión HD cuando la hay; si no, la mejor disponible.
 */
object ChannelQualityGrouping {

    fun group(channels: List<Channel>): List<Channel> =
        channels
            .groupBy { GroupKey(it.categoryId, coreName(it.name)) }
            .values
            .map(::merge)
            .sortedBy { it.listNumber }
            .mapIndexed { index, channel -> channel.copy(listNumber = index + 1) }

    private data class GroupKey(val categoryId: String?, val coreName: String)

    private fun merge(variants: List<Channel>): Channel {
        if (variants.size == 1) return variants.single().let { it.copy(qualityOptions = emptyList()) }

        val ordered = variants.sortedWith(compareBy({ qualityPriority(qualityTag(it.name)) }, { it.listNumber }))
        val primary = ordered.first()
        val options = ordered.map { ChannelQuality(label = qualityLabel(it.name), streamUrl = it.streamUrl) }
        return primary.copy(
            listNumber = variants.minOf { it.listNumber },
            qualityOptions = options,
        )
    }

    /** HD por defecto; SD al final; lo que no lleve etiqueta se trata como HD. */
    private fun qualityPriority(tag: String?): Int = when (tag) {
        null, "hd" -> 0
        "fhd" -> 1
        "uhd", "4k" -> 1
        "sd" -> 2
        else -> 1
    }

    private fun qualityLabel(name: String): String = qualityTag(name)?.uppercase() ?: "HD"

    private fun qualityTag(name: String): String? =
        TextSearch.phrase(name).split(' ').lastOrNull { it in QUALITY_TAGS }

    /** El nombre sin la etiqueta de calidad al final ("Antena 3 FHD" -> "antena 3"). */
    private fun coreName(name: String): String {
        val tokens = TextSearch.phrase(name).split(' ').toMutableList()
        while (tokens.size > 1 && tokens.last() in QUALITY_TAGS) tokens.removeAt(tokens.lastIndex)
        return tokens.joinToString(" ")
    }

    private val QUALITY_TAGS = setOf("hd", "fhd", "uhd", "4k", "sd")
}
