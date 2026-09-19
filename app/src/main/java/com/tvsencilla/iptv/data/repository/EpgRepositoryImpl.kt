package com.tvsencilla.iptv.data.repository

import com.tvsencilla.iptv.data.local.ChannelWithFavorite
import com.tvsencilla.iptv.data.local.EpgDao
import com.tvsencilla.iptv.data.local.EpgProgramEntity
import com.tvsencilla.iptv.data.local.toDomain
import com.tvsencilla.iptv.data.local.toEntity
import com.tvsencilla.iptv.data.provider.ProviderRegistry
import com.tvsencilla.iptv.data.settings.RefreshTracker
import com.tvsencilla.iptv.domain.model.EpgProgram
import com.tvsencilla.iptv.domain.model.NowNext
import com.tvsencilla.iptv.domain.model.ProgramMatch
import com.tvsencilla.iptv.domain.repository.EpgRepository
import com.tvsencilla.iptv.domain.repository.SettingsRepository
import com.tvsencilla.iptv.domain.repository.SourceRepository
import com.tvsencilla.iptv.domain.search.TextSearch
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Singleton
class EpgRepositoryImpl @Inject constructor(
    private val epgDao: EpgDao,
    private val registry: ProviderRegistry,
    private val sourceRepository: SourceRepository,
    private val refreshTracker: RefreshTracker,
    private val settings: SettingsRepository,
) : EpgRepository {

    private val refreshMutex = Mutex()

    /**
     * Re-queries on a slow tick so "Ahora / Después" rolls over on its own without the user
     * touching anything.
     */
    override fun observeNowNext(epgChannelId: String?): Flow<NowNext> {
        if (epgChannelId.isNullOrBlank()) return flowOf(NowNext())
        return ticker(NOW_NEXT_TICK_MILLIS).flatMapLatest { now ->
            epgDao.observeNowNext(epgChannelId, now).map { rows ->
                val programs = rows.map { it.toDomain() }
                NowNext(
                    now = programs.firstOrNull { it.isLiveAt(now) },
                    next = programs.firstOrNull { it.startMillis > now },
                )
            }
        }
    }

    override fun observeSchedule(
        epgChannelId: String,
        fromMillis: Long,
        toMillis: Long,
    ): Flow<List<EpgProgram>> =
        epgDao.observeWindow(epgChannelId, fromMillis, toMillis).map { rows -> rows.map { it.toDomain() } }

    override suspend fun programAt(epgChannelId: String, atMillis: Long): EpgProgram? =
        epgDao.programAt(epgChannelId, atMillis)?.toDomain()

    override suspend fun searchPrograms(query: String): List<ProgramMatch> {
        val now = System.currentTimeMillis()
        val hidden = settings.current().hiddenCategoryIds

        return TextSearch.find(
            query = query,
            fetch = { pattern -> epgDao.searchPrograms(pattern, now, now + SEARCH_WINDOW_MILLIS) },
            text = { it.programTitle },
        )
            .asSequence()
            .map { row ->
                ProgramMatch(
                    channel = ChannelWithFavorite(row.channel, row.favoritePosition).toDomain(),
                    program = EpgProgram(
                        epgChannelId = row.channel.epgChannelId.orEmpty(),
                        title = row.programTitle,
                        startMillis = row.programStart,
                        endMillis = row.programEnd,
                    ),
                )
            }
            .filterNot { it.channel.categoryId in hidden }
            .distinctBy { it.channel.id to it.program.startMillis }
            .sortedWith(compareByDescending<ProgramMatch> { it.program.isLiveAt(now) }.thenBy { it.program.startMillis })
            .take(MAX_SEARCH_RESULTS)
            .toList()
    }

    override suspend fun refresh(force: Boolean) = refreshMutex.withLock {
        val source = sourceRepository.current() ?: return@withLock
        val provider = registry.forSource(source)
        val stale = refreshTracker.isStale(RefreshTracker.Kind.EPG, RefreshTracker.EPG_MAX_AGE)
        if (!force && !stale && epgDao.count() > 0) return@withLock

        withContext(Dispatchers.IO) {
            // Batching keeps a 100 MB guide down to a few hundred kilobytes of live memory.
            val batch = ArrayList<EpgProgramEntity>(BATCH_SIZE)
            provider.fetchEpg(source) { program ->
                batch += program.toEntity()
                if (batch.size >= BATCH_SIZE) {
                    epgDao.upsertBlocking(batch)
                    batch.clear()
                }
            }
            if (batch.isNotEmpty()) epgDao.upsertBlocking(batch)
            epgDao.deleteOlderThan(System.currentTimeMillis() - KEEP_PAST_MILLIS)
        }
        refreshTracker.markRefreshed(RefreshTracker.Kind.EPG)
    }

    private fun ticker(intervalMillis: Long): Flow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(intervalMillis)
        }
    }

    private companion object {
        const val BATCH_SIZE = 500
        const val NOW_NEXT_TICK_MILLIS = 30_000L
        const val KEEP_PAST_MILLIS = 6 * 60 * 60 * 1000L

        /** Un partido que empieza esta noche también interesa cuando se busca por la mañana. */
        const val SEARCH_WINDOW_MILLIS = 12 * 60 * 60 * 1000L
        const val MAX_SEARCH_RESULTS = 20
    }
}
