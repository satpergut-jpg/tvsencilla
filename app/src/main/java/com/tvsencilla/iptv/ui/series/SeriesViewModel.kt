package com.tvsencilla.iptv.ui.series

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tvsencilla.iptv.domain.model.Category
import com.tvsencilla.iptv.domain.model.Series
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

data class SeriesUiState(
    val isLoading: Boolean = true,
    @StringRes val errorMessage: Int? = null,
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: String? = null,
    val series: List<Series> = emptyList(),
    val hasLikes: Boolean = false,
    /** El catálogo entero, no la categoría elegida: una categoría vacía no debe ocultar las demás. */
    val catalogIsEmpty: Boolean = true,
) {
    val showingLiked: Boolean get() = selectedCategoryId == LIKED_CATEGORY_ID
}

@HiltViewModel
class SeriesViewModel @Inject constructor(
    private val vodRepository: VodRepository,
) : ViewModel() {

    private val selectedCategoryId = MutableStateFlow<String?>(null)
    private val loadError = MutableStateFlow<Int?>(null)
    private val isRefreshing = MutableStateFlow(true)

    val state: StateFlow<SeriesUiState> = combine(
        vodRepository.observeSeries(),
        vodRepository.observeLikedIds(),
        selectedCategoryId,
        loadError,
        isRefreshing,
    ) { series, likedIds, categoryId, error, refreshing ->
        val categories = series
            .mapNotNull { item -> item.categoryId?.let { it to item.categoryName } }
            .distinctBy { it.first }
            .mapNotNull { (id, name) -> name?.let { Category(id = id, name = it) } }
            .sortedBy { it.name }

        SeriesUiState(
            isLoading = refreshing && series.isEmpty(),
            errorMessage = error.takeIf { series.isEmpty() },
            categories = categories,
            selectedCategoryId = categoryId,
            series = when (categoryId) {
                null -> series
                LIKED_CATEGORY_ID -> series.filter { it.id in likedIds }
                else -> series.filter { it.categoryId == categoryId }
            },
            hasLikes = series.any { it.id in likedIds },
            catalogIsEmpty = series.isEmpty(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SeriesUiState(),
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
