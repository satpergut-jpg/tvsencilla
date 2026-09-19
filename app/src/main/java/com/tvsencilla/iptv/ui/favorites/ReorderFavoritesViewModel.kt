package com.tvsencilla.iptv.ui.favorites

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tvsencilla.iptv.R
import com.tvsencilla.iptv.domain.favorites.FavoritesOrder
import com.tvsencilla.iptv.domain.model.Channel
import com.tvsencilla.iptv.domain.repository.ChannelRepository
import com.tvsencilla.iptv.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReorderUiState(
    val favorites: List<Channel> = emptyList(),
    /** The channel the user has picked up with OK and is now moving with up/down. */
    val grabbedChannelId: String? = null,
    val isUnlocked: Boolean = false,
    @StringRes val pinError: Int? = null,
    val numberEntryFor: Channel? = null,
) {
    val grabbedChannel: Channel? get() = favorites.firstOrNull { it.id == grabbedChannelId }
}

@HiltViewModel
class ReorderFavoritesViewModel @Inject constructor(
    private val channelRepository: ChannelRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val grabbedChannelId = MutableStateFlow<String?>(null)
    private val unlocked = MutableStateFlow(false)
    private val pinError = MutableStateFlow<Int?>(null)
    private val numberEntryFor = MutableStateFlow<Channel?>(null)

    val state: StateFlow<ReorderUiState> = combine(
        channelRepository.observeFavorites(),
        grabbedChannelId,
        unlocked,
        pinError,
        numberEntryFor,
    ) { favorites, grabbed, isUnlocked, error, numberEntry ->
        ReorderUiState(
            favorites = favorites,
            grabbedChannelId = grabbed,
            isUnlocked = isUnlocked,
            pinError = error,
            numberEntryFor = numberEntry,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ReorderUiState(),
    )

    init {
        // Outside simple mode there is nothing to unlock.
        viewModelScope.launch {
            unlocked.value = !settingsRepository.current().simpleMode
        }
    }

    fun submitPin(pin: String) {
        viewModelScope.launch {
            if (pin == settingsRepository.current().pin) {
                unlocked.value = true
                pinError.value = null
            } else {
                pinError.value = R.string.pin_wrong
            }
        }
    }

    /** OK picks a channel up, and OK again puts it down where it now sits. */
    fun toggleGrab(channelId: String) {
        grabbedChannelId.update { if (it == channelId) null else channelId }
    }

    fun releaseGrab() {
        grabbedChannelId.value = null
    }

    fun moveGrabbedUp() = moveGrabbed(up = true)

    fun moveGrabbedDown() = moveGrabbed(up = false)

    private fun moveGrabbed(up: Boolean) {
        val channelId = grabbedChannelId.value ?: return
        viewModelScope.launch {
            val ids = currentOrder()
            val reordered = if (up) {
                FavoritesOrder.moveUp(ids, channelId)
            } else {
                FavoritesOrder.moveDown(ids, channelId)
            }
            if (reordered != ids) channelRepository.saveFavoriteOrder(reordered)
        }
    }

    fun askForNumber(channel: Channel) {
        numberEntryFor.value = channel
    }

    fun dismissNumberEntry() {
        numberEntryFor.value = null
    }

    fun setPosition(channelId: String, position: Int) {
        viewModelScope.launch {
            val ids = currentOrder()
            val target = position.coerceIn(1, ids.size)
            channelRepository.saveFavoriteOrder(FavoritesOrder.setPosition(ids, channelId, target))
            numberEntryFor.value = null
            grabbedChannelId.value = null
        }
    }

    fun remove(channelId: String) {
        viewModelScope.launch {
            channelRepository.removeFavorite(channelId)
            if (grabbedChannelId.value == channelId) grabbedChannelId.value = null
        }
    }

    private suspend fun currentOrder(): List<String> =
        channelRepository.observeFavorites().first().map { it.id }
}
