package com.tvsencilla.iptv.ui.settings

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tvsencilla.iptv.R
import com.tvsencilla.iptv.data.local.AppDatabase
import com.tvsencilla.iptv.domain.model.AppSettings
import com.tvsencilla.iptv.domain.model.Category
import com.tvsencilla.iptv.domain.model.FontSizeOption
import com.tvsencilla.iptv.domain.model.NumberingMode
import com.tvsencilla.iptv.domain.model.SearchInputMode
import com.tvsencilla.iptv.domain.model.SubtitleSizeOption
import com.tvsencilla.iptv.domain.repository.ChannelRepository
import com.tvsencilla.iptv.domain.repository.EpgRepository
import com.tvsencilla.iptv.domain.repository.SettingsRepository
import com.tvsencilla.iptv.domain.repository.SourceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Which action is waiting behind the PIN prompt. */
enum class PinPurpose { LEAVE_SIMPLE_MODE, OPEN_PARENTAL, CHANGE_PIN, CHANGE_SOURCE }

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val categories: List<Category> = emptyList(),
    val pinPurpose: PinPurpose? = null,
    @StringRes val pinError: Int? = null,
    @StringRes val notice: Int? = null,
    val isChangingPin: Boolean = false,
    val sourceCleared: Boolean = false,
) {
    /** In simple mode the advanced half of the screen is not shown at all. */
    val showAdvanced: Boolean get() = !settings.simpleMode
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val sourceRepository: SourceRepository,
    private val channelRepository: ChannelRepository,
    private val epgRepository: EpgRepository,
    private val database: AppDatabase,
) : ViewModel() {

    /** The parts of the screen state that are not persisted anywhere. */
    private data class Transient(
        val pinPurpose: PinPurpose? = null,
        @StringRes val pinError: Int? = null,
        @StringRes val notice: Int? = null,
        val isChangingPin: Boolean = false,
    )

    private val transient = MutableStateFlow(Transient())

    private val _sourceCleared = MutableStateFlow(false)
    val sourceCleared: StateFlow<Boolean> = _sourceCleared.asStateFlow()

    val state: StateFlow<SettingsUiState> = combine(
        settingsRepository.settings,
        channelRepository.observeCategories(),
        transient,
    ) { settings, categories, extra ->
        SettingsUiState(
            settings = settings,
            categories = categories,
            pinPurpose = extra.pinPurpose,
            pinError = extra.pinError,
            notice = extra.notice,
            isChangingPin = extra.isChangingPin,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(),
    )

    fun setFontSize(option: FontSizeOption) = update { it.copy(fontSize = option) }

    fun setSubtitleSize(option: SubtitleSizeOption) = update { it.copy(subtitleSize = option) }

    fun setNumberingMode(mode: NumberingMode) = update { it.copy(numberingMode = mode) }

    fun setStartOnLastChannel(enabled: Boolean) = update { it.copy(startOnLastChannel = enabled) }

    fun setShowEpgGrid(enabled: Boolean) = update { it.copy(showEpgGrid = enabled) }

    fun setVoiceAutoTune(enabled: Boolean) = update { it.copy(voiceAutoTune = enabled) }

    fun setSearchInputMode(mode: SearchInputMode) = update { it.copy(searchInputMode = mode) }

    fun setAudioLanguage(language: String) = update { it.copy(preferredAudioLanguage = language) }

    fun setSubtitleLanguage(language: String) = update { it.copy(preferredSubtitleLanguage = language) }

    fun toggleHiddenCategory(categoryId: String) = update { settings ->
        val hidden = settings.hiddenCategoryIds
        settings.copy(
            hiddenCategoryIds = if (categoryId in hidden) hidden - categoryId else hidden + categoryId,
        )
    }

    /** Leaving simple mode is the one switch that is deliberately hard to flip by accident. */
    fun requestSimpleModeOff() = askForPin(PinPurpose.LEAVE_SIMPLE_MODE)

    fun enableSimpleMode() = update { it.copy(simpleMode = true) }

    fun requestParental() = askForPin(PinPurpose.OPEN_PARENTAL)

    fun requestChangePin() = askForPin(PinPurpose.CHANGE_PIN)

    fun requestChangeSource() = askForPin(PinPurpose.CHANGE_SOURCE)

    private fun askForPin(purpose: PinPurpose) {
        transient.update { it.copy(pinPurpose = purpose, pinError = null) }
    }

    fun dismissPin() {
        transient.update { it.copy(pinPurpose = null, pinError = null, isChangingPin = false) }
    }

    /** Hands the unlocked purpose back, so the screen can route onwards. */
    fun submitPin(pin: String, onUnlocked: (PinPurpose) -> Unit) {
        viewModelScope.launch {
            val purpose = transient.value.pinPurpose ?: return@launch
            if (pin != settingsRepository.current().pin) {
                transient.update { it.copy(pinError = R.string.pin_wrong) }
                return@launch
            }

            transient.update {
                it.copy(
                    pinPurpose = null,
                    pinError = null,
                    isChangingPin = purpose == PinPurpose.CHANGE_PIN,
                )
            }
            if (purpose == PinPurpose.LEAVE_SIMPLE_MODE) update { it.copy(simpleMode = false) }
            onUnlocked(purpose)
        }
    }

    fun saveNewPin(pin: String) {
        if (pin.length != PIN_LENGTH) {
            transient.update { it.copy(pinError = R.string.pin_mismatch) }
            return
        }
        viewModelScope.launch {
            settingsRepository.update { it.copy(pin = pin) }
            transient.update { it.copy(isChangingPin = false, notice = R.string.pin_saved) }
        }
    }

    fun clearSource() {
        viewModelScope.launch {
            sourceRepository.clear()
            _sourceCleared.value = true
        }
    }

    fun refreshEpgNow() {
        viewModelScope.launch {
            runCatching { epgRepository.refresh(force = true) }
        }
    }

    /**
     * Borra solo lo que se vuelve a descargar: canales, guía y catálogo. Favoritos, "Me gusta" y
     * lo que se estaba viendo son del usuario y se conservan.
     */
    fun clearCache() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                database.runInTransaction {
                    database.openHelper.writableDatabase.apply {
                        execSQL("DELETE FROM epg_programs")
                        execSQL("DELETE FROM episodes")
                        execSQL("DELETE FROM movies")
                        execSQL("DELETE FROM series")
                        execSQL("DELETE FROM categories")
                        execSQL("DELETE FROM channels")
                    }
                }
            }
            runCatching { channelRepository.refresh(force = true) }
            transient.update { it.copy(notice = R.string.settings_cache_cleared) }
        }
    }

    fun dismissNotice() {
        transient.update { it.copy(notice = null) }
    }

    private fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { settingsRepository.update(transform) }
    }

    private companion object {
        const val PIN_LENGTH = 4
    }
}
