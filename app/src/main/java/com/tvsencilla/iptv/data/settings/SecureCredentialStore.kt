package com.tvsencilla.iptv.data.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.tvsencilla.iptv.domain.model.ContentSource
import com.tvsencilla.iptv.domain.model.SourceType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provider credentials at rest. Backed by [EncryptedSharedPreferences] so the M3U URL (which
 * embeds the subscription token) and the Xtream password never sit in plain text, and never in
 * logs — [ContentSource.toString] masks them too.
 */
@Singleton
class SecureCredentialStore @Inject constructor(
    @ApplicationContext context: Context,
) {

    /** Lazy: unlocking the keystore is slow enough to matter on a 1 GB TV box. */
    private val preferences: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun read(): ContentSource? {
        val type = preferences.getString(KEY_TYPE, null)
            ?.let { name -> runCatching { SourceType.valueOf(name) }.getOrNull() }
            ?: return null

        return ContentSource(
            type = type,
            m3uUrl = preferences.getString(KEY_M3U_URL, null),
            epgUrl = preferences.getString(KEY_EPG_URL, null),
            host = preferences.getString(KEY_HOST, null),
            username = preferences.getString(KEY_USERNAME, null),
            password = preferences.getString(KEY_PASSWORD, null),
        )
    }

    fun write(source: ContentSource) {
        preferences.edit()
            .putString(KEY_TYPE, source.type.name)
            .putString(KEY_M3U_URL, source.m3uUrl)
            .putString(KEY_EPG_URL, source.epgUrl)
            .putString(KEY_HOST, source.normalizedHost)
            .putString(KEY_USERNAME, source.username)
            .putString(KEY_PASSWORD, source.password)
            .apply()
    }

    fun clear() {
        preferences.edit().clear().apply()
    }

    private companion object {
        const val FILE_NAME = "provider_credentials"
        const val KEY_TYPE = "type"
        const val KEY_M3U_URL = "m3u_url"
        const val KEY_EPG_URL = "epg_url"
        const val KEY_HOST = "host"
        const val KEY_USERNAME = "username"
        const val KEY_PASSWORD = "password"
    }
}
