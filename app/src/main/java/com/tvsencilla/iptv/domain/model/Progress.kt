package com.tvsencilla.iptv.domain.model

enum class PlayableKind { MOVIE, EPISODE }

data class WatchProgress(
    val itemId: String,
    val kind: PlayableKind,
    val positionMillis: Long,
    val durationMillis: Long,
    val updatedAtMillis: Long,
) {
    val fraction: Float
        get() = if (durationMillis <= 0L) 0f
        else (positionMillis.toFloat() / durationMillis).coerceIn(0f, 1f)

    /** Nearly finished items should not clutter "Keep watching". */
    val isFinished: Boolean get() = fraction >= 0.95f

    val isWorthResuming: Boolean get() = !isFinished && positionMillis > 60_000L
}

data class ContinueWatchingItem(
    val itemId: String,
    val kind: PlayableKind,
    val title: String,
    val subtitle: String? = null,
    val posterUrl: String? = null,
    val streamUrl: String,
    val positionMillis: Long,
    val durationMillis: Long,
) {
    val fraction: Float
        get() = if (durationMillis <= 0L) 0f
        else (positionMillis.toFloat() / durationMillis).coerceIn(0f, 1f)
}
