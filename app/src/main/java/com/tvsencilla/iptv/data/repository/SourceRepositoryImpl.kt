package com.tvsencilla.iptv.data.repository

import com.tvsencilla.iptv.data.local.AppDatabase
import com.tvsencilla.iptv.data.provider.ProviderRegistry
import com.tvsencilla.iptv.data.settings.CapabilitiesStore
import com.tvsencilla.iptv.data.settings.RefreshTracker
import com.tvsencilla.iptv.data.settings.SecureCredentialStore
import com.tvsencilla.iptv.data.xtream.XTREAM_FAILOVER_HOSTS
import com.tvsencilla.iptv.domain.model.ContentSource
import com.tvsencilla.iptv.domain.model.ProviderCapabilities
import com.tvsencilla.iptv.domain.provider.ProviderException
import com.tvsencilla.iptv.domain.repository.SettingsRepository
import com.tvsencilla.iptv.domain.repository.SourceRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Singleton
class SourceRepositoryImpl @Inject constructor(
    private val credentials: SecureCredentialStore,
    private val capabilitiesStore: CapabilitiesStore,
    private val registry: ProviderRegistry,
    private val settings: SettingsRepository,
    private val refreshTracker: RefreshTracker,
    private val database: AppDatabase,
) : SourceRepository {

    private val _source = MutableStateFlow<ContentSource?>(null)
    private val loadMutex = Mutex()
    private var loaded = false

    override val source: Flow<ContentSource?> = _source.onStart { ensureLoaded() }

    override val capabilities: Flow<ProviderCapabilities> = capabilitiesStore.capabilities

    override suspend fun current(): ContentSource? {
        ensureLoaded()
        return _source.value
    }

    private suspend fun ensureLoaded() {
        if (loaded) return
        loadMutex.withLock {
            if (loaded) return
            _source.value = withContext(Dispatchers.IO) { credentials.read() }
            loaded = true
        }
    }

    override suspend fun save(source: ContentSource): ProviderCapabilities {
        // Nothing is stored until the provider confirms the subscription actually works.
        val (resolved, workingSource) = if (source.autoServer) {
            authenticateWithFailover(source)
        } else {
            registry.forSource(source).authenticate(source) to source
        }
        withContext(Dispatchers.IO) { credentials.write(workingSource) }
        capabilitiesStore.save(resolved)
        _source.value = workingSource
        loaded = true
        settings.update { it.copy(setupCompleted = true) }
        refreshTracker.reset()
        return resolved
    }

    override suspend fun clear() {
        withContext(Dispatchers.IO) { credentials.clear() }
        capabilitiesStore.clear()
        refreshTracker.reset()
        _source.value = null
        loaded = true
        withContext(Dispatchers.IO) { database.clearAllTables() }
        settings.update { it.copy(setupCompleted = false, lastChannelId = null) }
    }

    override suspend fun <T> withFailover(action: suspend (ContentSource) -> T): T {
        val source = current() ?: throw ProviderException(ProviderException.Reason.BAD_CREDENTIALS)
        if (!source.autoServer) return action(source)

        var lastError: Throwable? = null
        for (host in failoverHosts(source)) {
            val candidate = source.copy(host = host)
            try {
                val result = action(candidate)
                if (candidate.host != source.host) persistWorkingHost(candidate)
                return result
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                lastError = error
            }
        }
        throw lastError ?: ProviderException(ProviderException.Reason.UNREACHABLE)
    }

    /** Igual que [save], pero probando servidores hasta que uno autentique. */
    private suspend fun authenticateWithFailover(source: ContentSource): Pair<ProviderCapabilities, ContentSource> {
        var lastError: Throwable? = null
        for (host in failoverHosts(source)) {
            val candidate = source.copy(host = host)
            try {
                return registry.forSource(candidate).authenticate(candidate) to candidate
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                lastError = error
            }
        }
        throw lastError ?: ProviderException(ProviderException.Reason.BAD_CREDENTIALS)
    }

    private suspend fun persistWorkingHost(newSource: ContentSource) {
        _source.value = newSource
        withContext(Dispatchers.IO) { credentials.write(newSource) }
    }

    /** El servidor que ya funcionaba primero, para no cambiar de sitio sin necesidad. */
    private fun failoverHosts(source: ContentSource): List<String> {
        val known = source.normalizedHost
        val rest = XTREAM_FAILOVER_HOSTS.filterNot { it.equals(known, ignoreCase = true) }
        return listOfNotNull(known) + rest
    }
}
