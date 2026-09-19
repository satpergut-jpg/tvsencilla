package com.tvsencilla.iptv.ui.movies

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tvsencilla.iptv.domain.model.Movie
import com.tvsencilla.iptv.domain.model.WatchProgress
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

data class MovieDetailUiState(
    val movie: Movie? = null,
    val progress: WatchProgress? = null,
    val isLoading: Boolean = true,
    val isLiked: Boolean = false,
) {
    val canResume: Boolean get() = progress?.isWorthResuming == true
}

@HiltViewModel
class MovieDetailViewModel @Inject constructor(
    private val vodRepository: VodRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val movieId: String = requireNotNull(savedStateHandle[Routes.ARG_MOVIE_ID])

    private val loaded = MutableStateFlow(MovieDetailUiState())

    val state: StateFlow<MovieDetailUiState> = combine(
        loaded,
        vodRepository.observeLikedIds(),
    ) { detail, likedIds -> detail.copy(isLiked = movieId in likedIds) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MovieDetailUiState())

    init {
        viewModelScope.launch {
            val movie = vodRepository.movieById(movieId)
            val progress = vodRepository.progressFor(movieId)
            loaded.value = MovieDetailUiState(movie = movie, progress = progress, isLoading = false)
        }
    }

    fun toggleLike() {
        viewModelScope.launch {
            vodRepository.setLiked(movieId, isSeries = false, liked = !state.value.isLiked)
        }
    }
}
