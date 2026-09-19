package com.tvsencilla.iptv.data.repository

import com.tvsencilla.iptv.data.local.LikeDao
import com.tvsencilla.iptv.data.local.LikeEntity
import com.tvsencilla.iptv.data.local.ProgressDao
import com.tvsencilla.iptv.data.local.VodDao
import com.tvsencilla.iptv.data.local.WatchProgressEntity
import com.tvsencilla.iptv.data.local.toDomain
import com.tvsencilla.iptv.data.local.toEntity
import com.tvsencilla.iptv.data.local.toSeasons
import com.tvsencilla.iptv.data.provider.ProviderRegistry
import com.tvsencilla.iptv.data.settings.RefreshTracker
import com.tvsencilla.iptv.domain.model.ContinueWatchingItem
import com.tvsencilla.iptv.domain.model.Episode
import com.tvsencilla.iptv.domain.model.Movie
import com.tvsencilla.iptv.domain.model.PlayableKind
import com.tvsencilla.iptv.domain.model.Series
import com.tvsencilla.iptv.domain.model.WatchProgress
import com.tvsencilla.iptv.domain.repository.SettingsRepository
import com.tvsencilla.iptv.domain.repository.SourceRepository
import com.tvsencilla.iptv.domain.repository.VodRepository
import com.tvsencilla.iptv.domain.search.TextSearch
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class VodRepositoryImpl @Inject constructor(
    private val vodDao: VodDao,
    private val progressDao: ProgressDao,
    private val likeDao: LikeDao,
    private val registry: ProviderRegistry,
    private val sourceRepository: SourceRepository,
    private val settings: SettingsRepository,
    private val refreshTracker: RefreshTracker,
) : VodRepository {

    private val refreshMutex = Mutex()

    private val hiddenCategoryIds: Flow<Set<String>> =
        settings.settings.map { it.hiddenCategoryIds }

    override fun observeMovies(): Flow<List<Movie>> =
        combine(vodDao.observeMovies(), hiddenCategoryIds) { rows, hidden ->
            rows.map { it.toDomain() }.filterNot { it.categoryId in hidden }
        }

    override fun observeSeries(): Flow<List<Series>> =
        combine(vodDao.observeSeries(), hiddenCategoryIds) { rows, hidden ->
            rows.map { it.toDomain() }.filterNot { it.categoryId in hidden }
        }

    override fun observeContinueWatching(): Flow<List<ContinueWatchingItem>> = combine(
        progressDao.observeMoviesInProgress(MIN_RESUME_MILLIS, MAX_RESUME_FRACTION),
        progressDao.observeEpisodesInProgress(MIN_RESUME_MILLIS, MAX_RESUME_FRACTION),
    ) { movies, episodes ->
        val movieItems = movies.map { row ->
            ContinueWatchingItem(
                itemId = row.movie.id,
                kind = PlayableKind.MOVIE,
                title = row.movie.title,
                posterUrl = row.movie.posterUrl,
                streamUrl = row.movie.streamUrl,
                positionMillis = row.positionMillis,
                durationMillis = row.durationMillis,
            ) to row.updatedAtMillis
        }
        val episodeItems = episodes.map { row ->
            ContinueWatchingItem(
                itemId = row.episode.id,
                kind = PlayableKind.EPISODE,
                title = row.seriesTitle ?: row.episode.title,
                subtitle = "${row.episode.seasonNumber}x${row.episode.episodeNumber} " +
                    row.episode.title,
                posterUrl = row.seriesPosterUrl ?: row.episode.stillUrl,
                streamUrl = row.episode.streamUrl,
                positionMillis = row.positionMillis,
                durationMillis = row.durationMillis,
            ) to row.updatedAtMillis
        }
        (movieItems + episodeItems).sortedByDescending { it.second }.map { it.first }.take(12)
    }

    override suspend fun movieById(id: String): Movie? = vodDao.movieById(id)?.toDomain()

    /**
     * Seasons and episodes are fetched the first time a series is opened, then cached, so moving
     * between episodes does not hit the network again.
     */
    override suspend fun seriesDetail(id: String): Series? {
        val stored = vodDao.seriesById(id) ?: return null
        if (stored.detailLoaded) {
            return stored.toDomain(seasons = vodDao.episodesOf(id).toSeasons())
        }

        val source = sourceRepository.current() ?: return stored.toDomain()
        val detail = runCatching { registry.forSource(source).fetchSeriesDetail(source, id) }
            .getOrNull()
            ?: return stored.toDomain()

        val episodes = detail.seasons.flatMap { it.episodes }
        vodDao.upsertEpisodes(episodes.map { it.toEntity() })
        vodDao.upsertSeries(listOf(stored.copy(detailLoaded = true)))
        return stored.toDomain(seasons = detail.seasons)
    }

    override suspend fun episodeById(id: String): Episode? = vodDao.episodeById(id)?.toDomain()

    override suspend fun nextEpisode(episodeId: String): Episode? {
        val current = vodDao.episodeById(episodeId) ?: return null
        val ordered = vodDao.episodesOf(current.seriesId)
        val index = ordered.indexOfFirst { it.id == episodeId }
        if (index < 0 || index == ordered.lastIndex) return null
        return ordered[index + 1].toDomain()
    }

    override suspend fun refresh(force: Boolean) = refreshMutex.withLock {
        val source = sourceRepository.current() ?: return@withLock
        val stale = refreshTracker.isStale(RefreshTracker.Kind.VOD, RefreshTracker.VOD_MAX_AGE)
        if (!force && !stale && vodDao.movieCount() > 0) return@withLock

        val provider = registry.forSource(source)
        val movies = runCatching { provider.fetchMovies(source) }.getOrDefault(emptyList())
        val series = runCatching { provider.fetchSeries(source) }.getOrDefault(emptyList())
        if (movies.isEmpty() && series.isEmpty()) return@withLock

        vodDao.replaceCatalog(
            movies = movies.map { it.toEntity() },
            series = series.map { it.toEntity() },
        )
        refreshTracker.markRefreshed(RefreshTracker.Kind.VOD)
    }

    override suspend fun saveProgress(
        itemId: String,
        kind: PlayableKind,
        positionMillis: Long,
        durationMillis: Long,
    ) {
        progressDao.upsert(
            WatchProgressEntity(
                itemId = itemId,
                kind = kind.name,
                positionMillis = positionMillis,
                durationMillis = durationMillis,
                updatedAtMillis = System.currentTimeMillis(),
            ),
        )
    }

    override suspend fun progressFor(itemId: String): WatchProgress? =
        progressDao.byItem(itemId)?.toDomain()

    override suspend fun searchMovies(query: String): List<Movie> {
        val hidden = settings.current().hiddenCategoryIds
        return TextSearch.find(query, { vodDao.searchMovies(it) }, { it.title })
            .map { it.toDomain() }
            .filterNot { it.categoryId in hidden }
            .sortedBy { it.title }
            .take(MAX_SEARCH_RESULTS)
    }

    override suspend fun searchSeries(query: String): List<Series> {
        val hidden = settings.current().hiddenCategoryIds
        return TextSearch.find(query, { vodDao.searchSeries(it) }, { it.title })
            .map { it.toDomain() }
            .filterNot { it.categoryId in hidden }
            .sortedBy { it.title }
            .take(MAX_SEARCH_RESULTS)
    }

    override fun observeWatchedIds(): Flow<Set<String>> =
        progressDao.observeWatchedItemIds(WATCHED_FRACTION).map { it.toSet() }

    override fun observeLikedIds(): Flow<Set<String>> =
        likeDao.observeLikedIds().map { it.toSet() }

    override suspend fun setLiked(itemId: String, isSeries: Boolean, liked: Boolean) {
        if (liked) {
            likeDao.insert(
                LikeEntity(
                    itemId = itemId,
                    kind = if (isSeries) "SERIES" else "MOVIE",
                    likedAtMillis = System.currentTimeMillis(),
                ),
            )
        } else {
            likeDao.delete(itemId)
        }
    }

    private companion object {
        const val MIN_RESUME_MILLIS = 60_000L
        const val MAX_RESUME_FRACTION = 0.95
        const val WATCHED_FRACTION = 0.9
        const val MAX_SEARCH_RESULTS = 60
    }
}
