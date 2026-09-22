package com.tvsencilla.iptv.domain.model

enum class SourceType { M3U, XTREAM }

/**
 * The provider subscription the user is entitled to. Credentials never reach logs;
 * they are persisted through [com.tvsencilla.iptv.data.settings.SecureCredentialStore].
 */
data class ContentSource(
    val type: SourceType,
    val m3uUrl: String? = null,
    val epgUrl: String? = null,
    val host: String? = null,
    val username: String? = null,
    val password: String? = null,
    /**
     * El usuario no ha fijado un servidor propio: [host] es el último que funcionó, y si deja de
     * responder se prueban los demás servidores conocidos hasta encontrar uno que sirva.
     */
    val autoServer: Boolean = false,
) {
    /** Base URL of an Xtream server, always without a trailing slash. */
    val normalizedHost: String?
        get() = host?.trim()?.removeSuffix("/")?.let {
            if (it.startsWith("http://") || it.startsWith("https://")) it else "http://$it"
        }

    override fun toString(): String = when (type) {
        SourceType.M3U -> "ContentSource(type=M3U, m3uUrl=${m3uUrl?.take(24)}…)"
        SourceType.XTREAM -> "ContentSource(type=XTREAM, host=$host, username=***, password=***)"
    }
}

data class ProviderCapabilities(
    val supportsLive: Boolean = true,
    val supportsMovies: Boolean = false,
    val supportsSeries: Boolean = false,
    val supportsEpg: Boolean = false,
    val supportsCatchUp: Boolean = false,
)
