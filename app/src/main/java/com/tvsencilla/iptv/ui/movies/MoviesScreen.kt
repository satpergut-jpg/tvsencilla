package com.tvsencilla.iptv.ui.movies

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tvsencilla.iptv.R
import com.tvsencilla.iptv.ui.components.EmptyState
import com.tvsencilla.iptv.ui.components.ErrorState
import com.tvsencilla.iptv.ui.components.LoadingState
import com.tvsencilla.iptv.ui.components.PosterGrid
import com.tvsencilla.iptv.ui.components.TvScreen
import com.tvsencilla.iptv.ui.components.VodCategoryColumn

@Composable
fun MoviesScreen(
    onOpenMovie: (String) -> Unit,
    viewModel: MoviesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    TvScreen(title = stringResource(R.string.movies_title)) {
        when {
            state.isLoading -> LoadingState()

            state.errorMessage != null -> ErrorState(
                message = stringResource(state.errorMessage!!),
                onRetry = { viewModel.refresh(force = true) },
            )

            state.catalogIsEmpty -> EmptyState(stringResource(R.string.common_empty))

            else -> Row(Modifier.fillMaxSize()) {
                VodCategoryColumn(
                    categories = state.categories,
                    selectedCategoryId = state.selectedCategoryId,
                    allLabel = stringResource(R.string.movies_all),
                    onSelect = viewModel::selectCategory,
                    modifier = Modifier.width(220.dp).fillMaxHeight(),
                )

                Spacer(Modifier.width(16.dp))

                if (state.movies.isEmpty()) {
                    EmptyState(
                        message = stringResource(
                            if (state.showingLiked) R.string.likes_empty else R.string.common_empty,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    PosterGrid(
                        items = state.movies,
                        key = { it.id },
                        title = { it.title },
                        subtitle = { it.year },
                        posterUrl = { it.posterUrl },
                        onClick = { onOpenMovie(it.id) },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            }
        }
    }
}
