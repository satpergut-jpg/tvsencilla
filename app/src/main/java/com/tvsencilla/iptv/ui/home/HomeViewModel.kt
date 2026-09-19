package com.tvsencilla.iptv.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tvsencilla.iptv.BuildConfig
import com.tvsencilla.iptv.domain.model.ContinueWatchingItem
import com.tvsencilla.iptv.domain.repository.ChannelRepository
import com.tvsencilla.iptv.domain.repository.EpgRepository
import com.tvsencilla.iptv.domain.repository.SettingsRepository
import com.tvsencilla.iptv.domain.repository.SourceRepository
import com.tvsencilla.iptv.domain.repository.VodRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val showMovies: Boolean = false,
    val showSeries: Boolean = false,
    val continueWatching: List<ContinueWatchingItem> = emptyList(),
    /** Set when the user asked to land straight on the last channel watched. */
    val jumpToChannelId: String? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val channelRepository: ChannelRepository,
    private val vodRepository: VodRepository,
    private val epgRepository: EpgRepository,
    sourceRepository: SourceRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private var jumpConsumed = false

    val state: StateFlow<HomeUiState> = combine(
        sourceRepository.capabilities,
        vodRepository.observeContinueWatching(),
        settingsRepository.settings,
    ) { capabilities, continueWatching, settings ->
        HomeUiState(
            showMovies = capabilities.supportsMovies && !BuildConfig.SOLO_DIRECTO,
            showSeries = capabilities.supportsSeries && !BuildConfig.SOLO_DIRECTO,
            continueWatching = if (BuildConfig.SOLO_DIRECTO) emptyList() else continueWatching,
            jumpToChannelId = settings.lastChannelId
                ?.takeIf { settings.startOnLastChannel && !jumpConsumed },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    init {
        // Warm the caches in the background so the menus are instant when they are opened.
        viewModelScope.launch { runCatching { channelRepository.refresh() } }
        if (!BuildConfig.SOLO_DIRECTO) {
            viewModelScope.launch { runCatching { vodRepository.refresh() } }
        }
        viewModelScope.launch { runCatching { epgRepository.refresh() } }
    }

    fun onJumpConsumed() {
        jumpConsumed = true
    }
}
