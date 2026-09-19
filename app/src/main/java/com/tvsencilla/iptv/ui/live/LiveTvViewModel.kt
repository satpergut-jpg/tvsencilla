package com.tvsencilla.iptv.ui.live

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tvsencilla.iptv.domain.model.Category
import com.tvsencilla.iptv.domain.model.Channel
import com.tvsencilla.iptv.domain.model.NowNext
import com.tvsencilla.iptv.domain.model.displayName
import com.tvsencilla.iptv.domain.repository.ChannelRepository
import com.tvsencilla.iptv.domain.repository.EpgRepository
import com.tvsencilla.iptv.domain.repository.SettingsRepository
import com.tvsencilla.iptv.domain.tuner.ChannelTunerFactory
import com.tvsencilla.iptv.ui.util.userMessageRes
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LiveTvUiState(
    val isLoading: Boolean = true,
    @StringRes val errorMessage: Int? = null,
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: String? = null,
    /** Los canales de la categoría elegida. */
    val channels: List<Channel> = emptyList(),
    /** Todos los canales, favoritos primero: para la lista que se abre encima del vídeo. */
    val allChannels: List<Channel> = emptyList(),
    val hasFavorites: Boolean = false,
    /** La guía en cuadrícula es opcional y se activa en Ajustes. */
    val showFullGuide: Boolean = false,
) {
    val showingFavorites: Boolean get() = selectedCategoryId == FAVORITES_CATEGORY_ID

    /** En Favoritos se ve su propia numeración 1, 2, 3…; en el resto, la de la lista completa. */
    fun numberShownFor(channel: Channel): Int =
        if (showingFavorites) channel.favoriteNumber ?: channel.listNumber else channel.listNumber
}

/** The channel the cursor is sitting on, together with what is on it right now. */
data class FocusedChannel(
    val channel: Channel,
    val nowNext: NowNext = NowNext(),
    val remoteNumber: Int? = null,
)

/** Confirmación que se enseña al añadir o quitar un favorito. */
data class FavoriteNotice(val channelName: String, val addedAsNumber: Int?)

/** Categoría virtual que agrupa los favoritos del usuario. */
const val FAVORITES_CATEGORY_ID = "__favoritos__"

@HiltViewModel
class LiveTvViewModel @Inject constructor(
    private val channelRepository: ChannelRepository,
    private val epgRepository: EpgRepository,
    private val settingsRepository: SettingsRepository,
    tunerFactory: ChannelTunerFactory,
) : ViewModel() {

    val tuner = tunerFactory.create(viewModelScope)

    private val selectedCategoryId = MutableStateFlow<String?>(null)
    private var userChoseCategory = false

    /**
     * Se guarda solo el id, no una copia del canal: así el panel refleja al momento cambios como
     * añadirlo a favoritos.
     */
    private val focusedChannelId = MutableStateFlow<String?>(null)

    /** Para devolver el cursor al canal donde estaba al volver del reproductor. */
    val lastFocusedChannelId: String? get() = focusedChannelId.value

    private val loadError = MutableStateFlow<Int?>(null)
    private val isRefreshing = MutableStateFlow(true)

    private val _notFoundNumber = MutableStateFlow<Int?>(null)
    val notFoundNumber: StateFlow<Int?> = _notFoundNumber.asStateFlow()

    private val _favoriteNotice = MutableStateFlow<FavoriteNotice?>(null)
    val favoriteNotice: StateFlow<FavoriteNotice?> = _favoriteNotice.asStateFlow()

    val state: StateFlow<LiveTvUiState> = combine(
        channelRepository.observeChannels(),
        channelRepository.observeFavorites(),
        channelRepository.observeCategories(),
        selectedCategoryId,
        combine(loadError, isRefreshing, settingsRepository.settings) { error, refreshing, settings ->
            Triple(error, refreshing, settings.showEpgGrid)
        },
    ) { channels, favorites, categories, categoryId, (error, refreshing, showGuide) ->
        val visible = when (categoryId) {
            null -> channels
            FAVORITES_CATEGORY_ID -> favorites
            else -> channels.filter { it.categoryId == categoryId }
        }
        val favoriteIds = favorites.map { it.id }.toSet()
        LiveTvUiState(
            isLoading = refreshing && channels.isEmpty(),
            errorMessage = error.takeIf { channels.isEmpty() },
            categories = categories,
            selectedCategoryId = categoryId,
            channels = visible,
            allChannels = favorites + channels.filterNot { it.id in favoriteIds },
            hasFavorites = favorites.isNotEmpty(),
            showFullGuide = showGuide,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LiveTvUiState(),
    )

    val focused: StateFlow<FocusedChannel?> = combine(
        channelRepository.observeChannels(),
        focusedChannelId,
    ) { channels, id -> channels.firstOrNull { it.id == id } }
        .flatMapLatest { channel ->
            if (channel == null) {
                flowOf(null)
            } else {
                epgRepository.observeNowNext(channel.epgChannelId).map { nowNext ->
                    FocusedChannel(
                        channel = channel,
                        nowNext = nowNext,
                        remoteNumber = tuner.numberFor(channel),
                    )
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    init {
        refresh()
        viewModelScope.launch {
            tuner.numberNotFound.collect { number -> _notFoundNumber.value = number }
        }
        // Quien ya tiene favoritos entra directamente en ellos: es la lista que más usa.
        viewModelScope.launch {
            val favorites = channelRepository.observeFavorites().first()
            if (favorites.isNotEmpty() && !userChoseCategory) {
                selectedCategoryId.value = FAVORITES_CATEGORY_ID
            }
        }
    }

    fun refresh(force: Boolean = false) {
        viewModelScope.launch {
            isRefreshing.value = true
            loadError.value = null
            runCatching { channelRepository.refresh(force) }
                .onFailure { loadError.value = it.userMessageRes() }
            isRefreshing.value = false
        }
    }

    fun selectCategory(categoryId: String?) {
        userChoseCategory = true
        selectedCategoryId.value = categoryId
    }

    fun onChannelFocused(channel: Channel) {
        focusedChannelId.value = channel.id
    }

    fun toggleFavorite(channel: Channel) {
        viewModelScope.launch {
            if (channel.isFavorite) {
                channelRepository.removeFavorite(channel.id)
            } else {
                channelRepository.addFavorite(channel.id)
            }
            // Se relee para saber qué número le ha tocado y decírselo al usuario.
            val updated = channelRepository.channelById(channel.id) ?: return@launch
            showNotice(FavoriteNotice(updated.displayName, updated.favoriteNumber))
        }
    }

    private suspend fun showNotice(notice: FavoriteNotice) {
        _favoriteNotice.value = notice
        delay(NOTICE_VISIBLE_MILLIS)
        _favoriteNotice.update { if (it == notice) null else it }
    }

    fun dismissNotFound() {
        _notFoundNumber.update { null }
    }

    private companion object {
        const val NOTICE_VISIBLE_MILLIS = 8_000L
    }
}
