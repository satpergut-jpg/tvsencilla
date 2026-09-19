package com.tvsencilla.iptv.data.xtream

import com.tvsencilla.iptv.data.xtream.dto.XtreamCategoryDto
import com.tvsencilla.iptv.data.xtream.dto.XtreamHandshakeDto
import com.tvsencilla.iptv.data.xtream.dto.XtreamLiveStreamDto
import com.tvsencilla.iptv.data.xtream.dto.XtreamSeriesDto
import com.tvsencilla.iptv.data.xtream.dto.XtreamSeriesInfoDto
import com.tvsencilla.iptv.data.xtream.dto.XtreamVodStreamDto
import retrofit2.http.GET
import retrofit2.http.Url

/**
 * Every call carries an absolute URL because the server host is the user's own provider and is
 * only known at runtime.
 */
interface XtreamApi {

    @GET
    suspend fun handshake(@Url url: String): XtreamHandshakeDto

    @GET
    suspend fun liveCategories(@Url url: String): List<XtreamCategoryDto>

    @GET
    suspend fun liveStreams(@Url url: String): List<XtreamLiveStreamDto>

    @GET
    suspend fun vodCategories(@Url url: String): List<XtreamCategoryDto>

    @GET
    suspend fun vodStreams(@Url url: String): List<XtreamVodStreamDto>

    @GET
    suspend fun seriesCategories(@Url url: String): List<XtreamCategoryDto>

    @GET
    suspend fun seriesList(@Url url: String): List<XtreamSeriesDto>

    @GET
    suspend fun seriesInfo(@Url url: String): XtreamSeriesInfoDto
}
