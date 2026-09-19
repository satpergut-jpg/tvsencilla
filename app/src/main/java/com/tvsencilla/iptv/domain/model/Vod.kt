package com.tvsencilla.iptv.domain.model

data class Movie(
    val id: String,
    val title: String,
    val streamUrl: String,
    val posterUrl: String? = null,
    val year: String? = null,
    val durationMinutes: Int? = null,
    val plot: String? = null,
    val categoryId: String? = null,
    val categoryName: String? = null,
)

data class Series(
    val id: String,
    val title: String,
    val posterUrl: String? = null,
    val year: String? = null,
    val plot: String? = null,
    val categoryId: String? = null,
    val categoryName: String? = null,
    val seasons: List<Season> = emptyList(),
)

data class Season(
    val number: Int,
    val episodes: List<Episode>,
)

data class Episode(
    val id: String,
    val seriesId: String,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val title: String,
    val streamUrl: String,
    val durationMinutes: Int? = null,
    val plot: String? = null,
    val stillUrl: String? = null,
)
