package com.tvsencilla.iptv.data.xtream.dto

import com.google.gson.annotations.SerializedName

/**
 * Xtream servers are wildly inconsistent about types: the same field arrives as `123`, `"123"`
 * or `""` depending on the panel version. Everything is therefore read as a string and converted
 * defensively.
 */

data class XtreamHandshakeDto(
    @SerializedName("user_info") val userInfo: XtreamUserInfoDto? = null,
    @SerializedName("server_info") val serverInfo: XtreamServerInfoDto? = null,
)

data class XtreamUserInfoDto(
    @SerializedName("auth") val auth: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("allowed_output_formats") val allowedOutputFormats: List<String>? = null,
) {
    val isAuthenticated: Boolean
        get() = auth == "1" && !status.equals("Banned", ignoreCase = true) &&
            !status.equals("Disabled", ignoreCase = true) &&
            !status.equals("Expired", ignoreCase = true)
}

data class XtreamServerInfoDto(
    @SerializedName("url") val url: String? = null,
    @SerializedName("port") val port: String? = null,
    @SerializedName("https_port") val httpsPort: String? = null,
    @SerializedName("timezone") val timezone: String? = null,
)

data class XtreamCategoryDto(
    @SerializedName("category_id") val categoryId: String? = null,
    @SerializedName("category_name") val categoryName: String? = null,
)

data class XtreamLiveStreamDto(
    @SerializedName("stream_id") val streamId: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("stream_icon") val streamIcon: String? = null,
    @SerializedName("epg_channel_id") val epgChannelId: String? = null,
    @SerializedName("category_id") val categoryId: String? = null,
    @SerializedName("num") val num: String? = null,
    @SerializedName("tv_archive") val tvArchive: String? = null,
    @SerializedName("tv_archive_duration") val tvArchiveDuration: String? = null,
) {
    val hasArchive: Boolean
        get() = tvArchive == "1" && (tvArchiveDuration?.toIntOrNull() ?: 0) > 0
}

data class XtreamVodStreamDto(
    @SerializedName("stream_id") val streamId: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("stream_icon") val streamIcon: String? = null,
    @SerializedName("container_extension") val containerExtension: String? = null,
    @SerializedName("category_id") val categoryId: String? = null,
    @SerializedName("plot") val plot: String? = null,
    @SerializedName("year") val year: String? = null,
    @SerializedName("releasedate") val releaseDate: String? = null,
    @SerializedName("episode_run_time") val runTime: String? = null,
)

data class XtreamSeriesDto(
    @SerializedName("series_id") val seriesId: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("cover") val cover: String? = null,
    @SerializedName("plot") val plot: String? = null,
    @SerializedName("releaseDate") val releaseDate: String? = null,
    @SerializedName("category_id") val categoryId: String? = null,
)

data class XtreamSeriesInfoDto(
    @SerializedName("info") val info: XtreamSeriesDto? = null,
    /** Keyed by season number, as a string. */
    @SerializedName("episodes") val episodes: Map<String, List<XtreamEpisodeDto>>? = null,
)

data class XtreamEpisodeDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("episode_num") val episodeNum: String? = null,
    @SerializedName("season") val season: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("container_extension") val containerExtension: String? = null,
    @SerializedName("info") val info: XtreamEpisodeInfoDto? = null,
)

data class XtreamEpisodeInfoDto(
    @SerializedName("plot") val plot: String? = null,
    @SerializedName("duration_secs") val durationSecs: String? = null,
    @SerializedName("movie_image") val movieImage: String? = null,
)
