package com.tvsencilla.iptv.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tvsencilla.iptv.BuildConfig
import com.tvsencilla.iptv.domain.model.Channel
import com.tvsencilla.iptv.domain.model.Movie
import com.tvsencilla.iptv.domain.model.ProgramMatch
import com.tvsencilla.iptv.domain.model.Series
import com.tvsencilla.iptv.domain.repository.ChannelRepository
import com.tvsencilla.iptv.domain.repository.EpgRepository
import com.tvsencilla.iptv.domain.repository.SettingsRepository
import com.tvsencilla.iptv.domain.repository.VodRepository
import com.tvsencilla.iptv.domain.search.VoiceTuning
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    /** Lo que se emite ahora o en las próximas horas: es lo que busca quien dice "Real Madrid". */
    val programs: List<ProgramMatch> = emptyList(),
    val channels: List<Channel> = emptyList(),
    val movies: List<Movie> = emptyList(),
    val series: List<Series> = emptyList(),
    val isSearching: Boolean = false,
) {
    val hasQuery: Boolean get() = query.isNotBlank()

    val isEmpty: Boolean
        get() = programs.isEmpty() && channels.isEmpty() && movies.isEmpty() && series.isEmpty()
}

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val channelRepository: ChannelRepository,
    private val epgRepository: EpgRepository,
    private val vodRepository: VodRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    private val queries = MutableStateFlow("")

    private val _tuneTo = MutableSharedFlow<Channel>(extraBufferCapacity = 1)

    /** El canal que hay que poner porque se ha pedido por voz. */
    val tuneTo: SharedFlow<Channel> = _tuneTo.asSharedFlow()

    init {
        viewModelScope.launch {
            // Waiting a beat avoids re-querying on every letter typed with the remote.
            queries.debounce(SEARCH_DEBOUNCE_MILLIS).collect { query -> runSearch(query) }
        }
    }

    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query) }
        queries.value = query
    }

    /**
     * Lo dicho por voz se busca al momento y, si el ajuste está activado y queda claro qué canal
     * se ha pedido, se pone directamente. Al escribir no se hace, porque saltaría de canal a
     * mitad de palabra.
     */
    fun onVoiceResult(spoken: String) {
        onQueryChange(spoken)
        viewModelScope.launch {
            val results = runSearch(spoken)
            if (!settingsRepository.current().voiceAutoTune) return@launch
            val channel = VoiceTuning.pick(
                query = spoken,
                channels = results.channels,
                programs = results.programs,
                nowMillis = System.currentTimeMillis(),
            ) ?: return@launch
            _tuneTo.emit(channel)
        }
    }

    private suspend fun runSearch(query: String): SearchUiState {
        if (query.isBlank()) {
            return _state.updateAndGet {
                it.copy(
                    programs = emptyList(),
                    channels = emptyList(),
                    movies = emptyList(),
                    series = emptyList(),
                    isSearching = false,
                )
            }
        }

        _state.update { it.copy(isSearching = true) }
        val programs = runCatching { epgRepository.searchPrograms(query) }.getOrDefault(emptyList())
        val channels = runCatching { channelRepository.search(query) }.getOrDefault(emptyList())
        val movies = if (BuildConfig.SOLO_DIRECTO) emptyList()
        else runCatching { vodRepository.searchMovies(query) }.getOrDefault(emptyList())
        val series = if (BuildConfig.SOLO_DIRECTO) emptyList()
        else runCatching { vodRepository.searchSeries(query) }.getOrDefault(emptyList())
        return _state.updateAndGet {
            it.copy(
                programs = programs,
                channels = channels,
                movies = movies,
                series = series,
                isSearching = false,
            )
        }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MILLIS = 350L
    }
}
