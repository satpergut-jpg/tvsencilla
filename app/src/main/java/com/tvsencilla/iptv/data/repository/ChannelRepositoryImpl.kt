package com.tvsencilla.iptv.data.repository

import com.tvsencilla.iptv.data.local.ChannelDao
import com.tvsencilla.iptv.data.local.FavoriteDao
import com.tvsencilla.iptv.data.local.toDomain
import com.tvsencilla.iptv.data.local.toEntity
import com.tvsencilla.iptv.data.provider.ProviderRegistry
import com.tvsencilla.iptv.data.settings.RefreshTracker
import com.tvsencilla.iptv.domain.model.Category
import com.tvsencilla.iptv.domain.model.Channel
import com.tvsencilla.iptv.domain.model.EpgProgram
import com.tvsencilla.iptv.domain.provider.ProviderException
import com.tvsencilla.iptv.domain.repository.ChannelRepository
import com.tvsencilla.iptv.domain.repository.FavoritesSyncRepository
import com.tvsencilla.iptv.domain.repository.SettingsRepository
import com.tvsencilla.iptv.domain.repository.SourceRepository
import com.tvsencilla.iptv.domain.search.TextSearch
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class ChannelRepositoryImpl @Inject constructor(
    private val channelDao: ChannelDao,
    private val favoriteDao: FavoriteDao,
    private val registry: ProviderRegistry,
    private val sourceRepository: SourceRepository,
    private val settings: SettingsRepository,
    private val refreshTracker: RefreshTracker,
    private val favoritesSync: FavoritesSyncRepository,
) : ChannelRepository {

    private val refreshMutex = Mutex()

    private val hiddenCategoryIds: Flow<Set<String>> =
        settings.settings.map { it.hiddenCategoryIds }

    override fun observeChannels(): Flow<List<Channel>> =
        combine(channelDao.observeChannels(), hiddenCategoryIds) { rows, hidden ->
            rows.map { it.toDomain() }.filterNot { it.categoryId in hidden }
        }

    /**
     * Favourites keep their own numbering even when a category is hidden by parental control:
     * a hidden channel drops out of the list and the remaining numbers close up, so the user
     * never sees a gap.
     */
    override fun observeFavorites(): Flow<List<Channel>> =
        combine(channelDao.observeFavorites(), hiddenCategoryIds) { rows, hidden ->
            rows.map { it.toDomain() }
                .filterNot { it.categoryId in hidden }
                .mapIndexed { index, channel -> channel.copy(favoriteNumber = index + 1) }
        }

    override fun observeCategories(): Flow<List<Category>> = combine(
        channelDao.observeCategories(),
        channelDao.observeCategoryCounts(),
        hiddenCategoryIds,
    ) { categories, counts, hidden ->
        val totals = counts.associate { it.id to it.total }
        categories
            .filterNot { it.id in hidden }
            .map { it.toDomain(channelCount = totals[it.id] ?: 0) }
            .filter { it.channelCount > 0 }
    }

    override suspend fun channelById(id: String): Channel? =
        channelDao.channelById(id)?.toDomain()

    override suspend fun catchUpUrl(channel: Channel, program: EpgProgram): String? {
        if (!channel.supportsCatchUp) return null
        val source = sourceRepository.current() ?: return null
        return registry.forSource(source).catchUpUrl(source, channel, program)
    }

    override suspend fun search(query: String): List<Channel> {
        val hidden = settings.current().hiddenCategoryIds
        return TextSearch.find(query, { channelDao.search(it) }, { it.channel.name })
            .map { it.toDomain() }
            .filterNot { it.categoryId in hidden }
            .sortedBy { it.listNumber }
            .take(MAX_SEARCH_RESULTS)
    }

    override suspend fun refresh(force: Boolean) = refreshMutex.withLock {
        val source = sourceRepository.current() ?: throw ProviderException(ProviderException.Reason.BAD_CREDENTIALS)
        val stale = refreshTracker.isStale(RefreshTracker.Kind.CHANNELS, RefreshTracker.CHANNELS_MAX_AGE)
        if (!force && !stale && channelDao.count() > 0) return@withLock

        val catalog = registry.forSource(source).fetchLive(source)
        if (catalog.channels.isEmpty()) throw ProviderException(ProviderException.Reason.EMPTY)

        channelDao.replaceAll(
            channels = catalog.channels.map { it.toEntity() },
            categories = catalog.categories.map { it.toEntity() },
        )
        restoreOrderFromProfile(catalog.channels.map { it.id }.toSet())
        refreshTracker.markRefreshed(RefreshTracker.Kind.CHANNELS)
    }

    override suspend fun addFavorite(channelId: String) {
        favoriteDao.addAtEnd(channelId)
        pushOrder()
    }

    override suspend fun removeFavorite(channelId: String) {
        favoriteDao.removeAndCompact(channelId)
        pushOrder()
    }

    override suspend fun saveFavoriteOrder(orderedChannelIds: List<String>) {
        favoriteDao.replaceOrder(orderedChannelIds)
        pushOrder()
    }

    /**
     * After a fresh channel list, the order stored against the user's profile wins, so replacing
     * the TV box brings the same numbering back. Ids the provider no longer serves are dropped.
     */
    private suspend fun restoreOrderFromProfile(availableIds: Set<String>) {
        val local = favoriteDao.orderedIds()
        val remote = runCatching { favoritesSync.pull() }.getOrNull()
        val chosen = if (local.isEmpty() && !remote.isNullOrEmpty()) remote else local
        val kept = chosen.filter { it in availableIds }
        if (kept != local) favoriteDao.replaceOrder(kept)
    }

    private suspend fun pushOrder() {
        val ids = favoriteDao.orderedIds()
        runCatching { favoritesSync.push(ids) }
    }

    private companion object {
        const val MAX_SEARCH_RESULTS = 60
    }
}
