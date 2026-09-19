package com.tvsencilla.iptv.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp

/**
 * Rejilla de carátulas de Películas y Series, cinco por fila.
 *
 * Al entrar, el cursor ya está en una carátula; y al volver de una ficha, está en la que se había
 * abierto, para no tener que recorrer otra vez una lista de cientos de títulos.
 */
@Composable
fun <T> PosterGrid(
    items: List<T>,
    key: (T) -> Any,
    title: (T) -> String,
    subtitle: (T) -> String?,
    posterUrl: (T) -> String?,
    onClick: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val gridState = rememberLazyGridState()
    var lastOpenedIndex by rememberSaveable { mutableIntStateOf(0) }
    val initialFocus = remember { FocusRequester() }
    val focusIndex = lastOpenedIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))

    LaunchedEffect(Unit) {
        if (items.isEmpty()) return@LaunchedEffect
        gridState.scrollToItem(focusIndex)
        runCatching { initialFocus.requestFocus() }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(COLUMNS),
        state = gridState,
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        itemsIndexed(items, key = { _, item -> key(item) }) { index, item ->
            PosterCard(
                title = title(item),
                subtitle = subtitle(item),
                posterUrl = posterUrl(item),
                onClick = {
                    lastOpenedIndex = index
                    onClick(item)
                },
                modifier = if (index == focusIndex) Modifier.focusRequester(initialFocus) else Modifier,
            )
        }
    }
}

private const val COLUMNS = 5
