package com.tvsencilla.iptv.ui.navigation

import android.net.Uri

object Routes {
    const val SETUP = "setup"
    const val HOME = "home"
    const val LIVE = "live"
    const val REORDER_FAVORITES = "favorites/reorder"
    const val MOVIES = "movies"
    const val SERIES = "series"
    const val SEARCH = "search"
    const val SETTINGS = "settings"
    const val PARENTAL = "settings/parental"

    private const val LIVE_PLAYER_BASE = "player/live"
    private const val MOVIE_DETAIL_BASE = "movie"
    private const val SERIES_DETAIL_BASE = "series/detail"
    private const val VOD_PLAYER_BASE = "player/vod"
    private const val EPG_GRID_BASE = "epg"

    const val LIVE_PLAYER = "$LIVE_PLAYER_BASE/{channelId}?fromSearch={fromSearch}"
    const val MOVIE_DETAIL = "$MOVIE_DETAIL_BASE/{movieId}"
    const val SERIES_DETAIL = "$SERIES_DETAIL_BASE/{seriesId}"
    const val VOD_PLAYER = "$VOD_PLAYER_BASE/{kind}/{itemId}?resume={resume}"
    const val EPG_GRID = "$EPG_GRID_BASE/{channelId}"

    const val ARG_CHANNEL_ID = "channelId"
    const val ARG_MOVIE_ID = "movieId"
    const val ARG_SERIES_ID = "seriesId"
    const val ARG_ITEM_ID = "itemId"
    const val ARG_KIND = "kind"
    const val ARG_RESUME = "resume"
    const val ARG_FROM_SEARCH = "fromSearch"

    const val KIND_MOVIE = "movie"
    const val KIND_EPISODE = "episode"

    fun livePlayer(channelId: String, fromSearch: Boolean = false) =
        "$LIVE_PLAYER_BASE/${channelId.encoded()}?$ARG_FROM_SEARCH=$fromSearch"

    fun movieDetail(movieId: String) = "$MOVIE_DETAIL_BASE/${movieId.encoded()}"

    fun seriesDetail(seriesId: String) = "$SERIES_DETAIL_BASE/${seriesId.encoded()}"

    fun vodPlayer(kind: String, itemId: String, resume: Boolean) =
        "$VOD_PLAYER_BASE/$kind/${itemId.encoded()}?resume=$resume"

    fun epgGrid(channelId: String) = "$EPG_GRID_BASE/${channelId.encoded()}"

    /** Provider ids contain colons and slashes, so they have to survive the route path. */
    private fun String.encoded(): String = Uri.encode(this)
}
