package com.tvsencilla.iptv.data.xtream

import com.tvsencilla.iptv.data.net.StreamDownloader
import com.tvsencilla.iptv.data.net.toProviderException
import com.tvsencilla.iptv.data.xmltv.XmltvParser
import com.tvsencilla.iptv.data.xtream.dto.XtreamCategoryDto
import com.tvsencilla.iptv.data.xtream.dto.XtreamEpisodeDto
import com.tvsencilla.iptv.data.xtream.dto.XtreamUserInfoDto
import com.tvsencilla.iptv.domain.model.Category
import com.tvsencilla.iptv.domain.model.Channel
import com.tvsencilla.iptv.domain.model.ContentSource
import com.tvsencilla.iptv.domain.model.EpgProgram
import com.tvsencilla.iptv.domain.model.Episode
import com.tvsencilla.iptv.domain.model.Movie
import com.tvsencilla.iptv.domain.model.ProviderCapabilities
import com.tvsencilla.iptv.domain.model.Season
import com.tvsencilla.iptv.domain.model.Series
import com.tvsencilla.iptv.domain.model.SourceType
import com.tvsencilla.iptv.domain.provider.ContentProvider
import com.tvsencilla.iptv.domain.provider.LiveCatalog
import com.tvsencilla.iptv.domain.provider.ProviderException
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** The Xtream Codes player API: live channels, video on demand, series and an XMLTV guide. */
@Singleton
class XtreamContentProvider @Inject constructor(
    private val api: XtreamApi,
    private val downloader: StreamDownloader,
    private val guideParser: XmltvParser,
) : ContentProvider {

    override val handles: SourceType = SourceType.XTREAM

    private val liveExtensionCache = java.util.concurrent.ConcurrentHashMap<String, String>()

    override suspend fun authenticate(source: ContentSource): ProviderCapabilities {
        val userInfo = handshake(source)
        if (!userInfo.isAuthenticated) throw ProviderException(ProviderException.Reason.BAD_CREDENTIALS)

        val catalog = fetchLive(source)
        if (catalog.channels.isEmpty()) throw ProviderException(ProviderException.Reason.EMPTY)

        // Se miran las categorías, no los catálogos completos: basta para saber si el panel sirve
        // películas o series, y son dos respuestas pequeñas en lugar de miles de fichas.
        // Un fallo aquí solo oculta el menú correspondiente.
        return ProviderCapabilities(
            supportsLive = true,
            supportsMovies = runCatching {
                call(source) { api.vodCategories(source.action(ACTION_VOD_CATEGORIES)) }.isNotEmpty()
            }.getOrDefault(false),
            supportsSeries = runCatching {
                call(source) { api.seriesCategories(source.action(ACTION_SERIES_CATEGORIES)) }.isNotEmpty()
            }.getOrDefault(false),
            supportsEpg = true,
            supportsCatchUp = catalog.channels.any { it.supportsCatchUp },
        )
    }

    override suspend fun fetchLive(source: ContentSource): LiveCatalog {
        val extension = liveExtension(source)
        val categories = call(source) { api.liveCategories(source.action(ACTION_LIVE_CATEGORIES)) }
            .toCategories()
        val categoryNames = categories.associate { it.id to it.name }

        val streams = call(source) { api.liveStreams(source.action(ACTION_LIVE_STREAMS)) }
        val channels = streams.mapIndexedNotNull { index, dto ->
            val streamId = dto.streamId?.takeIf { it.isNotBlank() } ?: return@mapIndexedNotNull null
            val name = dto.name?.trim()?.takeIf { it.isNotEmpty() } ?: return@mapIndexedNotNull null
            Channel(
                id = "$LIVE_ID_PREFIX$streamId",
                name = name,
                streamUrl = source.streamUrl("live", streamId, extension),
                logoUrl = dto.streamIcon?.takeIf { it.isNotBlank() },
                categoryId = dto.categoryId,
                categoryName = dto.categoryId?.let { categoryNames[it] },
                epgChannelId = dto.epgChannelId?.takeIf { it.isNotBlank() },
                // Panels number channels themselves; fall back to list order when they do not.
                listNumber = dto.num?.toIntOrNull() ?: (index + 1),
                supportsCatchUp = dto.hasArchive,
            )
        }
        return LiveCatalog(channels = channels, categories = categories)
    }

    override suspend fun fetchMovies(source: ContentSource): List<Movie> {
        val categoryNames = call(source) { api.vodCategories(source.action(ACTION_VOD_CATEGORIES)) }
            .toCategories()
            .associate { it.id to it.name }

        return call(source) { api.vodStreams(source.action(ACTION_VOD_STREAMS)) }
            .mapNotNull { dto ->
                val streamId = dto.streamId?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val title = dto.name?.trim()?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
                Movie(
                    id = "$MOVIE_ID_PREFIX$streamId",
                    title = title,
                    streamUrl = source.streamUrl(
                        "movie",
                        streamId,
                        dto.containerExtension?.takeIf { it.isNotBlank() } ?: DEFAULT_VOD_EXTENSION,
                    ),
                    posterUrl = dto.streamIcon?.takeIf { it.isNotBlank() },
                    year = dto.year?.takeIf { it.isNotBlank() } ?: dto.releaseDate?.take(4),
                    durationMinutes = dto.runTime?.toIntOrNull(),
                    plot = dto.plot?.takeIf { it.isNotBlank() },
                    categoryId = dto.categoryId,
                    categoryName = dto.categoryId?.let { categoryNames[it] },
                )
            }
    }

    override suspend fun fetchSeries(source: ContentSource): List<Series> {
        val categoryNames = call(source) { api.seriesCategories(source.action(ACTION_SERIES_CATEGORIES)) }
            .toCategories()
            .associate { it.id to it.name }

        return call(source) { api.seriesList(source.action(ACTION_SERIES)) }
            .mapNotNull { dto ->
                val seriesId = dto.seriesId?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val title = dto.name?.trim()?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
                Series(
                    id = "$SERIES_ID_PREFIX$seriesId",
                    title = title,
                    posterUrl = dto.cover?.takeIf { it.isNotBlank() },
                    year = dto.releaseDate?.take(4)?.takeIf { it.isNotBlank() },
                    plot = dto.plot?.takeIf { it.isNotBlank() },
                    categoryId = dto.categoryId,
                    categoryName = dto.categoryId?.let { categoryNames[it] },
                )
            }
    }

    override suspend fun fetchSeriesDetail(source: ContentSource, seriesId: String): Series? {
        val rawId = seriesId.removePrefix(SERIES_ID_PREFIX)
        val url = source.action(ACTION_SERIES_INFO) + "&series_id=" + rawId.encoded()
        val info = call(source) { api.seriesInfo(url) }

        val seasons = info.episodes.orEmpty()
            .mapNotNull { (seasonKey, episodes) ->
                val seasonNumber = seasonKey.toIntOrNull() ?: return@mapNotNull null
                Season(
                    number = seasonNumber,
                    episodes = episodes
                        .mapNotNull { it.toEpisode(source, seriesId, seasonNumber) }
                        .sortedBy { it.episodeNumber },
                )
            }
            .filter { it.episodes.isNotEmpty() }
            .sortedBy { it.number }

        val detail = info.info
        return Series(
            id = seriesId,
            title = detail?.name?.trim().orEmpty(),
            posterUrl = detail?.cover?.takeIf { it.isNotBlank() },
            year = detail?.releaseDate?.take(4)?.takeIf { it.isNotBlank() },
            plot = detail?.plot?.takeIf { it.isNotBlank() },
            categoryId = detail?.categoryId,
            seasons = seasons,
        )
    }

    override suspend fun fetchEpg(source: ContentSource, onProgram: (EpgProgram) -> Unit) {
        val host = source.requireHost()
        val url = "$host/xmltv.php?username=${source.requireUsername().encoded()}" +
            "&password=${source.requirePassword().encoded()}"
        downloader.readStream(url) { stream -> guideParser.parse(stream, onProgram) }
    }

    override fun catchUpUrl(source: ContentSource, channel: Channel, program: EpgProgram): String? {
        if (!channel.supportsCatchUp) return null
        val host = source.normalizedHost ?: return null
        val streamId = channel.id.removePrefix(LIVE_ID_PREFIX)
        val durationMinutes = ((program.endMillis - program.startMillis) / 60_000L).toInt()
        if (durationMinutes <= 0) return null
        val start = TIMESHIFT_FORMAT.format(Date(program.startMillis))
        return "$host/streaming/timeshift.php?username=${source.requireUsername().encoded()}" +
            "&password=${source.requirePassword().encoded()}" +
            "&stream=$streamId&start=$start&duration=$durationMinutes"
    }

    private suspend fun handshake(source: ContentSource): XtreamUserInfoDto {
        val response = call(source) { api.handshake(source.playerApi()) }
        return response.userInfo ?: throw ProviderException(ProviderException.Reason.BAD_CREDENTIALS)
    }

    /**
     * Panels that do not offer HLS need the raw transport stream instead. The answer is cached for
     * the session so refreshing the catalogue does not re-handshake on every call.
     */
    private suspend fun liveExtension(source: ContentSource): String {
        val host = source.requireHost()
        liveExtensionCache[host]?.let { return it }
        val formats = runCatching { handshake(source).allowedOutputFormats }.getOrNull().orEmpty()
        val extension =
            if (formats.isEmpty() || formats.any { it.equals("m3u8", ignoreCase = true) }) "m3u8" else "ts"
        liveExtensionCache[host] = extension
        return extension
    }

    private suspend fun <T> call(source: ContentSource, request: suspend () -> T): T {
        source.requireHost()
        return try {
            request()
        } catch (e: Exception) {
            throw e.toProviderException()
        }
    }

    private fun XtreamEpisodeDto.toEpisode(
        source: ContentSource,
        seriesId: String,
        seasonNumber: Int,
    ): Episode? {
        val episodeId = id?.takeIf { it.isNotBlank() } ?: return null
        val number = episodeNum?.toIntOrNull() ?: return null
        return Episode(
            id = "$EPISODE_ID_PREFIX$episodeId",
            seriesId = seriesId,
            seasonNumber = seasonNumber,
            episodeNumber = number,
            title = title?.trim()?.takeIf { it.isNotEmpty() } ?: "",
            streamUrl = source.streamUrl(
                "series",
                episodeId,
                containerExtension?.takeIf { it.isNotBlank() } ?: DEFAULT_VOD_EXTENSION,
            ),
            durationMinutes = info?.durationSecs?.toIntOrNull()?.let { it / 60 },
            plot = info?.plot?.takeIf { it.isNotBlank() },
            stillUrl = info?.movieImage?.takeIf { it.isNotBlank() },
        )
    }

    private fun List<XtreamCategoryDto>.toCategories(): List<Category> = mapNotNull { dto ->
        val id = dto.categoryId?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        val name = dto.categoryName?.trim()?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
        Category(id = id, name = name)
    }

    private fun ContentSource.playerApi(): String =
        "${requireHost()}/player_api.php?username=${requireUsername().encoded()}" +
            "&password=${requirePassword().encoded()}"

    private fun ContentSource.action(action: String): String = "${playerApi()}&action=$action"

    private fun ContentSource.streamUrl(kind: String, id: String, extension: String): String =
        "${requireHost()}/$kind/${requireUsername().encoded()}/${requirePassword().encoded()}/$id.$extension"

    private fun ContentSource.requireHost(): String =
        normalizedHost?.takeIf { it.isNotBlank() }
            ?: throw ProviderException(ProviderException.Reason.BAD_CREDENTIALS)

    private fun ContentSource.requireUsername(): String =
        username?.takeIf { it.isNotBlank() }
            ?: throw ProviderException(ProviderException.Reason.BAD_CREDENTIALS)

    private fun ContentSource.requirePassword(): String =
        password?.takeIf { it.isNotBlank() }
            ?: throw ProviderException(ProviderException.Reason.BAD_CREDENTIALS)

    private fun String.encoded(): String = URLEncoder.encode(this, "UTF-8")

    companion object {
        const val LIVE_ID_PREFIX = "xt:live:"
        const val MOVIE_ID_PREFIX = "xt:movie:"
        const val SERIES_ID_PREFIX = "xt:series:"
        const val EPISODE_ID_PREFIX = "xt:episode:"

        private const val ACTION_LIVE_CATEGORIES = "get_live_categories"
        private const val ACTION_LIVE_STREAMS = "get_live_streams"
        private const val ACTION_VOD_CATEGORIES = "get_vod_categories"
        private const val ACTION_VOD_STREAMS = "get_vod_streams"
        private const val ACTION_SERIES_CATEGORIES = "get_series_categories"
        private const val ACTION_SERIES = "get_series"
        private const val ACTION_SERIES_INFO = "get_series_info"
        private const val DEFAULT_VOD_EXTENSION = "mp4"

        private val TIMESHIFT_FORMAT = SimpleDateFormat("yyyy-MM-dd:HH-mm", Locale.US)
    }
}
