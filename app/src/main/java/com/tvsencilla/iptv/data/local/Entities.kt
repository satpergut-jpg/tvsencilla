package com.tvsencilla.iptv.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "channels", indices = [Index("categoryId"), Index("listNumber")])
data class ChannelEntity(
    @PrimaryKey val id: String,
    val name: String,
    val streamUrl: String,
    val logoUrl: String?,
    val categoryId: String?,
    val categoryName: String?,
    val epgChannelId: String?,
    val listNumber: Int,
    val supportsCatchUp: Boolean,
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
)

/** The user's own numbering lives here: [position] 0 is favourite number 1. */
@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val channelId: String,
    val position: Int,
)

@Entity(
    tableName = "epg_programs",
    primaryKeys = ["epgChannelId", "startMillis"],
    indices = [Index("epgChannelId", "endMillis")],
)
data class EpgProgramEntity(
    val epgChannelId: String,
    val startMillis: Long,
    val endMillis: Long,
    val title: String,
    val description: String?,
)

@Entity(tableName = "movies", indices = [Index("categoryId")])
data class MovieEntity(
    @PrimaryKey val id: String,
    val title: String,
    val streamUrl: String,
    val posterUrl: String?,
    val year: String?,
    val durationMinutes: Int?,
    val plot: String?,
    val categoryId: String?,
    val categoryName: String?,
)

@Entity(tableName = "series", indices = [Index("categoryId")])
data class SeriesEntity(
    @PrimaryKey val id: String,
    val title: String,
    val posterUrl: String?,
    val year: String?,
    val plot: String?,
    val categoryId: String?,
    val categoryName: String?,
    val detailLoaded: Boolean = false,
)

@Entity(tableName = "episodes", indices = [Index("seriesId", "seasonNumber", "episodeNumber")])
data class EpisodeEntity(
    @PrimaryKey val id: String,
    val seriesId: String,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val title: String,
    val streamUrl: String,
    val durationMinutes: Int?,
    val plot: String?,
    val stillUrl: String?,
)

/** Películas y series marcadas con "Me gusta". [kind] es MOVIE o SERIES. */
@Entity(tableName = "likes")
data class LikeEntity(
    @PrimaryKey val itemId: String,
    val kind: String,
    val likedAtMillis: Long,
)

@Entity(tableName = "watch_progress")
data class WatchProgressEntity(
    @PrimaryKey val itemId: String,
    val kind: String,
    val positionMillis: Long,
    val durationMillis: Long,
    val updatedAtMillis: Long,
)

/** Every channel plus its favourite number, so one query feeds both numbering modes. */
data class ChannelWithFavorite(
    @Embedded val channel: ChannelEntity,
    val favoritePosition: Int?,
)

/** Un programa de la guía con el canal que lo emite, para la búsqueda en la programación. */
data class ProgramOnChannel(
    @Embedded val channel: ChannelEntity,
    val favoritePosition: Int?,
    val programTitle: String,
    val programStart: Long,
    val programEnd: Long,
)

data class MovieWithProgress(
    @Embedded val movie: MovieEntity,
    val positionMillis: Long,
    val durationMillis: Long,
    val updatedAtMillis: Long,
)

data class EpisodeWithProgress(
    @Embedded val episode: EpisodeEntity,
    val seriesTitle: String?,
    val seriesPosterUrl: String?,
    val positionMillis: Long,
    val durationMillis: Long,
    val updatedAtMillis: Long,
)
