package com.tvsencilla.iptv.data.sync

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.tvsencilla.iptv.domain.repository.FavoritesSyncRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/**
 * Stands in for the profile server until it exists. The order is kept on the device under a
 * separate key, which is enough to exercise the pull-then-push flow the real backend will use.
 */
@Singleton
class SimulatedFavoritesSync @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : FavoritesSyncRepository {

    override suspend fun pull(): List<String>? {
        val stored = dataStore.data.first()[KEY] ?: return null
        return stored.split(SEPARATOR).filter { it.isNotBlank() }
    }

    override suspend fun push(orderedChannelIds: List<String>) {
        dataStore.edit { it[KEY] = orderedChannelIds.joinToString(SEPARATOR) }
    }

    private companion object {
        val KEY = stringPreferencesKey("synced_favorite_order")
        const val SEPARATOR = "\n"
    }
}
