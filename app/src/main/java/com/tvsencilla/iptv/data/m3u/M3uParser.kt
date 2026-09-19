package com.tvsencilla.iptv.data.m3u

import javax.inject.Inject

data class M3uEntry(
    val name: String,
    val url: String,
    val tvgId: String? = null,
    val logoUrl: String? = null,
    val groupTitle: String? = null,
    val catchUpDays: Int? = null,
) {
    val supportsCatchUp: Boolean get() = (catchUpDays ?: 0) > 0
}

/**
 * Parses an M3U playlist lazily. Playlists with tens of thousands of channels are common, so the
 * input is a [Sequence] of lines and entries are yielded as they are completed rather than
 * collected into a list.
 */
class M3uParser @Inject constructor() {

    fun parse(lines: Sequence<String>): Sequence<M3uEntry> = sequence {
        var header: ExtInf? = null
        var groupFromExtGrp: String? = null

        for (raw in lines) {
            val line = raw.trim()
            if (line.isEmpty()) continue

            when {
                line.startsWith(EXTINF, ignoreCase = true) -> {
                    header = parseExtInf(line.substring(EXTINF.length))
                    groupFromExtGrp = null
                }

                line.startsWith(EXTGRP, ignoreCase = true) -> {
                    groupFromExtGrp = line.substring(EXTGRP.length).trim().ifEmpty { null }
                }

                // #EXTM3U, #EXTVLCOPT, #KODIPROP and plain comments carry nothing we need.
                line.startsWith("#") -> Unit

                else -> {
                    val current = header ?: continue
                    header = null
                    val group = current.attributes["group-title"]?.ifEmpty { null } ?: groupFromExtGrp
                    groupFromExtGrp = null
                    yield(
                        M3uEntry(
                            name = current.name,
                            url = line,
                            tvgId = current.attributes["tvg-id"]?.ifEmpty { null },
                            logoUrl = current.attributes["tvg-logo"]?.ifEmpty { null },
                            groupTitle = group,
                            catchUpDays = current.catchUpDays(),
                        ),
                    )
                }
            }
        }
    }

    private class ExtInf(val name: String, val attributes: Map<String, String>) {
        /**
         * Providers advertise catch-up in several dialects; any of them switching it on is enough
         * to show the option, and its absence keeps the option hidden.
         */
        fun catchUpDays(): Int? {
            val days = attributes["catchup-days"] ?: attributes["timeshift"] ?: attributes["tvg-rec"]
            days?.trim()?.toIntOrNull()?.let { if (it > 0) return it }
            val flag = attributes["catchup"]?.trim()?.lowercase()
            return if (flag != null && flag != "0" && flag != "false") DEFAULT_CATCHUP_DAYS else null
        }
    }

    private fun parseExtInf(body: String): ExtInf {
        val separator = indexOfUnquotedComma(body)
        val header = if (separator >= 0) body.substring(0, separator) else body
        val name = if (separator >= 0) body.substring(separator + 1).trim() else ""
        val attributes = ATTRIBUTE.findAll(header)
            .associate { it.groupValues[1].lowercase() to it.groupValues[2] }
        val displayName = name.ifEmpty { attributes["tvg-name"].orEmpty() }
        return ExtInf(name = displayName, attributes = attributes)
    }

    /** Attribute values may themselves contain commas, so quotes have to be respected. */
    private fun indexOfUnquotedComma(value: String): Int {
        var inQuotes = false
        value.forEachIndexed { index, char ->
            when (char) {
                '"' -> inQuotes = !inQuotes
                ',' -> if (!inQuotes) return index
            }
        }
        return -1
    }

    private companion object {
        const val EXTINF = "#EXTINF:"
        const val EXTGRP = "#EXTGRP:"
        const val DEFAULT_CATCHUP_DAYS = 7
        val ATTRIBUTE = Regex("""([A-Za-z0-9_-]+)\s*=\s*"([^"]*)"""")
    }
}
