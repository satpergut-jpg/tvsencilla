package com.tvsencilla.iptv.player

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.OkHttpClient

@Singleton
class PlayerFactory @Inject constructor(
    @ApplicationContext private val context: Context,
    private val client: OkHttpClient,
) {

    fun create(preferredAudioLanguage: String, preferredSubtitleLanguage: String): ExoPlayer {
        val httpDataSourceFactory = OkHttpDataSource.Factory(client)
            .setUserAgent(USER_AGENT)

        val dataSourceFactory: DataSource.Factory =
            DefaultDataSource.Factory(context, httpDataSourceFactory)

        val trackSelector = DefaultTrackSelector(context).apply {
            parameters = buildUponParameters()
                .setPreferredAudioLanguage(preferredAudioLanguage)
                .setPreferredTextLanguage(preferredSubtitleLanguage)
                .build()
        }

        // Por defecto el reproductor reintenta cada petición fallida varias veces por su cuenta
        // antes de avisar, y eso multiplicaba la espera en "Reconectando…". Con un solo reintento
        // interno, el fallo llega pronto a ReconnectPolicy, que decide con esperas más cortas.
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)
            .setLoadErrorHandlingPolicy(DefaultLoadErrorHandlingPolicy(INTERNAL_RETRIES))

        return ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setTrackSelector(trackSelector)
            .setLoadControl(zappingLoadControl())
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true,
            )
            .build()
            .apply { setWakeMode(C.WAKE_MODE_NETWORK) }
    }

    /**
     * Tuned for channel hopping rather than for smoothness: playback starts after half a second
     * of media, which is what keeps a channel change under two seconds.
     */
    private fun zappingLoadControl() = DefaultLoadControl.Builder()
        .setBufferDurationsMs(
            /* minBufferMs = */ 2_000,
            /* maxBufferMs = */ 20_000,
            /* bufferForPlaybackMs = */ 500,
            /* bufferForPlaybackAfterRebufferMs = */ 1_500,
        )
        .setPrioritizeTimeOverSizeThresholds(true)
        .build()

    private companion object {
        /**
         * Muchos paneles IPTV rechazan agentes de usuario que no conocen, así que se anuncia el
         * de VLC, que es el que todos aceptan.
         */
        const val USER_AGENT = "VLC/3.0.20 LibVLC/3.0.20"
        const val INTERNAL_RETRIES = 1
    }
}
