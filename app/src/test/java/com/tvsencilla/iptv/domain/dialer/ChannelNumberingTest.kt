package com.tvsencilla.iptv.domain.dialer

import com.tvsencilla.iptv.domain.model.Channel
import com.tvsencilla.iptv.domain.model.NumberingMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChannelNumberingTest {

    private fun channel(id: String, listNumber: Int = 0, favoriteNumber: Int? = null) = Channel(
        id = id,
        name = "Canal $id",
        streamUrl = "http://example.test/$id",
        listNumber = listNumber,
        favoriteNumber = favoriteNumber,
    )

    private val favorites = listOf(
        channel("c", favoriteNumber = 1),
        channel("a", favoriteNumber = 2),
        channel("b", favoriteNumber = 3),
    )

    @Test
    fun `favourites answer to their own numbers, not the provider's`() {
        val numbering = ChannelNumbering.forFavorites(favorites)

        assertEquals("c", numbering.channelAt(1)?.id)
        assertEquals("a", numbering.channelAt(2)?.id)
        assertEquals("b", numbering.channelAt(3)?.id)
    }

    @Test
    fun `a number with no channel behind it resolves to nothing`() {
        val numbering = ChannelNumbering.forFavorites(favorites)
        assertNull(numbering.channelAt(25))
    }

    @Test
    fun `channel up wraps round to the first favourite`() {
        val numbering = ChannelNumbering.forFavorites(favorites)

        assertEquals("a", numbering.next("c")?.id)
        assertEquals("b", numbering.next("a")?.id)
        assertEquals("c", numbering.next("b")?.id)
    }

    @Test
    fun `channel down wraps round to the last favourite`() {
        val numbering = ChannelNumbering.forFavorites(favorites)

        assertEquals("b", numbering.previous("c")?.id)
        assertEquals("c", numbering.previous("a")?.id)
    }

    @Test
    fun `an unknown current channel lands on the first one`() {
        val numbering = ChannelNumbering.forFavorites(favorites)
        assertEquals("c", numbering.next("not-in-the-list")?.id)
    }

    @Test
    fun `the full list uses the provider numbers, gaps and all`() {
        val numbering = ChannelNumbering.forFullList(
            listOf(
                channel("x", listNumber = 101),
                channel("y", listNumber = 7),
            ),
        )

        assertEquals("y", numbering.channelAt(7)?.id)
        assertEquals("x", numbering.channelAt(101)?.id)
        assertNull(numbering.channelAt(1))
        // Sorted by number, so CH+ from 7 reaches 101.
        assertEquals("x", numbering.next("y")?.id)
    }

    @Test
    fun `an empty numbering never returns a channel`() {
        val numbering = ChannelNumbering.forFavorites(emptyList())

        assertTrue(numbering.isEmpty)
        assertNull(numbering.channelAt(1))
        assertNull(numbering.next(null))
        assertNull(numbering.previous(null))
    }

    @Test
    fun `sin favoritos el mando usa la lista completa para poder cambiar de canal`() {
        val all = listOf(channel("x", listNumber = 1), channel("y", listNumber = 2))

        val numbering = ChannelNumbering.forMode(NumberingMode.FAVORITES, all, favorites = emptyList())

        assertEquals("y", numbering.next("x")?.id)
        assertEquals("x", numbering.channelAt(1)?.id)
    }

    @Test
    fun `con favoritos el modo favoritos usa los favoritos`() {
        val all = listOf(channel("x", listNumber = 1), channel("c", listNumber = 9))

        val numbering = ChannelNumbering.forMode(NumberingMode.FAVORITES, all, favorites)

        assertEquals("c", numbering.channelAt(1)?.id)
    }

    @Test
    fun `numberFor reports the number the remote responds to`() {
        val numbering = ChannelNumbering.forFavorites(favorites)

        assertEquals(2, numbering.numberFor(favorites[1]))
        assertNull(numbering.numberFor(channel("zz", favoriteNumber = null)))
    }
}
