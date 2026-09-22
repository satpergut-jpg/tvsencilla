package com.tvsencilla.iptv.ui.player

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.tvsencilla.iptv.R
import com.tvsencilla.iptv.domain.model.Channel
import com.tvsencilla.iptv.domain.model.ChannelQuality
import com.tvsencilla.iptv.domain.model.NowNext
import com.tvsencilla.iptv.domain.model.displayName
import com.tvsencilla.iptv.domain.repository.ChannelRepository
import com.tvsencilla.iptv.domain.repository.EpgRepository
import com.tvsencilla.iptv.domain.repository.SettingsRepository
import com.tvsencilla.iptv.domain.tuner.ChannelTunerFactory
import com.tvsencilla.iptv.player.LiveFormatMemory
import com.tvsencilla.iptv.player.PlayerFactory
import com.tvsencilla.iptv.player.alternateLiveUrl
import com.tvsencilla.iptv.player.isFormatProblem
import com.tvsencilla.iptv.player.ReconnectPolicy
import com.tvsencilla.iptv.player.userMessageRes
import com.tvsencilla.iptv.ui.live.FavoriteNotice
import com.tvsencilla.iptv.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LivePlayerUiState(
    val channel: Channel? = null,
    val remoteNumber: Int? = null,
    val isBannerVisible: Boolean = false,
    /**
     * La barra con el cursor dentro de sus botones. Así no se cierra sola mientras se está
     * eligiendo una opción.
     */
    val isBannerInteractive: Boolean = false,
    val favoriteNotice: FavoriteNotice? = null,
    /** Cargando el canal: se dice con palabras, no solo con la rueda del reproductor. */
    val isBuffering: Boolean = false,
    val isReconnecting: Boolean = false,
    val isChannelListVisible: Boolean = false,
    val hasPreviousChannel: Boolean = false,
    val canWatchFromStart: Boolean = false,
    @StringRes val errorMessage: Int? = null,
    val notFoundNumber: Int? = null,
    /** La calidad que se está viendo ahora, cuando el canal tiene más de una. */
    val selectedQuality: ChannelQuality? = null,
)

@HiltViewModel
class LivePlayerViewModel @Inject constructor(
    private val channelRepository: ChannelRepository,
    private val epgRepository: EpgRepository,
    private val settingsRepository: SettingsRepository,
    private val playerFactory: PlayerFactory,
    private val formatMemory: LiveFormatMemory,
    tunerFactory: ChannelTunerFactory,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val tuner = tunerFactory.create(viewModelScope)

    private val initialChannelId: String? = savedStateHandle[Routes.ARG_CHANNEL_ID]

    private val _state = MutableStateFlow(LivePlayerUiState())
    val state: StateFlow<LivePlayerUiState> = _state.asStateFlow()

    private val _player = MutableStateFlow<ExoPlayer?>(null)
    val player: StateFlow<ExoPlayer?> = _player.asStateFlow()

    private val currentChannel = MutableStateFlow<Channel?>(null)

    val nowNext: StateFlow<NowNext> = currentChannel
        .flatMapLatest { channel ->
            if (channel == null) flowOf(NowNext()) else epgRepository.observeNowNext(channel.epgChannelId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NowNext())

    val dialledDigits: StateFlow<String?> = tuner.dialledDigits

    private val reconnectPolicy = ReconnectPolicy()
    private var reconnectJob: Job? = null
    private var bannerJob: Job? = null
    private var previousChannelId: String? = null
    private var currentStreamUrl: String? = null
    private var hasTriedAlternateFormat = false
    private var startWatchdog: Job? = null

    private val playerListener = object : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
            // Un panel suele servir el directo en HLS o en MPEG-TS, pero no siempre en los dos, y
            // no lo anuncia de forma fiable. Si lo que llega no se entiende, se prueba el otro
            // formato; si el fallo es del servidor (un 502, por ejemplo), cambiarlo no arregla nada.
            if (error.isFormatProblem() && tryAlternateFormat()) return
            scheduleReconnect(error)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING -> _state.update { it.copy(isBuffering = true) }
                Player.STATE_READY -> {
                    reconnectPolicy.reset()
                    reconnectJob?.cancel()
                    startWatchdog?.cancel()
                    currentStreamUrl?.let(formatMemory::rememberWorking)
                    _state.update { it.copy(isReconnecting = false, errorMessage = null, isBuffering = false) }
                }
            }
        }
    }

    init {
        viewModelScope.launch {
            val settings = settingsRepository.current()
            val created = playerFactory.create(
                preferredAudioLanguage = settings.preferredAudioLanguage,
                preferredSubtitleLanguage = settings.preferredSubtitleLanguage,
            )
            created.addListener(playerListener)
            created.playWhenReady = true
            _player.value = created

            val startId = initialChannelId ?: settings.lastChannelId
            val channel = startId?.let { channelRepository.channelById(it) }
            if (channel != null) play(channel) else _state.update { it.copy(isChannelListVisible = true) }
        }

        viewModelScope.launch { tuner.tuneTo.collect { play(it) } }

        viewModelScope.launch {
            tuner.numberNotFound.collect { number ->
                _state.update { it.copy(notFoundNumber = number) }
                delay(MESSAGE_VISIBLE_MILLIS)
                _state.update { if (it.notFoundNumber == number) it.copy(notFoundNumber = null) else it }
            }
        }
    }

    fun play(channel: Channel) {
        val player = _player.value ?: return
        val current = currentChannel.value
        if (current?.id == channel.id && player.isPlaying) {
            showBanner()
            return
        }
        if (current != null) previousChannelId = current.id

        reconnectJob?.cancel()
        reconnectPolicy.reset()
        currentChannel.value = channel
        val url = formatMemory.adapt(channel.streamUrl)
        currentStreamUrl = url
        hasTriedAlternateFormat = false

        player.setMediaItem(MediaItem.fromUri(url))
        player.prepare()
        watchStart()

        _state.update {
            it.copy(
                channel = channel,
                remoteNumber = tuner.numberFor(channel),
                isChannelListVisible = false,
                isBannerInteractive = false,
                isReconnecting = false,
                errorMessage = null,
                hasPreviousChannel = previousChannelId != null,
                canWatchFromStart = channel.supportsCatchUp,
                selectedQuality = channel.qualityOptions.firstOrNull(),
            )
        }
        showBanner()

        viewModelScope.launch {
            settingsRepository.update { it.copy(lastChannelId = channel.id) }
        }
    }

    /**
     * Un canal caído puede tener al servidor medio minuto sin contestar. En vez de dejar al usuario
     * mirando "Cargando…" sin saber qué pasa, a los 15 segundos se le dice que pruebe otro canal.
     */
    private fun watchStart() {
        startWatchdog?.cancel()
        startWatchdog = viewModelScope.launch {
            delay(START_TIMEOUT_MILLIS)
            reconnectJob?.cancel()
            _player.value?.stop()
            _state.update {
                it.copy(
                    isBuffering = false,
                    isReconnecting = false,
                    errorMessage = R.string.error_channel_not_responding,
                )
            }
        }
    }

    /** Pasa a la siguiente calidad disponible del canal actual, volviendo a la primera al llegar al final. */
    fun switchQuality() {
        val channel = currentChannel.value ?: return
        val options = channel.qualityOptions
        if (options.size < 2) return
        val player = _player.value ?: return

        val currentIndex = options.indexOf(_state.value.selectedQuality).let { if (it < 0) 0 else it }
        val next = options[(currentIndex + 1) % options.size]

        reconnectJob?.cancel()
        reconnectPolicy.reset()
        val url = formatMemory.adapt(next.streamUrl)
        currentStreamUrl = url
        hasTriedAlternateFormat = false

        player.setMediaItem(MediaItem.fromUri(url))
        player.prepare()
        player.play()

        _state.update { it.copy(selectedQuality = next, isReconnecting = false, errorMessage = null) }
        showBanner()
    }

    fun channelUp() {
        tuner.channelUp(currentChannel.value?.id)?.let(::play)
    }

    fun channelDown() {
        tuner.channelDown(currentChannel.value?.id)?.let(::play)
    }

    fun goToPreviousChannel() {
        val target = previousChannelId ?: return
        viewModelScope.launch { channelRepository.channelById(target)?.let(::play) }
    }

    /** The Info key brings the banner back without changing anything. */
    fun showBanner() {
        // Con el cursor en los botones, la barra se queda hasta que el usuario la cierre.
        if (_state.value.isBannerInteractive) return
        bannerJob?.cancel()
        _state.update { it.copy(isBannerVisible = true) }
        bannerJob = viewModelScope.launch {
            delay(BANNER_VISIBLE_MILLIS)
            // Mientras el canal carga, la barra sigue: es lo único que dice qué canal se ha puesto.
            _state.first { !it.isBuffering }
            _state.update { it.copy(isBannerVisible = false) }
        }
    }

    /** Aceptar sobre el vídeo: abre la barra y lleva el cursor a sus botones. */
    fun openBannerActions() {
        bannerJob?.cancel()
        _state.update { it.copy(isBannerVisible = true, isBannerInteractive = true) }
    }

    fun hideBanner() {
        bannerJob?.cancel()
        _state.update { it.copy(isBannerVisible = false, isBannerInteractive = false) }
    }

    fun toggleFavorite() {
        val channel = currentChannel.value ?: return
        viewModelScope.launch {
            if (channel.isFavorite) {
                channelRepository.removeFavorite(channel.id)
            } else {
                channelRepository.addFavorite(channel.id)
            }
            val updated = channelRepository.channelById(channel.id) ?: return@launch
            currentChannel.value = updated
            val notice = FavoriteNotice(updated.displayName, updated.favoriteNumber)
            _state.update { it.copy(channel = updated, favoriteNotice = notice) }
            delay(NOTICE_VISIBLE_MILLIS)
            _state.update { if (it.favoriteNotice == notice) it.copy(favoriteNotice = null) else it }
        }
    }

    /** Back during playback opens the channel list rather than leaving the channel. */
    fun openChannelList() = _state.update { it.copy(isChannelListVisible = true) }

    fun closeChannelList() = _state.update { it.copy(isChannelListVisible = false) }

    fun onDigit(digit: Int) = tuner.onDigit(digit)

    fun onConfirm(): Boolean = tuner.onConfirm()

    fun onCancelDialling(): Boolean = tuner.onCancel()

    /** Catch-up, only offered when the provider actually supports it for this channel. */
    fun watchCurrentProgrammeFromStart() {
        val channel = currentChannel.value ?: return
        val program = nowNext.value.now ?: return
        viewModelScope.launch {
            val url = runCatching { channelRepository.catchUpUrl(channel, program) }.getOrNull()
                ?: return@launch
            currentStreamUrl = url
            _player.value?.let { player ->
                player.setMediaItem(MediaItem.fromUri(url))
                player.prepare()
                player.play()
            }
            hideBanner()
        }
    }

    /**
     * Cambia entre `.m3u8` y `.ts` una sola vez por canal. Devuelve false si no hay alternativa o
     * si ya se probó, para que entonces entre la reconexión normal.
     */
    private fun tryAlternateFormat(): Boolean {
        if (hasTriedAlternateFormat) return false
        val player = _player.value ?: return false
        val alternate = currentStreamUrl?.let(::alternateLiveUrl) ?: return false

        hasTriedAlternateFormat = true
        currentStreamUrl = alternate
        player.setMediaItem(MediaItem.fromUri(alternate))
        player.prepare()
        player.play()
        return true
    }


    private fun scheduleReconnect(error: PlaybackException) {
        val delayMillis = reconnectPolicy.nextDelayMillis()
        if (delayMillis == null) {
            _state.update { it.copy(isReconnecting = false, errorMessage = error.userMessageRes()) }
            return
        }

        _state.update { it.copy(isReconnecting = true) }
        reconnectJob?.cancel()
        reconnectJob = viewModelScope.launch {
            delay(delayMillis)
            _player.value?.let { player ->
                player.prepare()
                player.play()
            }
        }
    }

    override fun onCleared() {
        reconnectJob?.cancel()
        bannerJob?.cancel()
        _player.value?.let { player ->
            player.removeListener(playerListener)
            player.release()
        }
        _player.value = null
    }

    private companion object {
        const val BANNER_VISIBLE_MILLIS = 5_000L
        const val MESSAGE_VISIBLE_MILLIS = 8_000L
        const val NOTICE_VISIBLE_MILLIS = 8_000L
        const val START_TIMEOUT_MILLIS = 15_000L
    }
}
