package com.tvsencilla.iptv.ui.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.tvsencilla.iptv.R
import com.tvsencilla.iptv.domain.model.ContinueWatchingItem
import com.tvsencilla.iptv.domain.model.displayName
import com.tvsencilla.iptv.ui.components.LiveCard
import com.tvsencilla.iptv.ui.components.NavRail
import com.tvsencilla.iptv.ui.components.PosterCard
import com.tvsencilla.iptv.ui.components.ProgressStripe
import com.tvsencilla.iptv.ui.components.RAIL_COLLAPSED
import com.tvsencilla.iptv.ui.components.RemoteImage
import com.tvsencilla.iptv.ui.components.SectionTitle
import com.tvsencilla.iptv.ui.theme.Accent
import com.tvsencilla.iptv.ui.theme.LiveRed
import com.tvsencilla.iptv.ui.theme.ScreenBackground
import com.tvsencilla.iptv.ui.theme.overscan
import com.tvsencilla.iptv.ui.util.formatHourMinute
import kotlinx.coroutines.delay

/** Lo que enseña la parte alta de la portada: sigue al elemento que tiene el foco. */
private data class Hero(
    val label: String,
    val isLive: Boolean,
    val title: String,
    val detail: String?,
    val imageUrl: String?,
    val progress: Float?,
)

/**
 * Portada del acabado premium: un rail de destinos a la izquierda, el elemento enfocado a pantalla
 * completa detrás, y filas horizontales debajo. Es solo una vista; no cambia adónde lleva nada.
 */
@Composable
fun PremiumHome(
    state: HomeUiState,
    firstDestination: FocusRequester,
    onLiveTv: () -> Unit,
    onMovies: () -> Unit,
    onSeries: () -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
    onContinueWatching: (ContinueWatchingItem) -> Unit,
    onJumpToChannel: (String) -> Unit,
) {
    val liveLabel = stringResource(R.string.home_hero_live)
    val continueLabel = stringResource(R.string.home_hero_continue)
    val untilFormat = stringResource(R.string.live_until, "%s")

    fun heroOf(item: ContinueWatchingItem) = Hero(
        label = continueLabel,
        isLive = false,
        title = item.title,
        detail = item.subtitle,
        imageUrl = item.posterUrl,
        progress = item.fraction,
    )

    fun heroOf(item: LiveNowItem, now: Long) = Hero(
        label = liveLabel,
        isLive = true,
        title = item.now?.title ?: displayName(item.channel.name),
        detail = item.now?.let {
            displayName(item.channel.name) + " · " + untilFormat.replace("%s", formatHourMinute(it.endMillis))
        },
        imageUrl = item.channel.logoUrl,
        progress = item.now?.progressAt(now),
    )

    val liveNowFallbackLabel = stringResource(R.string.home_live_now)
    val now = rememberClock()
    var focusedHero by remember { mutableStateOf<Hero?>(null) }
    val defaultHero = state.continueWatching.firstOrNull()?.let(::heroOf)
        ?: state.liveNow.firstOrNull()?.items?.firstOrNull()?.let { heroOf(it, now) }
    val hero = focusedHero ?: defaultHero

    Box(Modifier.fillMaxSize().background(ScreenBackground)) {
        // Imagen del elemento enfocado a pantalla completa y muy oscurecida: solo da ambiente.
        Crossfade(
            targetState = hero?.imageUrl,
            animationSpec = tween(400),
            label = "heroBackdrop",
            modifier = Modifier.fillMaxSize(),
        ) { url ->
            if (url != null) {
                RemoteImage(url = url, contentDescription = null, modifier = Modifier.fillMaxSize())
            }
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(listOf(Color(0xFF05070B), Color(0xE605070B), Color(0x9905070B))),
            ),
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(0.25f to Color(0x6605070B), 0.6f to Color(0xEE05070B), 1f to Color(0xFF05070B)),
            ),
        )

        Column(Modifier.fillMaxSize().overscan().padding(start = RAIL_COLLAPSED + 20.dp)) {
            HeroText(
                hero = hero,
                clock = formatHourMinute(now),
                modifier = Modifier.fillMaxWidth().height(150.dp),
            )
            Spacer(Modifier.height(6.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                if (state.continueWatching.isNotEmpty()) {
                    item(key = "continue") {
                        Shelf(stringResource(R.string.home_continue_watching)) {
                            items(state.continueWatching, key = { it.itemId }) { item ->
                                PosterCard(
                                    title = item.title,
                                    subtitle = item.subtitle,
                                    posterUrl = item.posterUrl,
                                    progress = item.fraction,
                                    onClick = { onContinueWatching(item) },
                                    modifier = Modifier
                                        .width(112.dp)
                                        .onFocusChanged { if (it.hasFocus) focusedHero = heroOf(item) },
                                )
                            }
                        }
                    }
                }
                items(state.liveNow, key = { "live_${it.categoryName}" }) { group ->
                    Shelf(group.categoryName ?: liveNowFallbackLabel) {
                        items(group.items, key = { it.channel.id }) { item ->
                            LiveCard(
                                channelName = item.channel.name,
                                logoUrl = item.channel.logoUrl,
                                programTitle = item.now?.title,
                                progress = item.now?.progressAt(now),
                                onClick = { onJumpToChannel(item.channel.id) },
                                modifier = Modifier
                                    .width(214.dp)
                                    .onFocusChanged { if (it.hasFocus) focusedHero = heroOf(item, now) },
                            )
                        }
                    }
                }
            }
        }

        NavRail(
            current = null,
            showMovies = state.showMovies,
            showSeries = state.showSeries,
            firstDestination = firstDestination,
            onSearch = onSearch,
            onLiveTv = onLiveTv,
            onMovies = onMovies,
            onSeries = onSeries,
            onSettings = onSettings,
            modifier = Modifier.fillMaxHeight().overscan(),
        )
    }
}

@Composable
private fun HeroText(hero: Hero?, clock: String, modifier: Modifier = Modifier) {
    Box(modifier) {
        Text(
            text = clock,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.TopEnd),
        )
        Column(Modifier.align(Alignment.BottomStart).widthIn(max = 560.dp)) {
            if (hero == null) {
                Text(text = stringResource(R.string.app_name), style = MaterialTheme.typography.displayLarge)
                return@Column
            }
            Text(
                text = hero.label.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = if (hero.isLive) LiveRed else Accent,
            )
            Text(
                text = hero.title,
                style = MaterialTheme.typography.displaySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
            if (hero.detail != null) {
                Text(
                    text = hero.detail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            if (hero.progress != null && hero.progress > 0f) {
                ProgressStripe(
                    progress = hero.progress,
                    modifier = Modifier.padding(top = 10.dp).width(280.dp).clip(RoundedCornerShape(3.dp)),
                )
            }
        }
    }
}

@Composable
private fun Shelf(title: String, row: LazyListScope.() -> Unit) {
    Column {
        SectionTitle(title)
        Spacer(Modifier.height(10.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            // Aire arriba y abajo: la tarjeta enfocada crece y su halo no debe recortarse.
            contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp, end = 32.dp),
            content = row,
        )
    }
}

/** Hora actual, refrescada cada 30 s; también sirve para mover las barras de progreso. */
@Composable
private fun rememberClock(): Long {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            now = System.currentTimeMillis()
        }
    }
    return now
}
