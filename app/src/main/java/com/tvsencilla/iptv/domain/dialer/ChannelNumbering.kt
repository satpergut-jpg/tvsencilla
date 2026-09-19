package com.tvsencilla.iptv.domain.dialer

import com.tvsencilla.iptv.domain.model.Channel
import com.tvsencilla.iptv.domain.model.NumberingMode

/**
 * The numbering the remote follows: either the user's favourites (1, 2, 3…) or the provider's
 * full list. Only one is ever active, so a typed number can never be ambiguous.
 */
class ChannelNumbering(
    private val ordered: List<Channel>,
    private val numberOf: (Channel) -> Int,
) {

    private val byNumber: Map<Int, Channel> = ordered.associateBy(numberOf)

    val size: Int get() = ordered.size

    val isEmpty: Boolean get() = ordered.isEmpty()

    fun channelAt(number: Int): Channel? = byNumber[number]

    fun numberFor(channel: Channel): Int? =
        if (byNumber[numberOf(channel)]?.id == channel.id) numberOf(channel) else null

    /** CH+ wraps round to the first channel. */
    fun next(currentId: String?): Channel? = step(currentId, +1)

    /** CH- wraps round to the last channel. */
    fun previous(currentId: String?): Channel? = step(currentId, -1)

    private fun step(currentId: String?, delta: Int): Channel? {
        if (ordered.isEmpty()) return null
        val index = ordered.indexOfFirst { it.id == currentId }
        if (index < 0) return ordered.first()
        val size = ordered.size
        return ordered[((index + delta) % size + size) % size]
    }

    companion object {
        /**
         * La numeración que manda en el mando. Con el modo Favoritos pero sin ningún favorito
         * todavía, se usa la lista completa: si no, CH+/CH−, arriba/abajo y los números no harían
         * nada, y alguien que estrena la app no podría ni cambiar de canal.
         */
        fun forMode(mode: NumberingMode, all: List<Channel>, favorites: List<Channel>): ChannelNumbering =
            if (mode == NumberingMode.FAVORITES && favorites.isNotEmpty()) {
                forFavorites(favorites)
            } else {
                forFullList(all)
            }

        fun forFavorites(favorites: List<Channel>): ChannelNumbering {
            val ordered = favorites
                .filter { it.favoriteNumber != null }
                .sortedBy { it.favoriteNumber }
            return ChannelNumbering(ordered) { it.favoriteNumber ?: 0 }
        }

        fun forFullList(channels: List<Channel>): ChannelNumbering {
            val ordered = channels.sortedBy { it.listNumber }
            return ChannelNumbering(ordered) { it.listNumber }
        }
    }
}
