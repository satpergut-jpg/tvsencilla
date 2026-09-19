package com.tvsencilla.iptv.ui.epg

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tvsencilla.iptv.domain.model.Channel
import com.tvsencilla.iptv.domain.model.EpgProgram
import com.tvsencilla.iptv.domain.repository.ChannelRepository
import com.tvsencilla.iptv.domain.repository.EpgRepository
import com.tvsencilla.iptv.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

data class EpgGridUiState(
    val channels: List<Channel> = emptyList(),
    val selectedChannel: Channel? = null,
    val schedule: List<EpgProgram> = emptyList(),
)

@HiltViewModel
class EpgGridViewModel @Inject constructor(
    channelRepository: ChannelRepository,
    private val epgRepository: EpgRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val requestedChannelId: String? = savedStateHandle[Routes.ARG_CHANNEL_ID]
    private val selectedChannelId = MutableStateFlow(requestedChannelId)

    private val channels = channelRepository.observeChannels()

    private val selectedChannel: StateFlow<Channel?> =
        combine(channels, selectedChannelId) { all, id ->
            all.firstOrNull { it.id == id } ?: all.firstOrNull()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val state: StateFlow<EpgGridUiState> = combine(
        channels,
        selectedChannel,
        selectedChannel.flatMapLatest { channel ->
            val epgId = channel?.epgChannelId
            if (epgId.isNullOrBlank()) {
                flowOf(emptyList())
            } else {
                val now = System.currentTimeMillis()
                epgRepository.observeSchedule(epgId, now - PAST_WINDOW_MILLIS, now + FUTURE_WINDOW_MILLIS)
            }
        },
    ) { allChannels, channel, schedule ->
        EpgGridUiState(channels = allChannels, selectedChannel = channel, schedule = schedule)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EpgGridUiState(),
    )

    fun selectChannel(channelId: String) {
        selectedChannelId.value = channelId
    }

    private companion object {
        const val PAST_WINDOW_MILLIS = 60 * 60 * 1000L
        const val FUTURE_WINDOW_MILLIS = 12 * 60 * 60 * 1000L
    }
}
