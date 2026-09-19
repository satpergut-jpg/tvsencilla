package com.tvsencilla.iptv.ui.movies

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.tvsencilla.iptv.R
import com.tvsencilla.iptv.ui.components.BigButton
import com.tvsencilla.iptv.ui.components.EmptyState
import com.tvsencilla.iptv.ui.components.LoadingState
import com.tvsencilla.iptv.ui.components.RemoteImage
import com.tvsencilla.iptv.ui.components.SectionTitle
import com.tvsencilla.iptv.ui.components.TvScreen
import com.tvsencilla.iptv.ui.util.formatDurationMinutes

@Composable
fun MovieDetailScreen(
    onPlay: (movieId: String, resume: Boolean) -> Unit,
    viewModel: MovieDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val primaryAction = remember { FocusRequester() }

    LaunchedEffect(state.movie) {
        if (state.movie != null) runCatching { primaryAction.requestFocus() }
    }

    if (state.isLoading) {
        LoadingState()
        return
    }

    val movie = state.movie
    if (movie == null) {
        EmptyState(stringResource(R.string.error_generic))
        return
    }

    TvScreen(title = movie.title) {
        Row(Modifier.fillMaxSize()) {
            RemoteImage(
                url = movie.posterUrl,
                contentDescription = movie.title,
                modifier = Modifier
                    .width(200.dp)
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(12.dp)),
            )

            Spacer(Modifier.width(24.dp))

            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                val facts = listOfNotNull(
                    movie.year?.let { stringResource(R.string.common_year, it) },
                    formatDurationMinutes(movie.durationMinutes)
                        ?.let { stringResource(R.string.movies_duration, it) },
                )
                if (facts.isNotEmpty()) {
                    Text(
                        text = facts.joinToString("  ·  "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(14.dp))
                }

                if (!movie.plot.isNullOrBlank()) {
                    SectionTitle(stringResource(R.string.movies_synopsis))
                    Spacer(Modifier.height(8.dp))
                    Text(text = movie.plot, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(20.dp))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    if (state.canResume) {
                        BigButton(
                            text = stringResource(R.string.movies_continue),
                            icon = Icons.Default.PlayArrow,
                            onClick = { onPlay(movie.id, true) },
                            modifier = Modifier.weight(1f).focusRequester(primaryAction),
                        )
                        BigButton(
                            text = stringResource(R.string.movies_restart),
                            icon = Icons.Default.Replay,
                            onClick = { onPlay(movie.id, false) },
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        BigButton(
                            text = stringResource(R.string.movies_watch),
                            icon = Icons.Default.PlayArrow,
                            onClick = { onPlay(movie.id, false) },
                            modifier = Modifier.weight(1f).focusRequester(primaryAction),
                        )
                    }
                    // Justo al lado de "Ver": a un paso a la derecha con el mando.
                    BigButton(
                        text = stringResource(if (state.isLiked) R.string.likes_remove else R.string.likes_add),
                        icon = if (state.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        onClick = viewModel::toggleLike,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
