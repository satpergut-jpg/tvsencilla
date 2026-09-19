package com.tvsencilla.iptv.ui.movies

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tvsencilla.iptv.domain.model.Category
import com.tvsencilla.iptv.domain.model.Movie
import com.tvsencilla.iptv.domain.repository.VodRepository
import com.tvsencilla.iptv.ui.components.LIKED_CATEGORY_ID
import com.tvsencilla.iptv.ui.util.userMessageRes
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MoviesUiState(
    val isLoading: Boolean = true,
    @StringRes val errorMessage: Int? = null,
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: String? = null,
    val movies: List<Movie> = emptyList(),
    val hasLikes: Boolean = false,
    /** El catálogo entero, no la categoría elegida: una categoría vacía no debe ocultar las demás. */
    val catalogIsEmpty: Boolean = true,
) {
    val showingLiked: Boolean get() = selectedCategoryId == LIKED_CATEGORY_ID
}

@HiltViewModel
class MoviesViewModel @Inject constructor(
    private val vodRepository: VodRepository,
) : ViewModel() {

    private val selectedCategoryId = MutableStateFlow<String?>(null)
    private val loadError = MutableStateFlow<Int?>(null)
    private val isRefreshing = MutableStateFlow(true)

    val state: StateFlow<MoviesUiState> = combine(
        vodRepository.observeMovies(),
        vodRepository.observeLikedIds(),
        selectedCategoryId,
        loadError,
        isRefreshing,
    ) { movies, likedIds, categoryId, error, refreshing ->
        // Categories come from the catalogue itself; the provider does not always list them.
        val categories = movies
            .mapNotNull { movie -> movie.categoryId?.let { it to movie.categoryName } }
            .distinctBy { it.first }
            .mapNotNull { (id, name) -> name?.let { Category(id = id, name = it) } }
            .sortedBy { it.name }

        MoviesUiState(
            isLoading = refreshing && movies.isEmpty(),
            errorMessage = error.takeIf { movies.isEmpty() },
            categories = categories,
            selectedCategoryId = categoryId,
            movies = when (categoryId) {
                null -> movies
                LIKED_CATEGORY_ID -> movies.filter { it.id in likedIds }
                else -> movies.filter { it.categoryId == categoryId }
            },
            hasLikes = movies.any { it.id in likedIds },
            catalogIsEmpty = movies.isEmpty(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MoviesUiState(),
    )

    init {
        refresh()
    }

    fun refresh(force: Boolean = false) {
        viewModelScope.launch {
            isRefreshing.value = true
            loadError.value = null
            runCatching { vodRepository.refresh(force) }
                .onFailure { loadError.value = it.userMessageRes() }
            isRefreshing.value = false
        }
    }

    fun selectCategory(categoryId: String?) {
        selectedCategoryId.value = categoryId
    }
}
