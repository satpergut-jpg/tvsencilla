package com.tvsencilla.iptv.player

import androidx.annotation.StringRes
import androidx.media3.common.PlaybackException
import com.tvsencilla.iptv.R
import java.net.UnknownHostException

/**
 * La frase que ve el usuario cuando un vídeo no arranca. Si el aparato no encuentra el servidor es
 * que no hay internet, y decirlo así le indica qué revisar (el router) en lugar de culpar al canal.
 */
/**
 * El vídeo llegó, pero en un formato que no es el esperado (p. ej. se pidió HLS y llegó MPEG-TS).
 * Es el único caso en que probar el otro formato tiene sentido.
 */
fun PlaybackException.isFormatProblem(): Boolean =
    errorCode in PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED..
        PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED

@StringRes
fun PlaybackException?.userMessageRes(): Int {
    var cause: Throwable? = this
    while (cause != null) {
        if (cause is UnknownHostException) return R.string.error_no_internet
        cause = cause.cause
    }
    return R.string.error_playback
}
