package com.tvsencilla.iptv

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tvsencilla.iptv.domain.model.AppSettings
import com.tvsencilla.iptv.domain.repository.SettingsRepository
import com.tvsencilla.iptv.domain.repository.SourceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class AppState(
    val settings: AppSettings = AppSettings(),
    val hasSource: Boolean = false,
    val isReady: Boolean = false,
) {
    /** Setup is the entry point until a working source has been stored. */
    val needsSetup: Boolean get() = !hasSource || !settings.setupCompleted
}

@HiltViewModel
class MainViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
    sourceRepository: SourceRepository,
) : ViewModel() {

    val state: StateFlow<AppState> = combine(
        settingsRepository.settings,
        sourceRepository.source,
    ) { settings, source ->
        AppState(settings = settings, hasSource = source != null, isReady = true)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppState(),
    )
}
