package com.tvsencilla.iptv.ui.series

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tvsencilla.iptv.domain.model.Episode
import com.tvsencilla.iptv.domain.model.Season
import com.tvsencilla.iptv.domain.model.Series
import com.tvsencilla.iptv.domain.repository.VodRepository
import com.tvsencilla.iptv.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SeriesDetailUiState(
    val series: Series? = null,
    val selectedSeasonNumber: Int? = null,
    val watchedEpisodeIds: Set<String> = emptySet(),
    val isLoading: Boolean = true,
    val isLiked: Boolean = false,
) {
    val seasons: List<Season> get() = series?.seasons.orEmpty()

    val episodes: List<Episode>
        get() = seasons.firstOrNull { it.number == selectedSeasonNumber }?.episodes.orEmpty()
}

@HiltViewModel
class SeriesDetailViewModel @Inject constructor(
    private val vodRepository: VodRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val seriesId: String = requireNotNull(savedStateHandle[Routes.ARG_SERIES_ID])

    private val series = MutableStateFlow<Series?>(null)
    private val selectedSeason = MutableStateFlow<Int?>(null)
    private val isLoading = MutableStateFlow(true)

    val state: StateFlow<SeriesDetailUiState> = combine(
        series,
        selectedSeason,
        vodRepository.observeWatchedIds(),
        vodRepository.observeLikedIds(),
        isLoading,
    ) { detail, season, watched, liked, loading ->
        SeriesDetailUiState(
            series = detail,
            selectedSeasonNumber = season ?: detail?.seasons?.firstOrNull()?.number,
            watchedEpisodeIds = watched,
            isLoading = loading,
            isLiked = seriesId in liked,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SeriesDetailUiState(),
    )

    init {
        viewModelScope.launch {
            series.value = vodRepository.seriesDetail(seriesId)
            isLoading.value = false
        }
    }

    fun selectSeason(number: Int) {
        selectedSeason.value = number
    }

    fun toggleLike() {
        viewModelScope.launch {
            vodRepository.setLiked(seriesId, isSeries = true, liked = !state.value.isLiked)
        }
    }
}
