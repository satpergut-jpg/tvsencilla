package com.tvsencilla.iptv.domain.favorites

import org.junit.Assert.assertEquals
import org.junit.Test

class FavoritesOrderTest {

    private val order = listOf("a", "b", "c", "d")

    @Test
    fun `moving up swaps a channel with the one above it`() {
        assertEquals(listOf("a", "c", "b", "d"), FavoritesOrder.moveUp(order, "c"))
    }

    @Test
    fun `moving down swaps a channel with the one below it`() {
        assertEquals(listOf("b", "a", "c", "d"), FavoritesOrder.moveDown(order, "a"))
    }

    @Test
    fun `the first channel cannot move up and the last cannot move down`() {
        assertEquals(order, FavoritesOrder.moveUp(order, "a"))
        assertEquals(order, FavoritesOrder.moveDown(order, "d"))
    }

    @Test
    fun `an unknown channel leaves the order untouched`() {
        assertEquals(order, FavoritesOrder.moveUp(order, "zz"))
        assertEquals(order, FavoritesOrder.setPosition(order, "zz", 1))
    }

    @Test
    fun `setPosition uses the number the user typed, starting at one`() {
        assertEquals(listOf("d", "a", "b", "c"), FavoritesOrder.setPosition(order, "d", 1))
        assertEquals(listOf("b", "c", "a", "d"), FavoritesOrder.setPosition(order, "a", 3))
    }

    @Test
    fun `setPosition beyond the ends is clamped instead of failing`() {
        assertEquals(listOf("b", "c", "d", "a"), FavoritesOrder.setPosition(order, "a", 99))
        assertEquals(listOf("d", "a", "b", "c"), FavoritesOrder.setPosition(order, "d", 0))
    }

    @Test
    fun `new favourites go to the end so existing numbers do not shift`() {
        assertEquals(listOf("a", "b", "c", "d", "e"), FavoritesOrder.add(order, "e"))
    }

    @Test
    fun `adding a channel that is already a favourite changes nothing`() {
        assertEquals(order, FavoritesOrder.add(order, "b"))
    }

    @Test
    fun `removing a favourite closes the gap it leaves`() {
        val without = FavoritesOrder.remove(order, "b")

        assertEquals(listOf("a", "c", "d"), without)
        assertEquals(mapOf("a" to 1, "c" to 2, "d" to 3), FavoritesOrder.numbering(without))
    }

    @Test
    fun `numbering is one based and follows the order exactly`() {
        assertEquals(
            mapOf("a" to 1, "b" to 2, "c" to 3, "d" to 4),
            FavoritesOrder.numbering(order),
        )
    }

    @Test
    fun `moving a channel renumbers everything it passed`() {
        val moved = FavoritesOrder.setPosition(order, "d", 2)

        assertEquals(listOf("a", "d", "b", "c"), moved)
        assertEquals(2, FavoritesOrder.numbering(moved)["d"])
    }
}
