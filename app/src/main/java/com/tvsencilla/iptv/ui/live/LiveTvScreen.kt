package com.tvsencilla.iptv.ui.live

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.tvsencilla.iptv.R
import com.tvsencilla.iptv.domain.model.Channel
import com.tvsencilla.iptv.domain.model.displayName
import com.tvsencilla.iptv.ui.components.BigButton
import com.tvsencilla.iptv.ui.components.ChannelListItem
import com.tvsencilla.iptv.ui.components.DialledNumberOverlay
import com.tvsencilla.iptv.ui.components.EmptyState
import com.tvsencilla.iptv.ui.components.ErrorState
import com.tvsencilla.iptv.ui.components.LoadingState
import com.tvsencilla.iptv.ui.components.MessageBanner
import com.tvsencilla.iptv.ui.components.SectionTitle
import com.tvsencilla.iptv.ui.components.TvScreen
import com.tvsencilla.iptv.ui.remote.RemoteKey
import com.tvsencilla.iptv.ui.remote.RemoteKeyBus
import com.tvsencilla.iptv.ui.remote.RemoteKeyHandler
import com.tvsencilla.iptv.ui.util.formatHourMinute
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import com.tvsencilla.iptv.ui.components.NavDestination
import com.tvsencilla.iptv.ui.components.NavRail
import com.tvsencilla.iptv.ui.components.ProgressStripe
import com.tvsencilla.iptv.ui.components.RemoteImage
import com.tvsencilla.iptv.ui.components.SquareLogoTile
import com.tvsencilla.iptv.ui.theme.PremiumLook
import com.tvsencilla.iptv.ui.theme.Tint
import kotlinx.coroutines.delay

@Composable
fun LiveTvScreen(
    remoteKeyBus: RemoteKeyBus,
    onPlayChannel: (Channel) -> Unit,
    onReorderFavorites: () -> Unit,
    onOpenFullGuide: (channelId: String) -> Unit,
    onSearch: () -> Unit,
    onMovies: () -> Unit,
    onSeries: () -> Unit,
    onSettings: () -> Unit,
    viewModel: LiveTvViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focused by viewModel.focused.collectAsStateWithLifecycle()
    val dialledDigits by viewModel.tuner.dialledDigits.collectAsStateWithLifecycle()
    val notFoundNumber by viewModel.notFoundNumber.collectAsStateWithLifecycle()
    val favoriteNotice by viewModel.favoriteNotice.collectAsStateWithLifecycle()

    // Typing a number here tunes straight away, exactly as it does during playback.
    LaunchedEffect(Unit) {
        viewModel.tuner.tuneTo.collect(onPlayChannel)
    }

    RemoteKeyHandler(remoteKeyBus) { key ->
        when (key) {
            is RemoteKey.Digit -> viewModel.tuner.onDigit(key.value)
            RemoteKey.ChannelUp -> viewModel.tuner.channelUp(focused?.channel?.id)?.let(onPlayChannel)
            RemoteKey.ChannelDown -> viewModel.tuner.channelDown(focused?.channel?.id)?.let(onPlayChannel)
            else -> Unit
        }
    }

    TvScreen(
        title = stringResource(R.string.live_title),
        trailing = {
            Text(
                text = pluralStringResource(R.plurals.live_channel_count, state.channels.size, state.channels.size),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
    ) {
        Box(Modifier.fillMaxSize()) {
            when {
                state.isLoading -> LoadingState()

                state.errorMessage != null -> ErrorState(
                    message = stringResource(state.errorMessage!!),
                    onRetry = { viewModel.refresh(force = true) },
                )

                state.allChannels.isEmpty() -> EmptyState(stringResource(R.string.error_empty_list))

                else -> {
                    val channelsAndPanel: @Composable (Modifier) -> Unit = { boxModifier ->
                        Row(boxModifier) {
                            if (state.channels.isEmpty() && state.showingFavorites) {
                                // Sin favoritos todavía: se explica cómo añadirlos, en su sitio.
                                EmptyState(
                                    message = stringResource(R.string.favorites_empty),
                                    modifier = Modifier.weight(1f),
                                )
                            } else {
                                ChannelColumn(
                                    state = state,
                                    restoreFocusToId = viewModel.lastFocusedChannelId,
                                    onFocused = viewModel::onChannelFocused,
                                    onClick = onPlayChannel,
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                )
                            }
                            Spacer(Modifier.width(16.dp))
                            FocusedChannelPanel(
                                focused = focused,
                                showFullGuide = state.showFullGuide,
                                onPlay = { onPlayChannel(it) },
                                onToggleFavorite = viewModel::toggleFavorite,
                                onOpenFullGuide = { onOpenFullGuide(it.id) },
                                modifier = Modifier.width(300.dp).fillMaxHeight(),
                            )
                        }
                    }

                    if (PremiumLook) {
                        // Premium: rail global a la izquierda (como en Inicio) y, en vez de una
                        // lista filtrable por categoría, los canales agrupados por su categoría en
                        // una rejilla de logos cuadrados.
                        Row(Modifier.fillMaxSize()) {
                            NavRail(
                                current = NavDestination.LIVE,
                                showMovies = state.showMovies,
                                showSeries = state.showSeries,
                                onSearch = onSearch,
                                onLiveTv = {},
                                onMovies = onMovies,
                                onSeries = onSeries,
                                onSettings = onSettings,
                                modifier = Modifier.fillMaxHeight(),
                            )
                            Spacer(Modifier.width(20.dp))
                            ChannelGroups(
                                state = state,
                                onReorderFavorites = onReorderFavorites,
                                onFocused = viewModel::onChannelFocused,
                                onClick = onPlayChannel,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                            )
                        }
                    } else {
                        Row(Modifier.fillMaxSize()) {
                            CategoryColumn(
                                state = state,
                                onSelect = viewModel::selectCategory,
                                onReorderFavorites = onReorderFavorites,
                                modifier = Modifier.width(220.dp).fillMaxHeight(),
                            )
                            Spacer(Modifier.width(16.dp))
                            channelsAndPanel(Modifier.weight(1f).fillMaxHeight())
                        }
                    }
                }
            }

            if (dialledDigits != null) {
                DialledNumberOverlay(
                    digits = dialledDigits!!,
                    modifier = Modifier.align(Alignment.TopEnd),
                )
            }

            val missing = notFoundNumber
            if (missing != null) {
                LaunchedEffect(missing) {
                    delay(NOT_FOUND_VISIBLE_MILLIS)
                    viewModel.dismissNotFound()
                }
                MessageBanner(
                    message = stringResource(R.string.error_channel_not_found, missing),
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }

            favoriteNotice?.let { notice ->
                MessageBanner(
                    message = notice.text(),
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}

@Composable
fun FavoriteNotice.text(): String =
    if (addedAsNumber != null) {
        stringResource(R.string.favorites_added, channelName, addedAsNumber)
    } else {
        stringResource(R.string.favorites_removed, channelName)
    }

/**
 * Rejilla del acabado premium: los canales de cada categoría en su propia sección, como fichas
 * cuadradas con el logo. Los favoritos, si hay, encabezan la lista como una sección más.
 */
@Composable
private fun ChannelGroups(
    state: LiveTvUiState,
    onReorderFavorites: () -> Unit,
    onFocused: (Channel) -> Unit,
    onClick: (Channel) -> Unit,
    modifier: Modifier = Modifier,
) {
    val initialFocus = remember { FocusRequester() }
    val favorites = remember(state.allChannels) { state.allChannels.filter { it.isFavorite } }
    val otherChannelsTitle = stringResource(R.string.live_other_channels)
    // Un canal cuya categoría no está en la lista (sin categoría, o su categoría quedó oculta)
    // no debe desaparecer sin más: cae en "Otros canales" para que siga siendo alcanzable.
    val groups = remember(state.allChannels, state.categories, otherChannelsTitle) {
        val knownCategoryIds = state.categories.map { it.id }.toSet()
        val byCategory = state.allChannels.groupBy { it.categoryId }
        val known = state.categories.mapNotNull { category ->
            byCategory[category.id]?.takeIf { it.isNotEmpty() }
                ?.let { ChannelSection(category.id, displayName(category.name), it) }
        }
        val other = state.allChannels.filter { it.categoryId == null || it.categoryId !in knownCategoryIds }
        if (other.isNotEmpty()) {
            known + ChannelSection(OTHER_CATEGORY_ID, otherChannelsTitle, other)
        } else {
            known
        }
    }

    LaunchedEffect(Unit) { runCatching { initialFocus.requestFocus() } }

    if (favorites.isEmpty() && groups.isEmpty()) {
        EmptyState(message = stringResource(R.string.error_empty_list), modifier = modifier)
        return
    }

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(26.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        if (favorites.isNotEmpty()) {
            item(key = "favorites") {
                ChannelGroupSection(
                    title = stringResource(R.string.live_category_favorites),
                    channels = favorites,
                    firstItemFocusRequester = initialFocus,
                    onFocused = onFocused,
                    onClick = onClick,
                    trailing = if (favorites.size > 1) {
                        {
                            BigButton(
                                text = stringResource(R.string.favorites_reorder),
                                icon = Icons.Default.SwapVert,
                                onClick = onReorderFavorites,
                                minHeight = 40.dp,
                            )
                        }
                    } else {
                        null
                    },
                )
            }
        }
        itemsIndexed(groups, key = { _, group -> group.id }) { index, section ->
            ChannelGroupSection(
                title = section.title,
                channels = section.channels,
                firstItemFocusRequester = if (favorites.isEmpty() && index == 0) initialFocus else null,
                onFocused = onFocused,
                onClick = onClick,
            )
        }
    }
}

/** Una sección de la rejilla: una categoría real, o el cajón "Otros canales" sin categoría. */
private data class ChannelSection(val id: String, val title: String, val channels: List<Channel>)

private const val OTHER_CATEGORY_ID = "__otros__"

@Composable
private fun ChannelGroupSection(
    title: String,
    channels: List<Channel>,
    firstItemFocusRequester: FocusRequester?,
    onFocused: (Channel) -> Unit,
    onClick: (Channel) -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionTitle(title, modifier = Modifier.weight(1f))
            trailing?.invoke()
        }
        Spacer(Modifier.height(10.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp, end = 32.dp),
        ) {
            itemsIndexed(channels, key = { _, channel -> channel.id }) { index, channel ->
                SquareLogoTile(
                    name = channel.name,
                    logoUrl = channel.logoUrl,
                    isFavorite = channel.isFavorite,
                    onClick = { onClick(channel) },
                    modifier = Modifier
                        .width(128.dp)
                        .then(
                            if (firstItemFocusRequester != null && index == 0) {
                                Modifier.focusRequester(firstItemFocusRequester)
                            } else {
                                Modifier
                            },
                        )
                        .onFocusChanged { if (it.isFocused) onFocused(channel) },
                )
            }
        }
    }
}

@Composable
private fun CategoryColumn(
    state: LiveTvUiState,
    onSelect: (String?) -> Unit,
    onReorderFavorites: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // La categoría elegida lleva una marca escrita, no solo un color: así se sabe siempre dónde se está.
    fun label(text: String, selected: Boolean) = if (selected) "✓  $text" else text

    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            BigButton(
                text = label(stringResource(R.string.live_category_favorites), state.showingFavorites),
                onClick = { onSelect(FAVORITES_CATEGORY_ID) },
                minHeight = 56.dp,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (state.showingFavorites && state.hasFavorites) {
            item {
                BigButton(
                    text = stringResource(R.string.favorites_reorder),
                    icon = Icons.Default.SwapVert,
                    onClick = onReorderFavorites,
                    minHeight = 52.dp,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        item {
            BigButton(
                text = label(stringResource(R.string.live_all_categories), state.selectedCategoryId == null),
                onClick = { onSelect(null) },
                minHeight = 56.dp,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        items(state.categories, key = { it.id }) { category ->
            BigButton(
                text = label(displayName(category.name), state.selectedCategoryId == category.id),
                onClick = { onSelect(category.id) },
                minHeight = 56.dp,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ChannelColumn(
    state: LiveTvUiState,
    restoreFocusToId: String?,
    onFocused: (Channel) -> Unit,
    onClick: (Channel) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val initialFocus = remember { FocusRequester() }
    val initialIndex = state.channels.indexOfFirst { it.id == restoreFocusToId }.coerceAtLeast(0)

    // Al entrar, el cursor ya está en un canal (y al volver del reproductor, en el que se veía):
    // sin esto no hay nada resaltado y no se sabe dónde se está hasta pulsar una flecha.
    LaunchedEffect(Unit) {
        if (state.channels.isEmpty()) return@LaunchedEffect
        listState.scrollToItem(initialIndex)
        runCatching { initialFocus.requestFocus() }
    }

    LazyColumn(state = listState, modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        itemsIndexed(state.channels, key = { _, channel -> channel.id }) { index, channel ->
            ChannelListItem(
                number = state.numberShownFor(channel),
                name = channel.name,
                logoUrl = channel.logoUrl,
                isFavorite = channel.isFavorite,
                onClick = { onClick(channel) },
                modifier = Modifier
                    .then(if (index == initialIndex) Modifier.focusRequester(initialFocus) else Modifier)
                    .onFocusChanged { if (it.isFocused) onFocused(channel) },
            )
        }
    }
}

/** The panel is reached by pressing right from the list, which keeps every action on the D-pad. */
@Composable
private fun FocusedChannelPanel(
    focused: FocusedChannel?,
    showFullGuide: Boolean,
    onPlay: (Channel) -> Unit,
    onToggleFavorite: (Channel) -> Unit,
    onOpenFullGuide: (Channel) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (focused == null) {
        Spacer(modifier)
        return
    }

    Column(modifier = modifier) {
        if (PremiumLook) {
            RemoteImage(
                url = focused.channel.logoUrl,
                contentDescription = focused.channel.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Tint.Card)
                    .padding(12.dp),
            )
            Spacer(Modifier.height(12.dp))
        }
        Text(
            text = focused.channel.displayName,
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(14.dp))

        SectionTitle(stringResource(R.string.live_now))
        val now = focused.nowNext.now
        Text(
            text = if (now != null) {
                "${now.title} · ${stringResource(R.string.live_until, formatHourMinute(now.endMillis))}"
            } else {
                stringResource(R.string.live_no_epg)
            },
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp),
        )
        if (now != null) {
            ProgressStripe(
                progress = now.progressAt(System.currentTimeMillis()),
                modifier = Modifier.padding(top = 8.dp).clip(RoundedCornerShape(3.dp)),
            )
        }

        Spacer(Modifier.height(12.dp))

        SectionTitle(stringResource(R.string.live_next))
        val next = focused.nowNext.next
        Text(
            text = if (next != null) {
                "${formatHourMinute(next.startMillis)} · ${next.title}"
            } else {
                stringResource(R.string.live_no_epg)
            },
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp),
        )

        Spacer(Modifier.height(12.dp))

        BigButton(
            text = stringResource(R.string.player_play),
            icon = Icons.Default.PlayArrow,
            onClick = { onPlay(focused.channel) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        BigButton(
            text = stringResource(
                if (focused.channel.isFavorite) R.string.favorites_remove else R.string.favorites_add,
            ),
            icon = if (focused.channel.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
            onClick = { onToggleFavorite(focused.channel) },
            modifier = Modifier.fillMaxWidth(),
        )
        if (showFullGuide) {
            Spacer(Modifier.height(10.dp))
            BigButton(
                text = stringResource(R.string.live_full_guide),
                icon = Icons.Default.CalendarMonth,
                onClick = { onOpenFullGuide(focused.channel) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private const val NOT_FOUND_VISIBLE_MILLIS = 8_000L
