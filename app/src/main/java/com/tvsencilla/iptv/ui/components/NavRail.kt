package com.tvsencilla.iptv.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.tvsencilla.iptv.R
import com.tvsencilla.iptv.ui.theme.Tint

/** El destino donde está el usuario ahora mismo: marca ese icono del rail como el elegido. */
enum class NavDestination { SEARCH, LIVE, MOVIES, SERIES, SETTINGS }

/**
 * Rail de navegación global: solo iconos hasta que recibe el foco; entonces se despliega con los
 * nombres. Es el mismo componente en Inicio y en cada sección, para poder saltar entre ellas sin
 * volver antes a Inicio.
 */
@Composable
fun NavRail(
    current: NavDestination?,
    showMovies: Boolean,
    showSeries: Boolean,
    onSearch: () -> Unit,
    onLiveTv: () -> Unit,
    onMovies: () -> Unit,
    onSeries: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
    firstDestination: FocusRequester? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    val width by animateDpAsState(
        targetValue = if (expanded) RAIL_EXPANDED else RAIL_COLLAPSED,
        animationSpec = tween(220),
        label = "railWidth",
    )
    Column(
        modifier = modifier
            .width(width)
            .onFocusChanged { expanded = it.hasFocus },
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
    ) {
        NavRailItem(
            text = stringResource(R.string.common_search),
            icon = Icons.Default.Search,
            expanded = expanded,
            selected = current == NavDestination.SEARCH,
            onClick = onSearch,
        )
        NavRailItem(
            text = stringResource(R.string.home_live_tv),
            icon = Icons.Default.LiveTv,
            expanded = expanded,
            selected = current == NavDestination.LIVE,
            onClick = onLiveTv,
            modifier = firstDestination?.let { Modifier.focusRequester(it) } ?: Modifier,
        )
        if (showMovies) {
            NavRailItem(
                text = stringResource(R.string.home_movies),
                icon = Icons.Default.Movie,
                expanded = expanded,
                selected = current == NavDestination.MOVIES,
                onClick = onMovies,
            )
        }
        if (showSeries) {
            NavRailItem(
                text = stringResource(R.string.home_series),
                icon = Icons.Default.Tv,
                expanded = expanded,
                selected = current == NavDestination.SERIES,
                onClick = onSeries,
            )
        }
        Spacer(Modifier.height(10.dp))
        NavRailItem(
            text = stringResource(R.string.home_settings),
            icon = Icons.Default.Settings,
            expanded = expanded,
            selected = current == NavDestination.SETTINGS,
            onClick = onSettings,
        )
    }
}

@Composable
private fun NavRailItem(
    text: String,
    icon: ImageVector,
    expanded: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FocusableSurface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(28.dp),
        brush = if (selected) Tint.Live else Tint.Neutral,
        contentPadding = 14.dp,
    ) { _ ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = if (expanded) null else text, modifier = Modifier.size(28.dp))
            if (expanded) {
                Spacer(Modifier.width(14.dp))
                Text(text = text, style = MaterialTheme.typography.labelLarge, maxLines = 1)
            }
        }
    }
}

val RAIL_COLLAPSED = 64.dp
val RAIL_EXPANDED = 236.dp
