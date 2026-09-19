package com.tvsencilla.iptv.data.provider

import com.tvsencilla.iptv.domain.model.ContentSource
import com.tvsencilla.iptv.domain.model.SourceType
import com.tvsencilla.iptv.domain.provider.ContentProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves which [ContentProvider] serves a source. Providers are contributed as a set, so a new
 * protocol is added by binding one more implementation and nothing else.
 */
@Singleton
class ProviderRegistry @Inject constructor(
    private val providers: Set<@JvmSuppressWildcards ContentProvider>,
) {

    fun forType(type: SourceType): ContentProvider =
        providers.firstOrNull { it.handles == type }
            ?: error("No ContentProvider registered for $type")

    fun forSource(source: ContentSource): ContentProvider = forType(source.type)
}
