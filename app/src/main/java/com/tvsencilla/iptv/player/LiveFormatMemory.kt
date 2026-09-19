package com.tvsencilla.iptv.player

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Recuerda qué formato de directo funciona con el proveedor (HLS `.m3u8` o MPEG-TS `.ts`).
 *
 * Hay paneles que anuncian HLS pero sirven TS por esa misma dirección, así que el primer canal
 * falla y hay que probar el otro formato. Sin esta memoria, eso se repetía en cada cambio de
 * canal y cada zapping perdía un par de segundos.
 */
@Singleton
class LiveFormatMemory @Inject constructor() {

    @Volatile
    private var workingExtension: String? = null

    /** La dirección del canal con el formato que ya se sabe que funciona. */
    fun adapt(url: String): String = workingExtension?.let { withLiveExtension(url, it) } ?: url

    /** Se llama cuando un canal por fin se está viendo. */
    fun rememberWorking(url: String) {
        liveExtensionOf(url)?.let { workingExtension = it }
    }
}

private const val HLS = "m3u8"
private const val TS = "ts"

fun liveExtensionOf(url: String): String? = when {
    url.endsWith(".$HLS") -> HLS
    url.endsWith(".$TS") -> TS
    else -> null
}

/** Cambia la extensión de un directo; si la dirección no tiene ninguna conocida, no la toca. */
fun withLiveExtension(url: String, extension: String): String {
    val current = liveExtensionOf(url) ?: return url
    return url.removeSuffix(".$current") + ".$extension"
}

/** El otro formato: `.m3u8` <-> `.ts`. Null si la dirección no tiene ninguno de los dos. */
fun alternateLiveUrl(url: String): String? = when (liveExtensionOf(url)) {
    HLS -> withLiveExtension(url, TS)
    TS -> withLiveExtension(url, HLS)
    else -> null
}
