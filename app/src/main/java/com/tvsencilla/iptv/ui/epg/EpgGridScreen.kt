package com.tvsencilla.iptv.ui.epg

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.tvsencilla.iptv.R
import com.tvsencilla.iptv.domain.model.displayName
import com.tvsencilla.iptv.ui.components.BigButton
import com.tvsencilla.iptv.ui.components.EmptyState
import com.tvsencilla.iptv.ui.components.FocusableSurface
import com.tvsencilla.iptv.ui.components.SectionTitle
import com.tvsencilla.iptv.ui.components.TvScreen
import com.tvsencilla.iptv.ui.theme.FocusYellow
import com.tvsencilla.iptv.ui.util.formatHourMinute

/**
 * The full guide, offered only as an option in Settings. Day-to-day use is meant to rely on the
 * simpler "Ahora / Después" panel instead.
 */
@Composable
fun EpgGridScreen(
    viewModel: EpgGridViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val now = System.currentTimeMillis()

    TvScreen(title = stringResource(R.string.settings_epg_grid)) {
        Row(Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.width(260.dp).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.channels, key = { it.id }) { channel ->
                    BigButton(
                        text = "${channel.listNumber}  ${channel.displayName}",
                        onClick = { viewModel.selectChannel(channel.id) },
                        minHeight = 56.dp,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(Modifier.weight(1f).fillMaxHeight()) {
                SectionTitle(state.selectedChannel?.displayName ?: stringResource(R.string.live_title))
                Spacer(Modifier.padding(top = 16.dp))

                if (state.schedule.isEmpty()) {
                    EmptyState(stringResource(R.string.live_no_epg))
                    return@Column
                }

                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.schedule, key = { it.startMillis }) { program ->
                        val isLive = program.isLiveAt(now)
                        FocusableSurface(
                            onClick = { },
                            modifier = Modifier.fillMaxWidth(),
                        ) { _ ->
                            Row {
                                Text(
                                    text = formatHourMinute(program.startMillis),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (isLive) FocusYellow else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.width(92.dp),
                                )
                                Text(
                                    text = program.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f),
                                )
                                if (isLive) {
                                    Text(
                                        text = stringResource(R.string.live_now),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = FocusYellow,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
