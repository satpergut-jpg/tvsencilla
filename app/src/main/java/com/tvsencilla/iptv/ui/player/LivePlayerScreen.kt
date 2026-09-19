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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.SwapVert
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
import com.tvsencilla.iptv.domain.model.Channel
import com.tvsencilla.iptv.ui.components.BigButton
import com.tvsencilla.iptv.ui.components.ChannelListItem
import com.tvsencilla.iptv.ui.components.DialledNumberOverlay
import com.tvsencilla.iptv.ui.components.MessageBanner
import com.tvsencilla.iptv.ui.live.LiveTvViewModel
import com.tvsencilla.iptv.ui.live.text
import com.tvsencilla.iptv.ui.remote.RemoteKey
import com.tvsencilla.iptv.ui.remote.RemoteKeyBus
import com.tvsencilla.iptv.ui.remote.RemoteKeyHandler
import com.tvsencilla.iptv.ui.theme.overscan

@Composable
fun LivePlayerScreen(
    remoteKeyBus: RemoteKeyBus,
    onLeavePlayer: () -> Unit,
    /** Si es true, Atrás sale del reproductor sin pasar antes por la lista de canales. */
    backLeavesDirectly: Boolean = false,
    viewModel: LivePlayerViewModel = hiltViewModel(),
    channelListViewModel: LiveTvViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val player by viewModel.player.collectAsStateWithLifecycle()
    val nowNext by viewModel.nowNext.collectAsStateWithLifecycle()
    val dialledDigits by viewModel.dialledDigits.collectAsStateWithLifecycle()
    val listState by channelListViewModel.state.collectAsStateWithLifecycle()

    val surfaceFocus = remember { FocusRequester() }
    val firstBannerAction = remember { FocusRequester() }

    // El cursor va a los botones de la barra al abrirla, y vuelve al vídeo al cerrarla.
    LaunchedEffect(state.isChannelListVisible, state.isBannerInteractive, state.isBannerVisible) {
        when {
            state.isChannelListVisible -> Unit
            state.isBannerInteractive && state.isBannerVisible -> runCatching { firstBannerAction.requestFocus() }
            else -> runCatching { surfaceFocus.requestFocus() }
        }
    }

    RemoteKeyHandler(remoteKeyBus) { key ->
        when (key) {
            is RemoteKey.Digit -> viewModel.onDigit(key.value)
            RemoteKey.ChannelUp -> viewModel.channelUp()
            RemoteKey.ChannelDown -> viewModel.channelDown()
            RemoteKey.Info -> viewModel.showBanner()
            RemoteKey.PlayPause, RemoteKey.Play, RemoteKey.Pause -> viewModel.showBanner()
            else -> Unit
        }
    }

    // Atrás cierra lo que esté abierto, de dentro afuera: la barra, luego la lista de canales
    // (que se abre si no lo estaba) y solo desde la lista se sale del reproductor.
    BackHandler {
        when {
            viewModel.onCancelDialling() -> Unit
            state.isBannerInteractive -> viewModel.hideBanner()
            state.isChannelListVisible || backLeavesDirectly -> onLeavePlayer()
            else -> viewModel.openChannelList()
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        PlayerSurface(player = player, modifier = Modifier.fillMaxSize())

        // Invisible focus target that turns the D-pad into channel keys during full screen.
        Box(
            Modifier
                .fillMaxSize()
                .focusRequester(surfaceFocus)
                .focusable()
                .onKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                    when (event.key) {
                        Key.DirectionUp -> { viewModel.channelUp(); true }
                        Key.DirectionDown -> { viewModel.channelDown(); true }
                        Key.DirectionCenter, Key.Enter -> {
                            if (!viewModel.onConfirm()) viewModel.openBannerActions()
                            true
                        }
                        Key.DirectionLeft -> { viewModel.goToPreviousChannel(); true }
                        else -> false
                    }
                },
        )

        if (state.isBuffering && !state.isReconnecting && state.errorMessage == null && !state.isChannelListVisible) {
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

        if (dialledDigits != null) {
            DialledNumberOverlay(
                digits = dialledDigits!!,
                modifier = Modifier.align(Alignment.TopEnd).padding(28.dp),
            )
        }

        state.notFoundNumber?.let { number ->
            MessageBanner(
                message = stringResource(R.string.error_channel_not_found, number),
                modifier = Modifier.align(Alignment.BottomCenter).padding(28.dp),
            )
        }

        state.favoriteNotice?.let { notice ->
            MessageBanner(
                message = notice.text(),
                modifier = Modifier.align(Alignment.TopCenter).padding(28.dp),
            )
        }

        val channel = state.channel
        if (state.isBannerVisible && channel != null && !state.isChannelListVisible) {
            Column(
                modifier = Modifier.align(Alignment.BottomCenter).overscan(),
            ) {
                ChannelBanner(
                    channel = channel,
                    remoteNumber = state.remoteNumber,
                    nowNext = nowNext,
                )
                Spacer(Modifier.height(10.dp))
                if (!state.isBannerInteractive) {
                    // Nada de menús ocultos: la barra dice cómo llegar a sus opciones.
                    Text(
                        text = stringResource(R.string.live_banner_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .padding(bottom = 10.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black.copy(alpha = 0.86f))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BigButton(
                        text = stringResource(
                            if (channel.isFavorite) R.string.favorites_remove else R.string.favorites_add,
                        ),
                        icon = if (channel.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        onClick = viewModel::toggleFavorite,
                        minHeight = 56.dp,
                        modifier = Modifier.focusRequester(firstBannerAction),
                    )
                    if (state.hasPreviousChannel) {
                        BigButton(
                            text = stringResource(R.string.live_previous_channel),
                            icon = Icons.Default.SwapVert,
                            onClick = viewModel::goToPreviousChannel,
                            minHeight = 56.dp,
                        )
                    }
                    if (state.canWatchFromStart && nowNext.now != null) {
                        BigButton(
                            text = stringResource(R.string.live_catch_up),
                            icon = Icons.Default.Replay,
                            onClick = viewModel::watchCurrentProgrammeFromStart,
                            minHeight = 56.dp,
                        )
                    }
                    BigButton(
                        text = stringResource(R.string.live_open_list),
                        onClick = viewModel::openChannelList,
                        minHeight = 56.dp,
                    )
                }
            }
        }

        if (state.isChannelListVisible) {
            ChannelListOverlay(
                channels = listState.allChannels,
                currentChannelId = channel?.id,
                onSelect = viewModel::play,
                onClose = viewModel::closeChannelList,
            )
        }
    }
}

/** The list slides over the video, which keeps playing behind it. */
@Composable
private fun ChannelListOverlay(
    channels: List<Channel>,
    currentChannelId: String?,
    onSelect: (Channel) -> Unit,
    onClose: () -> Unit,
) {
    val listState = rememberLazyListState()
    val firstItemFocus = remember { FocusRequester() }

    LaunchedEffect(channels, currentChannelId) {
        val index = channels.indexOfFirst { it.id == currentChannelId }
        if (index > 0) listState.scrollToItem(index)
        runCatching { firstItemFocus.requestFocus() }
    }

    Row(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .width(560.dp)
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.94f))
                .overscan(),
        ) {
            Column(Modifier.fillMaxSize()) {
                Text(
                    text = stringResource(R.string.player_channel_list),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 14.dp),
                )
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    items(channels, key = { it.id }) { item ->
                        ChannelListItem(
                            number = item.favoriteNumber ?: item.listNumber,
                            name = item.name,
                            logoUrl = item.logoUrl,
                            isFavorite = item.isFavorite,
                            onClick = { onSelect(item) },
                            modifier = if (item.id == currentChannelId) {
                                Modifier.focusRequester(firstItemFocus)
                            } else {
                                Modifier
                            },
                        )
                    }
                }
                BigButton(
                    text = stringResource(R.string.common_close),
                    onClick = onClose,
                    minHeight = 56.dp,
                    modifier = Modifier.padding(top = 16.dp).fillMaxWidth(),
                )
            }
        }
        Spacer(
            Modifier
                .weight(1f)
                .fillMaxSize()
                .clip(RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp)),
        )
    }
}
