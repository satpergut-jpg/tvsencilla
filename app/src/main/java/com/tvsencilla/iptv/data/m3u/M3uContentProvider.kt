package com.tvsencilla.iptv.data.m3u

import com.tvsencilla.iptv.data.net.StreamDownloader
import com.tvsencilla.iptv.data.xmltv.XmltvParser
import com.tvsencilla.iptv.domain.model.Category
import com.tvsencilla.iptv.domain.model.Channel
import com.tvsencilla.iptv.domain.model.ContentSource
import com.tvsencilla.iptv.domain.model.EpgProgram
import com.tvsencilla.iptv.domain.model.Movie
import com.tvsencilla.iptv.domain.model.ProviderCapabilities
import com.tvsencilla.iptv.domain.model.Series
import com.tvsencilla.iptv.domain.model.SourceType
import com.tvsencilla.iptv.domain.provider.ContentProvider
import com.tvsencilla.iptv.domain.provider.LiveCatalog
import com.tvsencilla.iptv.domain.provider.ProviderException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A plain M3U playlist plus an optional XMLTV guide. Playlists carry live channels only; video on
 * demand needs a provider API, so Movies and Series stay hidden for this source type.
 */
@Singleton
class M3uContentProvider @Inject constructor(
    private val downloader: StreamDownloader,
    private val playlistParser: M3uParser,
    private val guideParser: XmltvParser,
) : ContentProvider {

    override val handles: SourceType = SourceType.M3U

    override suspend fun authenticate(source: ContentSource): ProviderCapabilities {
        val entries = loadEntries(source)
        if (entries.isEmpty()) throw ProviderException(ProviderException.Reason.EMPTY)
        return ProviderCapabilities(
            supportsLive = true,
            supportsMovies = false,
            supportsSeries = false,
            supportsEpg = !source.epgUrl.isNullOrBlank(),
            supportsCatchUp = entries.any { it.supportsCatchUp },
        )
    }

    override suspend fun fetchLive(source: ContentSource): LiveCatalog {
        val channels = loadEntries(source).mapIndexed { index, entry ->
            Channel(
                id = entry.channelId(),
                name = entry.name,
                streamUrl = entry.url,
                logoUrl = entry.logoUrl,
                categoryId = entry.groupTitle?.toCategoryId(),
                categoryName = entry.groupTitle,
                epgChannelId = entry.tvgId,
                listNumber = index + 1,
                supportsCatchUp = entry.supportsCatchUp,
            )
        }
        val categories = channels
            .mapNotNull { it.categoryName }
            .distinct()
            .sorted()
            .map { Category(id = it.toCategoryId(), name = it) }
        return LiveCatalog(channels = channels, categories = categories)
    }

    override suspend fun fetchMovies(source: ContentSource): List<Movie> = emptyList()

    override suspend fun fetchSeries(source: ContentSource): List<Series> = emptyList()

    override suspend fun fetchSeriesDetail(source: ContentSource, seriesId: String): Series? = null

    override suspend fun fetchEpg(source: ContentSource, onProgram: (EpgProgram) -> Unit) {
        val url = source.epgUrl?.takeIf { it.isNotBlank() } ?: return
        downloader.readStream(url) { stream -> guideParser.parse(stream, onProgram) }
    }

    override fun catchUpUrl(source: ContentSource, channel: Channel, program: EpgProgram): String? {
        if (!channel.supportsCatchUp) return null
        val separator = if (channel.streamUrl.contains('?')) "&" else "?"
        val startSeconds = program.startMillis / 1000
        val nowSeconds = System.currentTimeMillis() / 1000
        return "${channel.streamUrl}${separator}utc=$startSeconds&lutc=$nowSeconds"
    }

    private suspend fun loadEntries(source: ContentSource): List<M3uEntry> {
        val url = source.m3uUrl?.takeIf { it.isNotBlank() }
            ?: throw ProviderException(ProviderException.Reason.BAD_CREDENTIALS)
        return downloader.readLines(url) { lines ->
            playlistParser.parse(lines).filter { it.name.isNotBlank() && it.url.isNotBlank() }.toList()
        }
    }

    /**
     * The playlist has no stable ids, so one is derived from the tvg-id or, failing that, the
     * stream URL. This is what keeps favourites attached to the right channel across refreshes.
     */
    private fun M3uEntry.channelId(): String {
        val seed = tvgId?.takeIf { it.isNotBlank() } ?: url
        return "m3u:${seed.hashCode().toUInt().toString(16)}"
    }

    private fun String.toCategoryId(): String = "grp:${trim().lowercase()}"
}
