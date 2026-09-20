package com.tvsencilla.iptv.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tvsencilla.iptv.BuildConfig
import com.tvsencilla.iptv.R
import com.tvsencilla.iptv.domain.model.ContinueWatchingItem
import com.tvsencilla.iptv.ui.components.BigButton
import com.tvsencilla.iptv.ui.components.FocusableSurface
import com.tvsencilla.iptv.ui.theme.Tint
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.tvsencilla.iptv.ui.components.ConfirmDialog
import com.tvsencilla.iptv.ui.components.PosterCard
import com.tvsencilla.iptv.ui.components.SectionTitle
import com.tvsencilla.iptv.ui.components.TvScreen

/**
 * Tres destinos, siempre en el mismo orden y en el mismo sitio; los favoritos viven dentro de TV en
 * directo. Lo que el proveedor no ofrece no se enseña, en lugar de mostrarse desactivado.
 */
@Composable
fun HomeScreen(
    onLiveTv: () -> Unit,
    onMovies: () -> Unit,
    onSeries: () -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
    onContinueWatching: (ContinueWatchingItem) -> Unit,
    onJumpToChannel: (String) -> Unit,
    onExit: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showExitDialog by remember { mutableStateOf(false) }
    val firstButton = remember { FocusRequester() }

    LaunchedEffect(state.jumpToChannelId) {
        val channelId = state.jumpToChannelId ?: return@LaunchedEffect
        viewModel.onJumpConsumed()
        onJumpToChannel(channelId)
    }

    LaunchedEffect(Unit) { firstButton.requestFocus() }

    // Back never closes the app silently.
    BackHandler(enabled = !showExitDialog) { showExitDialog = true }

    if (BuildConfig.SOLO_DIRECTO) {
        TvScreen(
            title = stringResource(R.string.app_name),
            trailing = {
                BigButton(
                    text = stringResource(R.string.home_settings),
                    icon = Icons.Default.Settings,
                    onClick = onSettings,
                    minHeight = 52.dp,
                )
            },
        ) {
            // Solo dos destinos, en grande: primero Buscar y luego TV en directo.
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            ) {
                BigButton(
                    text = stringResource(R.string.common_search),
                    icon = Icons.Default.Search,
                    onClick = onSearch,
                    minHeight = 200.dp,
                    modifier = Modifier.weight(1f).focusRequester(firstButton),
                )
                BigButton(
                    text = stringResource(R.string.home_live_tv),
                    icon = Icons.Default.LiveTv,
                    onClick = onLiveTv,
                    minHeight = 200.dp,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    } else TvScreen(
        title = stringResource(R.string.app_name),
        trailing = {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BigButton(
                    text = stringResource(R.string.common_search),
                    icon = Icons.Default.Search,
                    onClick = onSearch,
                    minHeight = 52.dp,
                    brush = Tint.Search,
                )
                BigButton(
                    text = stringResource(R.string.home_settings),
                    icon = Icons.Default.Settings,
                    onClick = onSettings,
                    minHeight = 52.dp,
                    brush = Tint.Neutral,
                )
            }
        },
    ) {
        Column {
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp), modifier = Modifier.fillMaxWidth()) {
                HomeTile(
                    text = stringResource(R.string.home_live_tv),
                    icon = Icons.Default.LiveTv,
                    brush = Tint.Live,
                    onClick = onLiveTv,
                    modifier = Modifier.weight(1f).focusRequester(firstButton),
                )
                if (state.showMovies) {
                    HomeTile(
                        text = stringResource(R.string.home_movies),
                        icon = Icons.Default.Movie,
                        brush = Tint.Movies,
                        onClick = onMovies,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (state.showSeries) {
                    HomeTile(
                        text = stringResource(R.string.home_series),
                        icon = Icons.Default.Tv,
                        brush = Tint.Series,
                        onClick = onSeries,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            if (state.continueWatching.isNotEmpty()) {
                Spacer(Modifier.height(22.dp))
                SectionTitle(stringResource(R.string.home_continue_watching))
                Spacer(Modifier.height(12.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(end = 32.dp),
                ) {
                    items(state.continueWatching, key = { it.itemId }) { item ->
                        PosterCard(
                            title = item.title,
                            subtitle = item.subtitle,
                            posterUrl = item.posterUrl,
                            progress = item.fraction,
                            onClick = { onContinueWatching(item) },
                            // Más estrecha que en las rejillas: debajo de los botones de Inicio
                            // solo queda sitio para una fila, y con el ancho normal el título
                            // se salía por abajo de la pantalla.
                            modifier = Modifier.width(CONTINUE_WATCHING_WIDTH),
                        )
                    }
                }
            }
        }
    }

    if (showExitDialog) {
        ConfirmDialog(
            title = stringResource(R.string.home_exit_title),
            confirmText = stringResource(R.string.home_exit_confirm),
            dismissText = stringResource(R.string.home_exit_cancel),
            onConfirm = onExit,
            onDismiss = { showExitDialog = false },
        )
    }
}

/** Tarjeta grande de color, con el icono encima del nombre, como los iconos de Apple TV. */
@Composable
private fun HomeTile(
    text: String,
    icon: ImageVector,
    brush: Brush,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FocusableSurface(
        onClick = onClick,
        brush = brush,
        shape = RoundedCornerShape(24.dp),
        contentAlignment = Alignment.Center,
        modifier = modifier.height(132.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(44.dp))
            Spacer(Modifier.height(10.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private val CONTINUE_WATCHING_WIDTH = 112.dp
