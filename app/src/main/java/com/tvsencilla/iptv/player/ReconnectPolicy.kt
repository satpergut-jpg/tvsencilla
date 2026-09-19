package com.tvsencilla.iptv.player

/**
 * Backoff for a stream that drops. Live IPTV sources fail often and briefly, so the app retries
 * by itself with a widening gap rather than showing the user an error straight away.
 */
class ReconnectPolicy(
    private val maxAttempts: Int = MAX_ATTEMPTS,
    private val firstDelayMillis: Long = FIRST_DELAY_MILLIS,
) {

    var attempts: Int = 0
        private set

    val hasGivenUp: Boolean get() = attempts >= maxAttempts

    fun reset() {
        attempts = 0
    }

    /** Null once the retries are exhausted, which is when the user finally sees an error. */
    fun nextDelayMillis(): Long? {
        if (hasGivenUp) return null
        val delay = firstDelayMillis shl attempts
        attempts++
        return delay
    }

    private companion object {
        const val MAX_ATTEMPTS = 4

        /**
         * Medio segundo al principio: un corte de directo suele recuperarse solo al instante, y
         * esperar más deja al usuario mirando "Reconectando…" sin motivo. Los cuatro intentos
         * suman unos 7 segundos antes de dar el aviso de error.
         */
        const val FIRST_DELAY_MILLIS = 500L
    }
}
