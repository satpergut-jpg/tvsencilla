package com.tvsencilla.iptv.ui.series

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.tvsencilla.iptv.R
import com.tvsencilla.iptv.ui.components.BigButton
import com.tvsencilla.iptv.ui.components.EmptyState
import com.tvsencilla.iptv.ui.components.FocusableSurface
import com.tvsencilla.iptv.ui.components.LoadingState
import com.tvsencilla.iptv.ui.components.SectionTitle
import com.tvsencilla.iptv.ui.components.TvScreen
import com.tvsencilla.iptv.ui.theme.FocusYellow
import com.tvsencilla.iptv.ui.theme.SoftGreen
import com.tvsencilla.iptv.ui.util.formatDurationMinutes

@Composable
fun SeriesDetailScreen(
    onPlayEpisode: (episodeId: String, resume: Boolean) -> Unit,
    viewModel: SeriesDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    if (state.isLoading) {
        LoadingState()
        return
    }

    val series = state.series
    if (series == null || state.seasons.isEmpty()) {
        EmptyState(stringResource(R.string.common_empty))
        return
    }

    TvScreen(
        title = series.title,
        trailing = {
            BigButton(
                text = stringResource(if (state.isLiked) R.string.likes_remove else R.string.likes_add),
                icon = if (state.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                onClick = viewModel::toggleLike,
                minHeight = 52.dp,
            )
        },
    ) {
        Column(Modifier.fillMaxSize()) {
            if (state.seasons.size > 1) {
                SectionTitle(stringResource(R.string.series_seasons))
                Spacer(Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(state.seasons, key = { it.number }) { season ->
                        val label = stringResource(R.string.series_season, season.number)
                        BigButton(
                            text = if (season.number == state.selectedSeasonNumber) "✓  $label" else label,
                            onClick = { viewModel.selectSeason(season.number) },
                            minHeight = 54.dp,
                            modifier = Modifier.width(180.dp),
                        )
                    }
                }
                Spacer(Modifier.height(18.dp))
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().fillMaxHeight(),
            ) {
                items(state.episodes, key = { it.id }) { episode ->
                    val isWatched = episode.id in state.watchedEpisodeIds
                    FocusableSurface(
                        onClick = { onPlayEpisode(episode.id, true) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { _ ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = episode.episodeNumber.toString(),
                                style = MaterialTheme.typography.titleLarge,
                                color = FocusYellow,
                                modifier = Modifier.width(52.dp),
                            )
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = episode.title.ifBlank {
                                        stringResource(R.string.series_episode_number, episode.episodeNumber)
                                    },
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                val duration = formatDurationMinutes(episode.durationMinutes)
                                if (duration != null) {
                                    Text(
                                        text = duration,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            if (isWatched) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = stringResource(R.string.series_watched),
                                    tint = SoftGreen,
                                    modifier = Modifier.width(34.dp),
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = stringResource(R.string.player_play),
                                modifier = Modifier.width(34.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
