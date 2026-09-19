package com.tvsencilla.iptv.domain.repository

import com.tvsencilla.iptv.domain.model.AppSettings
import com.tvsencilla.iptv.domain.model.Category
import com.tvsencilla.iptv.domain.model.Channel
import com.tvsencilla.iptv.domain.model.ContentSource
import com.tvsencilla.iptv.domain.model.ContinueWatchingItem
import com.tvsencilla.iptv.domain.model.EpgProgram
import com.tvsencilla.iptv.domain.model.Episode
import com.tvsencilla.iptv.domain.model.Movie
import com.tvsencilla.iptv.domain.model.NowNext
import com.tvsencilla.iptv.domain.model.PlayableKind
import com.tvsencilla.iptv.domain.model.ProgramMatch
import com.tvsencilla.iptv.domain.model.ProviderCapabilities
import com.tvsencilla.iptv.domain.model.Series
import com.tvsencilla.iptv.domain.model.WatchProgress
import kotlinx.coroutines.flow.Flow

interface SourceRepository {
    val source: Flow<ContentSource?>
    val capabilities: Flow<ProviderCapabilities>
    suspend fun current(): ContentSource?

    /** Validates the source against the provider before storing it. */
    suspend fun save(source: ContentSource): ProviderCapabilities
    suspend fun clear()
}

interface ChannelRepository {
    fun observeChannels(): Flow<List<Channel>>
    fun observeCategories(): Flow<List<Category>>

    /** Favourites in the user's own order, already numbered 1, 2, 3… */
    fun observeFavorites(): Flow<List<Channel>>

    suspend fun channelById(id: String): Channel?

    /** Null when the provider has no catch-up for this programme, so the option stays hidden. */
    suspend fun catchUpUrl(channel: Channel, program: EpgProgram): String?
    suspend fun refresh(force: Boolean = false)
    suspend fun addFavorite(channelId: String)
    suspend fun removeFavorite(channelId: String)
    suspend fun saveFavoriteOrder(orderedChannelIds: List<String>)
    suspend fun search(query: String): List<Channel>
}

interface EpgRepository {
    fun observeNowNext(epgChannelId: String?): Flow<NowNext>
    fun observeSchedule(epgChannelId: String, fromMillis: Long, toMillis: Long): Flow<List<EpgProgram>>
    suspend fun refresh(force: Boolean = false)
    suspend fun programAt(epgChannelId: String, atMillis: Long): EpgProgram?

    /**
     * Programas cuyo título encaja con lo que el usuario ha dicho, emitiéndose ahora o en las
     * próximas horas. Lo que ya se está emitiendo va primero.
     */
    suspend fun searchPrograms(query: String): List<ProgramMatch>
}

interface VodRepository {
    fun observeMovies(): Flow<List<Movie>>
    fun observeSeries(): Flow<List<Series>>
    fun observeContinueWatching(): Flow<List<ContinueWatchingItem>>

    /** Ids the user has essentially finished, used to tick episodes as watched. */
    fun observeWatchedIds(): Flow<Set<String>>

    /** Películas y series marcadas con "Me gusta". */
    fun observeLikedIds(): Flow<Set<String>>

    suspend fun setLiked(itemId: String, isSeries: Boolean, liked: Boolean)
    suspend fun movieById(id: String): Movie?
    suspend fun seriesDetail(id: String): Series?
    suspend fun episodeById(id: String): Episode?

    /** The episode that follows [episodeId] inside its series, crossing into the next season. */
    suspend fun nextEpisode(episodeId: String): Episode?
    suspend fun refresh(force: Boolean = false)
    suspend fun saveProgress(itemId: String, kind: PlayableKind, positionMillis: Long, durationMillis: Long)
    suspend fun progressFor(itemId: String): WatchProgress?
    suspend fun searchMovies(query: String): List<Movie>
    suspend fun searchSeries(query: String): List<Series>
}

interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun current(): AppSettings
    suspend fun update(transform: (AppSettings) -> AppSettings)
}

/**
 * Keeps the favourite order attached to the user's profile on a server, so a replacement TV box
 * comes back with the same numbering. Simulated until the backend exists.
 */
interface FavoritesSyncRepository {
    suspend fun pull(): List<String>?
    suspend fun push(orderedChannelIds: List<String>)
}
