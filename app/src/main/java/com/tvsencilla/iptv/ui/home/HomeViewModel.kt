package com.tvsencilla.iptv.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tvsencilla.iptv.BuildConfig
import com.tvsencilla.iptv.domain.model.Channel
import com.tvsencilla.iptv.domain.model.ContinueWatchingItem
import com.tvsencilla.iptv.domain.model.EpgProgram
import com.tvsencilla.iptv.domain.repository.ChannelRepository
import com.tvsencilla.iptv.domain.repository.EpgRepository
import com.tvsencilla.iptv.domain.repository.SettingsRepository
import com.tvsencilla.iptv.domain.repository.SourceRepository
import com.tvsencilla.iptv.domain.repository.VodRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Un canal para la fila "Directos ahora", con lo que emite en este momento. */
data class LiveNowItem(val channel: Channel, val now: EpgProgram?)

/**
 * Los canales en directo agrupados por categoría ("Fútbol", "Baloncesto"…) para sus propias filas.
 * [categoryName] es null cuando el canal no tiene categoría; la UI decide qué título ponerle.
 */
data class LiveNowGroup(val categoryName: String?, val items: List<LiveNowItem>)

data class HomeUiState(
    val showMovies: Boolean = false,
    val showSeries: Boolean = false,
    val continueWatching: List<ContinueWatchingItem> = emptyList(),
    val liveNow: List<LiveNowGroup> = emptyList(),
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

    /**
     * Solo los canales que ahora mismo emiten algo entran en la fila; el resto no aparece. Se
     * agrupan por categoría para que "Fútbol", "Baloncesto"… tengan su propia fila.
     */
    private val liveNow: Flow<List<LiveNowGroup>> = epgRepository.observeLiveNow()
        .map { matches ->
            matches
                .groupBy { it.channel.categoryName?.trim()?.takeUnless(String::isEmpty) }
                .map { (categoryName, group) ->
                    LiveNowGroup(
                        categoryName = categoryName,
                        items = group.take(LIVE_NOW_CARDS_PER_CATEGORY).map { LiveNowItem(it.channel, it.program) },
                    )
                }
                .sortedWith(compareBy(nullsLast()) { it.categoryName })
        }

    val state: StateFlow<HomeUiState> = combine(
        sourceRepository.capabilities,
        vodRepository.observeContinueWatching(),
        settingsRepository.settings,
        liveNow,
    ) { capabilities, continueWatching, settings, live ->
        HomeUiState(
            showMovies = capabilities.supportsMovies && !BuildConfig.SOLO_DIRECTO,
            showSeries = capabilities.supportsSeries && !BuildConfig.SOLO_DIRECTO,
            continueWatching = if (BuildConfig.SOLO_DIRECTO) emptyList() else continueWatching,
            liveNow = if (BuildConfig.SOLO_DIRECTO) emptyList() else live,
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

private const val LIVE_NOW_CARDS_PER_CATEGORY = 12
