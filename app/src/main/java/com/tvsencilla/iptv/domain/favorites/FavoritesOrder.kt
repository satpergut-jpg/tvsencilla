package com.tvsencilla.iptv.domain.favorites

/**
 * Pure list arithmetic behind the "Ordenar favoritos" screen. Positions the user sees are
 * 1-based; the stored order is the index in the returned list.
 */
object FavoritesOrder {

    fun move(ids: List<String>, fromIndex: Int, toIndex: Int): List<String> {
        if (fromIndex !in ids.indices) return ids
        val target = toIndex.coerceIn(0, ids.lastIndex)
        if (target == fromIndex) return ids
        return ids.toMutableList().apply { add(target, removeAt(fromIndex)) }
    }

    fun moveUp(ids: List<String>, id: String): List<String> {
        val index = ids.indexOf(id)
        return if (index <= 0) ids else move(ids, index, index - 1)
    }

    fun moveDown(ids: List<String>, id: String): List<String> {
        val index = ids.indexOf(id)
        return if (index < 0 || index == ids.lastIndex) ids else move(ids, index, index + 1)
    }

    /** "Poner en el número…": [position] is what the user typed, so it starts at 1. */
    fun setPosition(ids: List<String>, id: String, position: Int): List<String> {
        val index = ids.indexOf(id)
        if (index < 0) return ids
        return move(ids, index, position - 1)
    }

    /** New favourites go to the end, so existing numbers never shift under the user. */
    fun add(ids: List<String>, id: String): List<String> =
        if (ids.contains(id)) ids else ids + id

    fun remove(ids: List<String>, id: String): List<String> = ids.filterNot { it == id }

    /** The favourite numbers the user sees, derived from the order alone. */
    fun numbering(ids: List<String>): Map<String, Int> =
        ids.withIndex().associate { (index, id) -> id to index + 1 }
}
