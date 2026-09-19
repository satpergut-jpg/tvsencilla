package com.tvsencilla.iptv.domain.provider

import com.tvsencilla.iptv.domain.model.Category
import com.tvsencilla.iptv.domain.model.Channel
import com.tvsencilla.iptv.domain.model.ContentSource
import com.tvsencilla.iptv.domain.model.EpgProgram
import com.tvsencilla.iptv.domain.model.Movie
import com.tvsencilla.iptv.domain.model.ProviderCapabilities
import com.tvsencilla.iptv.domain.model.Series
import com.tvsencilla.iptv.domain.model.SourceType

/**
 * A source of watchable content. Implemented once per protocol (M3U, Xtream, …) so a new
 * provider can be added without touching the UI or the database.
 */
interface ContentProvider {

    val handles: SourceType

    /** Verifies the user's entitlement and reports what this source can actually do. */
    suspend fun authenticate(source: ContentSource): ProviderCapabilities

    /**
     * Channels and their categories in a single pass. They come together because an M3U playlist
     * only reveals its groups while being parsed, and downloading it twice is not acceptable on a
     * low-end TV box.
     */
    suspend fun fetchLive(source: ContentSource): LiveCatalog

    suspend fun fetchMovies(source: ContentSource): List<Movie>

    suspend fun fetchSeries(source: ContentSource): List<Series>

    /** Seasons and episodes are usually a second request, so they are fetched on demand. */
    suspend fun fetchSeriesDetail(source: ContentSource, seriesId: String): Series?

    /**
     * Streams the EPG, handing each programme to [onProgram] as it is parsed. XMLTV guides can be
     * hundreds of megabytes, so nothing is accumulated in memory here.
     */
    suspend fun fetchEpg(source: ContentSource, onProgram: (EpgProgram) -> Unit)

    /** Null when the source has no catch-up for this programme, so the option stays hidden. */
    fun catchUpUrl(source: ContentSource, channel: Channel, program: EpgProgram): String?
}

data class LiveCatalog(
    val channels: List<Channel>,
    val categories: List<Category>,
)

class ProviderException(
    val reason: Reason,
    cause: Throwable? = null,
) : Exception(reason.name, cause) {
    enum class Reason { NO_NETWORK, UNREACHABLE, BAD_CREDENTIALS, EMPTY, UNKNOWN }
}
