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
import com.tvsencilla.iptv.di.ApplicationScope
import com.tvsencilla.iptv.domain.model.PlayableKind
import com.tvsencilla.iptv.domain.repository.SettingsRepository
import com.tvsencilla.iptv.domain.repository.VodRepository
import com.tvsencilla.iptv.player.PlayerFactory
import com.tvsencilla.iptv.player.ReconnectPolicy
import com.tvsencilla.iptv.player.userMessageRes
import com.tvsencilla.iptv.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VodPlayerUiState(
    val title: String = "",
    val subtitle: String? = null,
    val isPlaying: Boolean = false,
    val positionMillis: Long = 0L,
    val durationMillis: Long = 0L,
    val areControlsVisible: Boolean = true,
    val areControlsFocused: Boolean = false,
    val isReconnecting: Boolean = false,
    /** Counts down before the next episode starts, and can always be cancelled. */
    val nextEpisodeCountdown: Int? = null,
    val nextEpisodeTitle: String? = null,
    @StringRes val errorMessage: Int? = null,
    val isFinished: Boolean = false,
    val isBuffering: Boolean = false,
)

@HiltViewModel
class VodPlayerViewModel @Inject constructor(
    private val vodRepository: VodRepository,
    private val settingsRepository: SettingsRepository,
    private val playerFactory: PlayerFactory,
    @ApplicationScope private val applicationScope: CoroutineScope,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val kind: PlayableKind =
        if (savedStateHandle.get<String>(Routes.ARG_KIND) == Routes.KIND_EPISODE) {
            PlayableKind.EPISODE
        } else {
            PlayableKind.MOVIE
        }
    private val itemId: String = requireNotNull(savedStateHandle[Routes.ARG_ITEM_ID])
    private val shouldResume: Boolean = savedStateHandle.get<String>(Routes.ARG_RESUME) != "false"

    private val _state = MutableStateFlow(VodPlayerUiState())
    val state: StateFlow<VodPlayerUiState> = _state.asStateFlow()

    private val _player = MutableStateFlow<ExoPlayer?>(null)
    val player: StateFlow<ExoPlayer?> = _player.asStateFlow()

    private var currentItemId: String = itemId
    private var currentKind: PlayableKind = kind
    private val reconnectPolicy = ReconnectPolicy()
    private var reconnectJob: Job? = null
    private var countdownJob: Job? = null
    private var controlsJob: Job? = null

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.update { it.copy(isPlaying = isPlaying) }
        }

        override fun onPlayerError(error: PlaybackException) {
            val delayMillis = reconnectPolicy.nextDelayMillis()
            if (delayMillis == null) {
                _state.update { it.copy(isReconnecting = false, errorMessage = error.userMessageRes()) }
                return
            }
            _state.update { it.copy(isReconnecting = true) }
            reconnectJob?.cancel()
            reconnectJob = viewModelScope.launch {
                delay(delayMillis)
                _player.value?.let { it.prepare(); it.play() }
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING -> _state.update { it.copy(isBuffering = true) }

                Player.STATE_READY -> {
                    reconnectPolicy.reset()
                    _state.update { it.copy(isReconnecting = false, errorMessage = null, isBuffering = false) }
                }

                Player.STATE_ENDED -> onPlaybackEnded()
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
            load(currentItemId, currentKind, resume = shouldResume)
        }

        // Progress is written every ten seconds, so an abrupt power cut loses at most that much.
        viewModelScope.launch {
            while (true) {
                delay(PROGRESS_INTERVAL_MILLIS)
                persistProgress()
            }
        }
    }

    private suspend fun load(id: String, itemKind: PlayableKind, resume: Boolean) {
        val player = _player.value ?: return
        currentItemId = id
        currentKind = itemKind

        val streamUrl: String?
        val title: String
        var subtitle: String? = null

        if (itemKind == PlayableKind.MOVIE) {
            val movie = vodRepository.movieById(id)
            streamUrl = movie?.streamUrl
            title = movie?.title.orEmpty()
        } else {
            val episode = vodRepository.episodeById(id)
            streamUrl = episode?.streamUrl
            title = episode?.title.orEmpty()
            subtitle = episode?.let { "${it.seasonNumber}x${it.episodeNumber}" }
        }

        if (streamUrl == null) {
            _state.update { it.copy(errorMessage = R.string.error_playback) }
            return
        }

        val startAt = if (resume) {
            vodRepository.progressFor(id)?.takeIf { it.isWorthResuming }?.positionMillis ?: 0L
        } else {
            0L
        }

        _state.update {
            it.copy(
                title = title,
                subtitle = subtitle,
                errorMessage = null,
                nextEpisodeCountdown = null,
                nextEpisodeTitle = null,
                isFinished = false,
            )
        }

        // Se empieza ya en el minuto guardado. Preparar desde el principio y saltar después obliga
        // a descargar dos veces, y en el Fire TV eso llegaba a medio minuto de espera.
        player.setMediaItem(MediaItem.fromUri(streamUrl), startAt)
        player.prepare()
        player.play()
        showControls()
    }

    private fun onPlaybackEnded() {
        viewModelScope.launch {
            persistProgress(markComplete = true)

            if (currentKind != PlayableKind.EPISODE) {
                _state.update { it.copy(isFinished = true) }
                return@launch
            }

            val next = vodRepository.nextEpisode(currentItemId)
            if (next == null) {
                _state.update { it.copy(isFinished = true) }
                return@launch
            }

            countdownJob?.cancel()
            countdownJob = viewModelScope.launch {
                _state.update { it.copy(nextEpisodeTitle = next.title) }
                for (remaining in NEXT_EPISODE_COUNTDOWN_SECONDS downTo 1) {
                    _state.update { it.copy(nextEpisodeCountdown = remaining) }
                    delay(1_000L)
                }
                _state.update { it.copy(nextEpisodeCountdown = null) }
                load(next.id, PlayableKind.EPISODE, resume = false)
            }
        }
    }

    fun playNextEpisodeNow() {
        countdownJob?.cancel()
        viewModelScope.launch {
            val next = vodRepository.nextEpisode(currentItemId) ?: return@launch
            load(next.id, PlayableKind.EPISODE, resume = false)
        }
    }

    fun cancelNextEpisode() {
        countdownJob?.cancel()
        _state.update { it.copy(nextEpisodeCountdown = null, nextEpisodeTitle = null, isFinished = true) }
    }

    fun togglePlayPause() {
        val player = _player.value ?: return
        if (player.isPlaying) player.pause() else player.play()
        showControls()
    }

    /**
     * Salta hacia delante (direction > 0) o atrás (direction < 0). Si el usuario sigue
     * pulsando o mantiene la tecla en la misma dirección, el salto crece: 10 s, 30 s, 1 min, 5 min.
     */
    fun seekBy(direction: Int) {
        val player = _player.value ?: return
        val now = System.currentTimeMillis()
        val sign = if (direction >= 0) 1 else -1
        seekStreak = if (sign == lastSeekSign && now - lastSeekAtMillis < SEEK_STREAK_WINDOW_MILLIS) {
            seekStreak + 1
        } else {
            0
        }
        lastSeekSign = sign
        lastSeekAtMillis = now

        val step = SEEK_STEPS_MILLIS[
            (seekStreak / SEEK_PRESSES_PER_LEVEL).coerceAtMost(SEEK_STEPS_MILLIS.lastIndex),
        ]
        val duration = player.duration
        var target = (player.currentPosition + sign * step).coerceAtLeast(0L)
        if (duration > 0) target = target.coerceAtMost((duration - 1_000L).coerceAtLeast(0L))
        player.seekTo(target)
        showControls()
    }

    private var seekStreak = 0
    private var lastSeekSign = 0
    private var lastSeekAtMillis = 0L

    fun showControls() {
        refreshPosition()
        // Con el cursor en los botones, se quedan hasta que el usuario pulse Atrás.
        if (_state.value.areControlsFocused) return
        controlsJob?.cancel()
        _state.update { it.copy(areControlsVisible = true) }
        controlsJob = viewModelScope.launch {
            delay(CONTROLS_VISIBLE_MILLIS)
            _state.update { it.copy(areControlsVisible = false) }
        }
    }

    /** Arriba o abajo sobre la película: se enseñan los botones y el cursor pasa a ellos. */
    fun focusControls() {
        controlsJob?.cancel()
        refreshPosition()
        _state.update { it.copy(areControlsVisible = true, areControlsFocused = true) }
    }

    /** Atrás desde los botones: se esconden y el cursor vuelve a la película. */
    fun releaseControls() {
        controlsJob?.cancel()
        _state.update { it.copy(areControlsVisible = false, areControlsFocused = false) }
    }

    fun refreshPosition() {
        val player = _player.value ?: return
        _state.update {
            it.copy(
                positionMillis = player.currentPosition.coerceAtLeast(0L),
                durationMillis = player.duration.takeIf { duration -> duration > 0L } ?: 0L,
            )
        }
    }

    private suspend fun persistProgress(markComplete: Boolean = false) {
        val player = _player.value ?: return
        val duration = player.duration
        if (duration <= 0L) return
        val position = if (markComplete) duration else player.currentPosition
        vodRepository.saveProgress(
            itemId = currentItemId,
            kind = currentKind,
            positionMillis = position.coerceIn(0L, duration),
            durationMillis = duration,
        )
    }

    override fun onCleared() {
        val player = _player.value ?: return
        val duration = player.duration
        val position = player.currentPosition.coerceIn(0L, maxOf(duration, 0L))
        val id = currentItemId
        val itemKind = currentKind

        player.removeListener(playerListener)
        player.release()
        _player.value = null

        // viewModelScope is already cancelled here, hence the application scope.
        if (duration > 0L) {
            applicationScope.launch {
                vodRepository.saveProgress(id, itemKind, position, duration)
            }
        }
    }

    private companion object {
        const val PROGRESS_INTERVAL_MILLIS = 10_000L
        const val CONTROLS_VISIBLE_MILLIS = 10_000L
        const val NEXT_EPISODE_COUNTDOWN_SECONDS = 10
        val SEEK_STEPS_MILLIS = longArrayOf(10_000L, 30_000L, 60_000L, 300_000L)
        const val SEEK_PRESSES_PER_LEVEL = 3
        const val SEEK_STREAK_WINDOW_MILLIS = 1_200L
    }
}
