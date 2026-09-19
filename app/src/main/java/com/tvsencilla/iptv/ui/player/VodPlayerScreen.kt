package com.tvsencilla.iptv.ui.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.tvsencilla.iptv.R
import com.tvsencilla.iptv.ui.components.BigButton
import com.tvsencilla.iptv.ui.components.MessageBanner
import com.tvsencilla.iptv.ui.components.ProgressStripe
import com.tvsencilla.iptv.ui.remote.RemoteKey
import com.tvsencilla.iptv.ui.remote.RemoteKeyBus
import com.tvsencilla.iptv.ui.remote.RemoteKeyHandler
import com.tvsencilla.iptv.ui.theme.overscan
import kotlinx.coroutines.delay

@Composable
fun VodPlayerScreen(
    remoteKeyBus: RemoteKeyBus,
    onFinished: () -> Unit,
    viewModel: VodPlayerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val player by viewModel.player.collectAsStateWithLifecycle()
    val surfaceFocus = remember { FocusRequester() }
    val playPauseFocus = remember { FocusRequester() }

    // El cursor va a los botones cuando se abren, y vuelve a la película cuando se cierran.
    // Si está el aviso del siguiente capítulo, ese aviso ya se lleva el cursor.
    LaunchedEffect(state.areControlsFocused, state.areControlsVisible, state.nextEpisodeCountdown == null) {
        when {
            state.nextEpisodeCountdown != null -> Unit
            state.areControlsFocused && state.areControlsVisible -> runCatching { playPauseFocus.requestFocus() }
            else -> runCatching { surfaceFocus.requestFocus() }
        }
    }

    // Keeps the elapsed time and the bar moving while the controls are on screen.
    LaunchedEffect(state.areControlsVisible) {
        while (state.areControlsVisible) {
            viewModel.refreshPosition()
            delay(1_000L)
        }
    }

    LaunchedEffect(state.isFinished) {
        if (state.isFinished) onFinished()
    }

    RemoteKeyHandler(remoteKeyBus) { key ->
        when (key) {
            RemoteKey.PlayPause -> viewModel.togglePlayPause()
            RemoteKey.Play, RemoteKey.Pause -> viewModel.togglePlayPause()
            RemoteKey.FastForward -> viewModel.seekBy(1)
            RemoteKey.Rewind -> viewModel.seekBy(-1)
            RemoteKey.Info -> viewModel.showControls()
            else -> Unit
        }
    }

    BackHandler {
        if (state.areControlsFocused) viewModel.releaseControls() else onFinished()
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        PlayerSurface(player = player, modifier = Modifier.fillMaxSize())

        Box(
            Modifier
                .fillMaxSize()
                .focusRequester(surfaceFocus)
                .focusable()
                .onKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                    when (event.key) {
                        Key.DirectionCenter, Key.Enter -> { viewModel.togglePlayPause(); true }
                        Key.DirectionLeft -> { viewModel.seekBy(-1); true }
                        Key.DirectionRight -> { viewModel.seekBy(1); true }
                        Key.DirectionUp, Key.DirectionDown -> { viewModel.focusControls(); true }
                        else -> false
                    }
                },
        )

        if (state.isBuffering && !state.isReconnecting && state.errorMessage == null) {
            BufferingLabel(Modifier.align(Alignment.Center))
        }

        if (state.isReconnecting) {
            MessageBanner(
                message = stringResource(R.string.player_reconnecting),
                modifier = Modifier.align(Alignment.TopCenter).padding(28.dp),
            )
        }

        state.errorMessage?.let { message ->
            MessageBanner(
                message = stringResource(message),
                modifier = Modifier.align(Alignment.Center).padding(28.dp),
            )
        }

        if (state.areControlsVisible) {
            PlaybackControls(
                state = state,
                playPauseFocus = playPauseFocus,
                onTogglePlayPause = viewModel::togglePlayPause,
                onRewind = { viewModel.seekBy(-1) },
                onForward = { viewModel.seekBy(1) },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }

        val countdown = state.nextEpisodeCountdown
        if (countdown != null) {
            NextEpisodePrompt(
                secondsRemaining = countdown,
                episodeTitle = state.nextEpisodeTitle,
                onPlayNow = viewModel::playNextEpisodeNow,
                onCancel = viewModel::cancelNextEpisode,
                modifier = Modifier.align(Alignment.BottomEnd).padding(28.dp),
            )
        }
    }
}

@Composable
private fun PlaybackControls(
    state: VodPlayerUiState,
    playPauseFocus: FocusRequester,
    onTogglePlayPause: () -> Unit,
    onRewind: () -> Unit,
    onForward: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.86f))
            .overscan(),
    ) {
        Text(text = state.title, style = MaterialTheme.typography.titleMedium)
        state.subtitle?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(12.dp))

        ProgressStripe(
            progress = if (state.durationMillis > 0) {
                state.positionMillis.toFloat() / state.durationMillis
            } else {
                0f
            },
            modifier = Modifier.clip(RoundedCornerShape(6.dp)),
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "${formatElapsed(state.positionMillis)} / ${formatElapsed(state.durationMillis)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(14.dp))

        if (!state.areControlsFocused) {
            // Nada de menús ocultos: se dice cómo llegar a los botones.
            Text(
                text = stringResource(R.string.player_controls_hint),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 10.dp),
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BigButton(
                text = stringResource(R.string.player_rewind),
                icon = Icons.Default.FastRewind,
                onClick = onRewind,
                minHeight = 58.dp,
            )
            BigButton(
                text = stringResource(if (state.isPlaying) R.string.player_pause else R.string.player_play),
                icon = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                onClick = onTogglePlayPause,
                minHeight = 58.dp,
                modifier = Modifier.focusRequester(playPauseFocus),
            )
            BigButton(
                text = stringResource(R.string.player_forward),
                icon = Icons.Default.FastForward,
                onClick = onForward,
                minHeight = 58.dp,
            )
        }
    }
}

@Composable
private fun NextEpisodePrompt(
    secondsRemaining: Int,
    episodeTitle: String?,
    onPlayNow: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val playNowFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { playNowFocus.requestFocus() } }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(20.dp),
    ) {
        Text(
            text = stringResource(R.string.series_next_episode_in, secondsRemaining),
            style = MaterialTheme.typography.titleSmall,
        )
        episodeTitle?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BigButton(
                text = stringResource(R.string.series_play_next_now),
                onClick = onPlayNow,
                minHeight = 54.dp,
                modifier = Modifier.focusRequester(playNowFocus),
            )
            BigButton(
                text = stringResource(R.string.series_cancel_next),
                onClick = onCancel,
                minHeight = 54.dp,
            )
        }
    }
}

private fun formatElapsed(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0L)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

