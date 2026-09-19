package com.tvsencilla.iptv.data.local

import com.tvsencilla.iptv.domain.model.Category
import com.tvsencilla.iptv.domain.model.Channel
import com.tvsencilla.iptv.domain.model.EpgProgram
import com.tvsencilla.iptv.domain.model.Episode
import com.tvsencilla.iptv.domain.model.Movie
import com.tvsencilla.iptv.domain.model.PlayableKind
import com.tvsencilla.iptv.domain.model.Season
import com.tvsencilla.iptv.domain.model.Series
import com.tvsencilla.iptv.domain.model.WatchProgress

/** Stored positions are 0-based; the number the user sees starts at 1. */
fun ChannelWithFavorite.toDomain(): Channel = Channel(
    id = channel.id,
    name = channel.name,
    streamUrl = channel.streamUrl,
    logoUrl = channel.logoUrl,
    categoryId = channel.categoryId,
    categoryName = channel.categoryName,
    epgChannelId = channel.epgChannelId,
    listNumber = channel.listNumber,
    favoriteNumber = favoritePosition?.plus(1),
    supportsCatchUp = channel.supportsCatchUp,
)

fun Channel.toEntity(): ChannelEntity = ChannelEntity(
    id = id,
    name = name,
    streamUrl = streamUrl,
    logoUrl = logoUrl,
    categoryId = categoryId,
    categoryName = categoryName,
    epgChannelId = epgChannelId,
    listNumber = listNumber,
    supportsCatchUp = supportsCatchUp,
)

fun CategoryEntity.toDomain(channelCount: Int = 0): Category =
    Category(id = id, name = name, channelCount = channelCount)

fun Category.toEntity(): CategoryEntity = CategoryEntity(id = id, name = name)

fun EpgProgramEntity.toDomain(): EpgProgram = EpgProgram(
    epgChannelId = epgChannelId,
    title = title,
    startMillis = startMillis,
    endMillis = endMillis,
    description = description,
)

fun EpgProgram.toEntity(): EpgProgramEntity = EpgProgramEntity(
    epgChannelId = epgChannelId,
    startMillis = startMillis,
    endMillis = endMillis,
    title = title,
    description = description,
)

fun MovieEntity.toDomain(): Movie = Movie(
    id = id,
    title = title,
    streamUrl = streamUrl,
    posterUrl = posterUrl,
    year = year,
    durationMinutes = durationMinutes,
    plot = plot,
    categoryId = categoryId,
    categoryName = categoryName,
)

fun Movie.toEntity(): MovieEntity = MovieEntity(
    id = id,
    title = title,
    streamUrl = streamUrl,
    posterUrl = posterUrl,
    year = year,
    durationMinutes = durationMinutes,
    plot = plot,
    categoryId = categoryId,
    categoryName = categoryName,
)

fun SeriesEntity.toDomain(seasons: List<Season> = emptyList()): Series = Series(
    id = id,
    title = title,
    posterUrl = posterUrl,
    year = year,
    plot = plot,
    categoryId = categoryId,
    categoryName = categoryName,
    seasons = seasons,
)

fun Series.toEntity(detailLoaded: Boolean = false): SeriesEntity = SeriesEntity(
    id = id,
    title = title,
    posterUrl = posterUrl,
    year = year,
    plot = plot,
    categoryId = categoryId,
    categoryName = categoryName,
    detailLoaded = detailLoaded,
)

fun EpisodeEntity.toDomain(): Episode = Episode(
    id = id,
    seriesId = seriesId,
    seasonNumber = seasonNumber,
    episodeNumber = episodeNumber,
    title = title,
    streamUrl = streamUrl,
    durationMinutes = durationMinutes,
    plot = plot,
    stillUrl = stillUrl,
)

fun Episode.toEntity(): EpisodeEntity = EpisodeEntity(
    id = id,
    seriesId = seriesId,
    seasonNumber = seasonNumber,
    episodeNumber = episodeNumber,
    title = title,
    streamUrl = streamUrl,
    durationMinutes = durationMinutes,
    plot = plot,
    stillUrl = stillUrl,
)

fun List<EpisodeEntity>.toSeasons(): List<Season> = groupBy { it.seasonNumber }
    .toSortedMap()
    .map { (number, episodes) ->
        Season(
            number = number,
            episodes = episodes.sortedBy { it.episodeNumber }.map { it.toDomain() },
        )
    }

fun WatchProgressEntity.toDomain(): WatchProgress = WatchProgress(
    itemId = itemId,
    kind = runCatching { PlayableKind.valueOf(kind) }.getOrDefault(PlayableKind.MOVIE),
    positionMillis = positionMillis,
    durationMillis = durationMillis,
    updatedAtMillis = updatedAtMillis,
)
