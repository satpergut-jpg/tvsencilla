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
    /**
     * Other quality versions of this same channel ("Antena 3 HD", "Antena 3 FHD", "Antena 3 SD")
     * merged into this single entry by [com.tvsencilla.iptv.domain.channel.ChannelQualityGrouping].
     * Empty when the provider only had one version. [streamUrl] always matches the first entry.
     */
    val qualityOptions: List<ChannelQuality> = emptyList(),
) {
    val isFavorite: Boolean get() = favoriteNumber != null
    val hasQualityOptions: Boolean get() = qualityOptions.size > 1
}

/** One playable version of a [Channel] at a given quality, e.g. "HD" -> its stream URL. */
data class ChannelQuality(
    val label: String,
    val streamUrl: String,
)

data class Category(
    val id: String,
    val name: String,
    val channelCount: Int = 0,
)
