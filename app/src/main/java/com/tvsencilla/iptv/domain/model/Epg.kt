package com.tvsencilla.iptv.domain.model

data class EpgProgram(
    val epgChannelId: String,
    val title: String,
    val startMillis: Long,
    val endMillis: Long,
    val description: String? = null,
) {
    fun isLiveAt(nowMillis: Long): Boolean = nowMillis in startMillis until endMillis

    fun progressAt(nowMillis: Long): Float {
        val span = endMillis - startMillis
        if (span <= 0L) return 0f
        return ((nowMillis - startMillis).toFloat() / span).coerceIn(0f, 1f)
    }
}

/** Un programa de la guía que coincide con una búsqueda, junto al canal que lo emite. */
data class ProgramMatch(
    val channel: Channel,
    val program: EpgProgram,
)

/** The simplified guide shown to the user: only what is on now and what comes next. */
data class NowNext(
    val now: EpgProgram? = null,
    val next: EpgProgram? = null,
)
