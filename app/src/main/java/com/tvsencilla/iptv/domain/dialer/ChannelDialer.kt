package com.tvsencilla.iptv.domain.dialer

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Accumulates the digits typed on the remote and decides when they become a channel change.
 *
 * Deliberately free of any UI or Android dependency so the timing rules can be unit tested with
 * virtual time. Resolving a number to an actual channel is [ChannelNumbering]'s job.
 */
class ChannelDialer(
    private val scope: CoroutineScope,
    private val timeoutMillis: Long = DIGIT_TIMEOUT_MILLIS,
    private val maxDigits: Int = MAX_DIGITS,
) {

    private val _typed = MutableStateFlow<String?>(null)

    /** Digits typed so far, or null when the user is not dialling. Render it as "12_". */
    val typed: StateFlow<String?> = _typed.asStateFlow()

    private val _commits = MutableSharedFlow<Int>(extraBufferCapacity = 8)

    /** Emits the number the user settled on, either by timeout, by OK, or at [maxDigits]. */
    val commits: SharedFlow<Int> = _commits.asSharedFlow()

    private var timer: Job? = null

    val isDialling: Boolean get() = _typed.value != null

    fun onDigit(digit: Int) {
        require(digit in 0..9) { "not a digit: $digit" }
        val next = _typed.value.orEmpty() + digit
        _typed.value = next
        if (next.length >= maxDigits) commit() else restartTimer()
    }

    /** OK on the remote tunes straight away without waiting out the timeout. */
    fun onConfirm(): Boolean {
        if (!isDialling) return false
        commit()
        return true
    }

    /** Back on the remote abandons the number and leaves the current channel alone. */
    fun onCancel(): Boolean {
        if (!isDialling) return false
        timer?.cancel()
        timer = null
        _typed.value = null
        return true
    }

    private fun restartTimer() {
        timer?.cancel()
        timer = scope.launch {
            delay(timeoutMillis)
            commit()
        }
    }

    private fun commit() {
        timer?.cancel()
        timer = null
        val number = _typed.value?.toIntOrNull()
        _typed.value = null
        if (number != null) _commits.tryEmit(number)
    }

    companion object {
        const val DIGIT_TIMEOUT_MILLIS = 2_000L
        const val MAX_DIGITS = 3
    }
}
