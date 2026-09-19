package com.tvsencilla.iptv.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/** Remembers when each catalogue was last downloaded, so a cold start can serve cached data. */
@Singleton
class RefreshTracker @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {

    enum class Kind(internal val key: Preferences.Key<Long>) {
        CHANNELS(longPreferencesKey("refreshed_channels")),
        EPG(longPreferencesKey("refreshed_epg")),
        VOD(longPreferencesKey("refreshed_vod")),
    }

    suspend fun lastRefreshMillis(kind: Kind): Long =
        dataStore.data.first()[kind.key] ?: 0L

    suspend fun isStale(kind: Kind, maxAgeMillis: Long, nowMillis: Long = System.currentTimeMillis()): Boolean =
        nowMillis - lastRefreshMillis(kind) > maxAgeMillis

    suspend fun markRefreshed(kind: Kind, nowMillis: Long = System.currentTimeMillis()) {
        dataStore.edit { it[kind.key] = nowMillis }
    }

    suspend fun reset() {
        dataStore.edit { preferences -> Kind.entries.forEach { preferences.remove(it.key) } }
    }

    companion object {
        val CHANNELS_MAX_AGE = 6 * 60 * 60 * 1000L
        val EPG_MAX_AGE = 24 * 60 * 60 * 1000L
        val VOD_MAX_AGE = 24 * 60 * 60 * 1000L
    }
}
