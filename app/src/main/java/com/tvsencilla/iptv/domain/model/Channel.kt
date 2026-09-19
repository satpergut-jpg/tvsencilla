package com.tvsencilla.iptv.domain.model

data class Channel(
    val id: String,
    val name: String,
    val streamUrl: String,
    val logoUrl: String? = null,
    val categoryId: String? = null,
    val categoryName: String? = null,
    val epgChannelId: String? = null,
    /** Position in the provider's full channel list, starting at 1. */
    val listNumber: Int = 0,
    /** Position within the user's favourites, starting at 1. Null when not a favourite. */
    val favoriteNumber: Int? = null,
    val supportsCatchUp: Boolean = false,
) {
    val isFavorite: Boolean get() = favoriteNumber != null
}

data class Category(
    val id: String,
    val name: String,
    val channelCount: Int = 0,
)
