package com.tvsencilla.iptv.ui.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
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
import com.tvsencilla.iptv.ui.components.ChannelListItem
import com.tvsencilla.iptv.ui.components.EmptyState
import com.tvsencilla.iptv.ui.components.NumberEntryDialog
import com.tvsencilla.iptv.ui.components.PinDialog
import com.tvsencilla.iptv.ui.components.SectionTitle
import com.tvsencilla.iptv.ui.components.TvScreen

/**
 * Reordering by picking a channel up: OK grabs it, up and down move it, OK puts it down. There is
 * also a direct "put it at number N" route for anyone who would rather type the position.
 */
@Composable
fun ReorderFavoritesScreen(
    onDone: () -> Unit,
    viewModel: ReorderFavoritesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var focusedChannelId by remember { mutableStateOf<String?>(null) }

    if (!state.isUnlocked) {
        PinDialog(
            title = stringResource(R.string.pin_title),
            errorMessage = state.pinError?.let { stringResource(it) },
            onSubmit = viewModel::submitPin,
            onDismiss = onDone,
            hint = stringResource(R.string.pin_default_notice),
        )
        return
    }

    TvScreen(title = stringResource(R.string.favorites_reorder)) {
        Column(Modifier.fillMaxSize()) {
            val grabbed = state.grabbedChannel
            SectionTitle(
                if (grabbed != null) {
                    stringResource(R.string.favorites_reorder_moving, grabbed.name)
                } else {
                    stringResource(R.string.favorites_reorder_help)
                },
            )
            Spacer(Modifier.height(14.dp))

            if (state.favorites.isEmpty()) {
                EmptyState(stringResource(R.string.favorites_empty))
                return@Column
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f),
            ) {
                items(state.favorites, key = { it.id }) { channel ->
                    val isGrabbed = channel.id == state.grabbedChannelId
                    ChannelListItem(
                        number = channel.favoriteNumber ?: 0,
                        name = channel.name,
                        logoUrl = channel.logoUrl,
                        isFavorite = true,
                        nowTitle = if (isGrabbed) {
                            stringResource(R.string.favorites_reorder_moving, channel.name)
                        } else {
                            null
                        },
                        onClick = { viewModel.toggleGrab(channel.id) },
                        modifier = Modifier
                            .onFocusChanged { if (it.isFocused) focusedChannelId = channel.id }
                            // While a channel is held, up and down move it instead of moving focus.
                            .onKeyEvent { event ->
                                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                                if (!isGrabbed) return@onKeyEvent false
                                when (event.key) {
                                    Key.DirectionUp -> { viewModel.moveGrabbedUp(); true }
                                    Key.DirectionDown -> { viewModel.moveGrabbedDown(); true }
                                    Key.Back -> { viewModel.releaseGrab(); true }
                                    else -> false
                                }
                            },
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                val target = state.favorites.firstOrNull { it.id == focusedChannelId }
                BigButton(
                    text = stringResource(R.string.favorites_set_number),
                    icon = Icons.Default.Numbers,
                    onClick = { target?.let(viewModel::askForNumber) },
                    enabled = target != null,
                    minHeight = 56.dp,
                    modifier = Modifier.weight(1f),
                )
                BigButton(
                    text = stringResource(R.string.favorites_remove),
                    icon = Icons.Default.Delete,
                    onClick = { target?.let { viewModel.remove(it.id) } },
                    enabled = target != null,
                    minHeight = 56.dp,
                    modifier = Modifier.weight(1f),
                )
                BigButton(
                    text = stringResource(R.string.common_save),
                    onClick = onDone,
                    minHeight = 56.dp,
                    modifier = Modifier.weight(1f),
                )
            }

            Text(
                text = stringResource(R.string.pin_protected),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    state.numberEntryFor?.let { channel ->
        NumberEntryDialog(
            title = stringResource(R.string.favorites_set_number_title, channel.name),
            hint = stringResource(R.string.favorites_set_number_hint, state.favorites.size),
            maxLength = state.favorites.size.toString().length,
            onSubmit = { position -> viewModel.setPosition(channel.id, position) },
            onDismiss = viewModel::dismissNumberEntry,
        )
    }
}
