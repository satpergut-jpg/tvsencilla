package com.tvsencilla.iptv.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.tvsencilla.iptv.domain.model.AppSettings
import com.tvsencilla.iptv.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

@Singleton
class SettingsDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override val settings: Flow<AppSettings> = dataStore.data.map { it.toSettings() }

    override suspend fun current(): AppSettings = settings.first()

    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        dataStore.edit { preferences ->
            val updated = transform(preferences.toSettings())
            preferences[Keys.SETUP_COMPLETED] = updated.setupCompleted
            preferences[Keys.SIMPLE_MODE] = updated.simpleMode
            preferences[Keys.FONT_SIZE] = updated.fontSize.name
            preferences[Keys.SUBTITLE_SIZE] = updated.subtitleSize.name
            preferences[Keys.NUMBERING_MODE] = updated.numberingMode.name
            preferences[Keys.START_ON_LAST_CHANNEL] = updated.startOnLastChannel
            preferences[Keys.SHOW_EPG_GRID] = updated.showEpgGrid
            preferences[Keys.VOICE_AUTO_TUNE] = updated.voiceAutoTune
            preferences[Keys.AUDIO_LANGUAGE] = updated.preferredAudioLanguage
            preferences[Keys.SUBTITLE_LANGUAGE] = updated.preferredSubtitleLanguage
            preferences[Keys.PIN] = updated.pin
            preferences[Keys.HIDDEN_CATEGORIES] = updated.hiddenCategoryIds
            updated.lastChannelId
                ?.let { preferences[Keys.LAST_CHANNEL_ID] = it }
                ?: preferences.remove(Keys.LAST_CHANNEL_ID)
        }
    }

    private fun Preferences.toSettings(): AppSettings {
        val defaults = AppSettings()
        return AppSettings(
            setupCompleted = this[Keys.SETUP_COMPLETED] ?: defaults.setupCompleted,
            simpleMode = this[Keys.SIMPLE_MODE] ?: defaults.simpleMode,
            fontSize = this[Keys.FONT_SIZE].toEnum(defaults.fontSize),
            subtitleSize = this[Keys.SUBTITLE_SIZE].toEnum(defaults.subtitleSize),
            numberingMode = this[Keys.NUMBERING_MODE].toEnum(defaults.numberingMode),
            startOnLastChannel = this[Keys.START_ON_LAST_CHANNEL] ?: defaults.startOnLastChannel,
            showEpgGrid = this[Keys.SHOW_EPG_GRID] ?: defaults.showEpgGrid,
            voiceAutoTune = this[Keys.VOICE_AUTO_TUNE] ?: defaults.voiceAutoTune,
            preferredAudioLanguage = this[Keys.AUDIO_LANGUAGE] ?: defaults.preferredAudioLanguage,
            preferredSubtitleLanguage = this[Keys.SUBTITLE_LANGUAGE] ?: defaults.preferredSubtitleLanguage,
            pin = this[Keys.PIN] ?: defaults.pin,
            hiddenCategoryIds = this[Keys.HIDDEN_CATEGORIES] ?: defaults.hiddenCategoryIds,
            lastChannelId = this[Keys.LAST_CHANNEL_ID],
        )
    }

    private inline fun <reified T : Enum<T>> String?.toEnum(fallback: T): T =
        this?.let { name -> runCatching { enumValueOf<T>(name) }.getOrNull() } ?: fallback

    private object Keys {
        val SETUP_COMPLETED = booleanPreferencesKey("setup_completed")
        val SIMPLE_MODE = booleanPreferencesKey("simple_mode")
        val FONT_SIZE = stringPreferencesKey("font_size")
        val SUBTITLE_SIZE = stringPreferencesKey("subtitle_size")
        val NUMBERING_MODE = stringPreferencesKey("numbering_mode")
        val START_ON_LAST_CHANNEL = booleanPreferencesKey("start_on_last_channel")
        val SHOW_EPG_GRID = booleanPreferencesKey("show_epg_grid")
        val VOICE_AUTO_TUNE = booleanPreferencesKey("voice_auto_tune")
        val AUDIO_LANGUAGE = stringPreferencesKey("audio_language")
        val SUBTITLE_LANGUAGE = stringPreferencesKey("subtitle_language")
        val PIN = stringPreferencesKey("pin")
        val HIDDEN_CATEGORIES = stringSetPreferencesKey("hidden_categories")
        val LAST_CHANNEL_ID = stringPreferencesKey("last_channel_id")
    }
}
