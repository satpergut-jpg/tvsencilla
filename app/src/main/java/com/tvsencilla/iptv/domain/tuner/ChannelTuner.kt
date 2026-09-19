package com.tvsencilla.iptv.domain.tuner

import com.tvsencilla.iptv.domain.dialer.ChannelDialer
import com.tvsencilla.iptv.domain.dialer.ChannelNumbering
import com.tvsencilla.iptv.domain.model.Channel
import com.tvsencilla.iptv.domain.repository.ChannelRepository
import com.tvsencilla.iptv.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Joins the digit accumulator to whichever numbering is active, and answers CH+/CH-. The channel
 * list, the favourites list and the player all drive the same object, so typing "5" means the
 * same thing everywhere.
 */
class ChannelTuner(
    scope: CoroutineScope,
    channelRepository: ChannelRepository,
    settingsRepository: SettingsRepository,
) {

    private val dialer = ChannelDialer(scope)

    /** Digits typed so far, for the on-screen "12_" badge. */
    val dialledDigits: StateFlow<String?> = dialer.typed

    private val _tuneTo = MutableSharedFlow<Channel>(extraBufferCapacity = 4)
    val tuneTo: SharedFlow<Channel> = _tuneTo.asSharedFlow()

    private val _numberNotFound = MutableSharedFlow<Int>(extraBufferCapacity = 4)
    val numberNotFound: SharedFlow<Int> = _numberNotFound.asSharedFlow()

    @Volatile
    private var numbering: ChannelNumbering = ChannelNumbering(emptyList()) { 0 }

    init {
        scope.launch {
            combine(
                settingsRepository.settings,
                channelRepository.observeChannels(),
                channelRepository.observeFavorites(),
            ) { settings, all, favorites ->
                ChannelNumbering.forMode(settings.numberingMode, all, favorites)
            }.collect { numbering = it }
        }

        scope.launch {
            dialer.commits.collect { number ->
                val channel = numbering.channelAt(number)
                if (channel != null) _tuneTo.emit(channel) else _numberNotFound.emit(number)
            }
        }
    }

    fun onDigit(digit: Int) = dialer.onDigit(digit)

    fun onConfirm(): Boolean = dialer.onConfirm()

    fun onCancel(): Boolean = dialer.onCancel()

    val isDialling: Boolean get() = dialer.isDialling

    fun channelUp(currentChannelId: String?): Channel? = numbering.next(currentChannelId)

    fun channelDown(currentChannelId: String?): Channel? = numbering.previous(currentChannelId)

    /** The number the remote responds to for this channel, or null if it has none. */
    fun numberFor(channel: Channel): Int? = numbering.numberFor(channel)
}

@Singleton
class ChannelTunerFactory @Inject constructor(
    private val channelRepository: ChannelRepository,
    private val settingsRepository: SettingsRepository,
) {
    fun create(scope: CoroutineScope): ChannelTuner =
        ChannelTuner(scope, channelRepository, settingsRepository)
}
