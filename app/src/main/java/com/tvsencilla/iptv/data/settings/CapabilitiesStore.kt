package com.tvsencilla.iptv.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.tvsencilla.iptv.domain.model.ProviderCapabilities
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * What the current source can do, remembered across restarts so Home does not flash a Movies
 * button that the provider cannot serve.
 */
@Singleton
class CapabilitiesStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {

    val capabilities: Flow<ProviderCapabilities> = dataStore.data.map { preferences ->
        ProviderCapabilities(
            supportsLive = preferences[Keys.LIVE] ?: true,
            supportsMovies = preferences[Keys.MOVIES] ?: false,
            supportsSeries = preferences[Keys.SERIES] ?: false,
            supportsEpg = preferences[Keys.EPG] ?: false,
            supportsCatchUp = preferences[Keys.CATCH_UP] ?: false,
        )
    }

    suspend fun save(capabilities: ProviderCapabilities) {
        dataStore.edit { preferences ->
            preferences[Keys.LIVE] = capabilities.supportsLive
            preferences[Keys.MOVIES] = capabilities.supportsMovies
            preferences[Keys.SERIES] = capabilities.supportsSeries
            preferences[Keys.EPG] = capabilities.supportsEpg
            preferences[Keys.CATCH_UP] = capabilities.supportsCatchUp
        }
    }

    suspend fun clear() {
        dataStore.edit { preferences ->
            listOf(Keys.LIVE, Keys.MOVIES, Keys.SERIES, Keys.EPG, Keys.CATCH_UP)
                .forEach { preferences.remove(it) }
        }
    }

    private object Keys {
        val LIVE = booleanPreferencesKey("cap_live")
        val MOVIES = booleanPreferencesKey("cap_movies")
        val SERIES = booleanPreferencesKey("cap_series")
        val EPG = booleanPreferencesKey("cap_epg")
        val CATCH_UP = booleanPreferencesKey("cap_catchup")
    }
}
