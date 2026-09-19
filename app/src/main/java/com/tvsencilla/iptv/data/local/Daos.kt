package com.tvsencilla.iptv.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ChannelDao {

    @Query(
        """
        SELECT c.*, f.position AS favoritePosition
        FROM channels c LEFT JOIN favorites f ON f.channelId = c.id
        ORDER BY c.listNumber ASC
        """,
    )
    fun observeChannels(): Flow<List<ChannelWithFavorite>>

    @Query(
        """
        SELECT c.*, f.position AS favoritePosition
        FROM channels c INNER JOIN favorites f ON f.channelId = c.id
        ORDER BY f.position ASC
        """,
    )
    fun observeFavorites(): Flow<List<ChannelWithFavorite>>

    @Query(
        """
        SELECT c.*, f.position AS favoritePosition
        FROM channels c LEFT JOIN favorites f ON f.channelId = c.id
        WHERE c.id = :id
        """,
    )
    suspend fun channelById(id: String): ChannelWithFavorite?

    @Query(
        """
        SELECT c.*, f.position AS favoritePosition
        FROM channels c LEFT JOIN favorites f ON f.channelId = c.id
        WHERE c.name LIKE :pattern
        ORDER BY c.listNumber ASC
        LIMIT 300
        """,
    )
    suspend fun search(pattern: String): List<ChannelWithFavorite>

    @Query("SELECT COUNT(*) FROM channels")
    suspend fun count(): Int

    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun observeCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT categoryId AS id, COUNT(*) AS total FROM channels WHERE categoryId IS NOT NULL GROUP BY categoryId")
    fun observeCategoryCounts(): Flow<List<CategoryCount>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChannels(channels: List<ChannelEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategories(categories: List<CategoryEntity>)

    @Query("DELETE FROM channels")
    suspend fun clearChannels()

    @Query("DELETE FROM categories")
    suspend fun clearCategories()

    /**
     * Replacing the channel list must not drop favourites, so rows the user starred are kept by
     * id even when the provider renumbers everything.
     */
    @Transaction
    suspend fun replaceAll(channels: List<ChannelEntity>, categories: List<CategoryEntity>) {
        clearChannels()
        clearCategories()
        upsertCategories(categories)
        upsertChannels(channels)
        pruneOrphanFavorites()
    }

    @Query("DELETE FROM favorites WHERE channelId NOT IN (SELECT id FROM channels)")
    suspend fun pruneOrphanFavorites()
}

data class CategoryCount(val id: String, val total: Int)

@Dao
interface FavoriteDao {

    @Query("SELECT channelId FROM favorites ORDER BY position ASC")
    suspend fun orderedIds(): List<String>

    @Query("SELECT MAX(position) FROM favorites")
    suspend fun maxPosition(): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(favorite: FavoriteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(favorites: List<FavoriteEntity>)

    @Query("DELETE FROM favorites WHERE channelId = :channelId")
    suspend fun delete(channelId: String)

    @Query("DELETE FROM favorites")
    suspend fun clear()

    /** Writes the whole order in one go, so the numbers the user sees can never drift. */
    @Transaction
    suspend fun replaceOrder(orderedChannelIds: List<String>) {
        clear()
        upsertAll(orderedChannelIds.mapIndexed { index, id -> FavoriteEntity(id, index) })
    }

    @Transaction
    suspend fun addAtEnd(channelId: String) {
        val ids = orderedIds()
        if (ids.contains(channelId)) return
        upsert(FavoriteEntity(channelId, ids.size))
    }

    /** Removing a favourite closes the gap so the list stays 1, 2, 3… with no holes. */
    @Transaction
    suspend fun removeAndCompact(channelId: String) {
        val ids = orderedIds().filterNot { it == channelId }
        replaceOrder(ids)
    }
}

@Dao
interface EpgDao {

    @Query(
        """
        SELECT * FROM epg_programs
        WHERE epgChannelId = :epgChannelId AND endMillis > :fromMillis AND startMillis < :toMillis
        ORDER BY startMillis ASC
        """,
    )
    fun observeWindow(epgChannelId: String, fromMillis: Long, toMillis: Long): Flow<List<EpgProgramEntity>>

    @Query(
        """
        SELECT * FROM epg_programs
        WHERE epgChannelId = :epgChannelId AND endMillis > :fromMillis
        ORDER BY startMillis ASC
        LIMIT 2
        """,
    )
    fun observeNowNext(epgChannelId: String, fromMillis: Long): Flow<List<EpgProgramEntity>>

    @Query(
        """
        SELECT * FROM epg_programs
        WHERE epgChannelId = :epgChannelId AND startMillis <= :atMillis AND endMillis > :atMillis
        LIMIT 1
        """,
    )
    suspend fun programAt(epgChannelId: String, atMillis: Long): EpgProgramEntity?

    @Query(
        """
        SELECT c.*, f.position AS favoritePosition,
               p.title AS programTitle, p.startMillis AS programStart, p.endMillis AS programEnd
        FROM epg_programs p
        INNER JOIN channels c ON c.epgChannelId = p.epgChannelId
        LEFT JOIN favorites f ON f.channelId = c.id
        WHERE p.title LIKE :pattern AND p.endMillis > :fromMillis AND p.startMillis < :toMillis
        ORDER BY p.startMillis ASC
        LIMIT 300
        """,
    )
    suspend fun searchPrograms(pattern: String, fromMillis: Long, toMillis: Long): List<ProgramOnChannel>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(programs: List<EpgProgramEntity>)

    /**
     * Blocking on purpose: the XMLTV parser hands programmes back through a synchronous callback,
     * and this lets each batch be flushed from inside it without blocking a coroutine.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertBlocking(programs: List<EpgProgramEntity>)

    @Query("DELETE FROM epg_programs WHERE endMillis < :beforeMillis")
    suspend fun deleteOlderThan(beforeMillis: Long)

    @Query("DELETE FROM epg_programs")
    suspend fun clear()

    @Query("SELECT COUNT(*) FROM epg_programs")
    suspend fun count(): Int
}

@Dao
interface VodDao {

    @Query("SELECT * FROM movies ORDER BY title ASC")
    fun observeMovies(): Flow<List<MovieEntity>>

    @Query("SELECT * FROM series ORDER BY title ASC")
    fun observeSeries(): Flow<List<SeriesEntity>>

    @Query("SELECT * FROM movies WHERE id = :id")
    suspend fun movieById(id: String): MovieEntity?

    @Query("SELECT * FROM series WHERE id = :id")
    suspend fun seriesById(id: String): SeriesEntity?

    @Query("SELECT * FROM episodes WHERE seriesId = :seriesId ORDER BY seasonNumber ASC, episodeNumber ASC")
    suspend fun episodesOf(seriesId: String): List<EpisodeEntity>

    @Query("SELECT * FROM episodes WHERE id = :id")
    suspend fun episodeById(id: String): EpisodeEntity?

    @Query("SELECT * FROM movies WHERE title LIKE :pattern ORDER BY title ASC LIMIT 300")
    suspend fun searchMovies(pattern: String): List<MovieEntity>

    @Query("SELECT * FROM series WHERE title LIKE :pattern ORDER BY title ASC LIMIT 300")
    suspend fun searchSeries(pattern: String): List<SeriesEntity>

    @Query("SELECT COUNT(*) FROM movies")
    suspend fun movieCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMovies(movies: List<MovieEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSeries(series: List<SeriesEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEpisodes(episodes: List<EpisodeEntity>)

    @Query("DELETE FROM movies")
    suspend fun clearMovies()

    @Query("DELETE FROM series")
    suspend fun clearSeries()

    @Query("DELETE FROM episodes")
    suspend fun clearEpisodes()

    @Transaction
    suspend fun replaceCatalog(movies: List<MovieEntity>, series: List<SeriesEntity>) {
        clearMovies()
        clearSeries()
        clearEpisodes()
        upsertMovies(movies)
        upsertSeries(series)
    }
}

@Dao
interface LikeDao {

    @Query("SELECT itemId FROM likes")
    fun observeLikedIds(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(like: LikeEntity)

    @Query("DELETE FROM likes WHERE itemId = :itemId")
    suspend fun delete(itemId: String)
}

@Dao
interface ProgressDao {

    @Query("SELECT * FROM watch_progress WHERE itemId = :itemId")
    suspend fun byItem(itemId: String): WatchProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: WatchProgressEntity)

    @Query(
        """
        SELECT m.*, p.positionMillis, p.durationMillis, p.updatedAtMillis
        FROM movies m INNER JOIN watch_progress p ON p.itemId = m.id
        WHERE p.durationMillis > 0
          AND p.positionMillis > :minPositionMillis
          AND (CAST(p.positionMillis AS REAL) / p.durationMillis) < :maxFraction
        ORDER BY p.updatedAtMillis DESC
        LIMIT 12
        """,
    )
    fun observeMoviesInProgress(
        minPositionMillis: Long,
        maxFraction: Double,
    ): Flow<List<MovieWithProgress>>

    @Query(
        """
        SELECT e.*, s.title AS seriesTitle, s.posterUrl AS seriesPosterUrl,
               p.positionMillis, p.durationMillis, p.updatedAtMillis
        FROM episodes e
        INNER JOIN watch_progress p ON p.itemId = e.id
        LEFT JOIN series s ON s.id = e.seriesId
        WHERE p.durationMillis > 0
          AND p.positionMillis > :minPositionMillis
          AND (CAST(p.positionMillis AS REAL) / p.durationMillis) < :maxFraction
        ORDER BY p.updatedAtMillis DESC
        LIMIT 12
        """,
    )
    fun observeEpisodesInProgress(
        minPositionMillis: Long,
        maxFraction: Double,
    ): Flow<List<EpisodeWithProgress>>

    @Query("SELECT itemId FROM watch_progress WHERE durationMillis > 0 AND (CAST(positionMillis AS REAL) / durationMillis) >= :watchedFraction")
    fun observeWatchedItemIds(watchedFraction: Double): Flow<List<String>>

    @Query("DELETE FROM watch_progress")
    suspend fun clear()
}
