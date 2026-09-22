package com.tvsencilla.iptv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.tvsencilla.iptv.domain.model.displayName
import com.tvsencilla.iptv.ui.theme.FocusYellow
import com.tvsencilla.iptv.ui.theme.LiveRed
import com.tvsencilla.iptv.ui.theme.PremiumLook
import com.tvsencilla.iptv.ui.theme.Tint
import androidx.compose.ui.graphics.Brush


/**
 * Ancho de carátula para las filas horizontales, elegido para que quepan cinco. En las rejillas
 * NO se aplica: allí el ancho lo marca la columna, y forzarlo desbordaba la pantalla.
 */
val PosterWidth: Dp = 158.dp

@Composable
fun PosterCard(
    title: String,
    posterUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    progress: Float? = null,
) {
    Column(modifier = modifier) {
        FocusableSurface(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f),
            shape = RoundedCornerShape(if (PremiumLook) 14.dp else 10.dp),
            contentPadding = 0.dp,
            contentAlignment = Alignment.BottomStart,
            focusedScale = if (PremiumLook) CARD_FOCUSED_SCALE else 1.02f,
        ) { _ ->
            Box(Modifier.fillMaxSize()) {
                RemoteImage(
                    url = posterUrl,
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                )
                if (progress != null && progress > 0f) {
                    ProgressStripe(
                        progress = progress,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp),
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Aumento de las tarjetas de las filas horizontales del acabado premium. */
private const val CARD_FOCUSED_SCALE = 1.06f

/**
 * Tarjeta 16:9 de un canal en directo: logo sobre fondo tintado, insignia DIRECTO y, si la guía lo
 * sabe, el programa en emisión con su barra de progreso real.
 */
@Composable
fun LiveCard(
    channelName: String,
    logoUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    programTitle: String? = null,
    progress: Float? = null,
) {
    Column(modifier = modifier) {
        FocusableSurface(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
            shape = RoundedCornerShape(14.dp),
            contentPadding = 0.dp,
            contentAlignment = Alignment.BottomStart,
            focusedScale = CARD_FOCUSED_SCALE,
        ) { _ ->
            Box(Modifier.fillMaxSize().background(Tint.Card)) {
                RemoteImage(
                    url = logoUrl,
                    contentDescription = channelName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 26.dp, vertical = 22.dp),
                )
                Text(
                    text = "DIRECTO",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(LiveRed)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
                if (progress != null && progress > 0f) {
                    ProgressStripe(progress = progress, modifier = Modifier.align(Alignment.BottomCenter))
                }
            }
        }
        Text(
            text = displayName(channelName),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp),
        )
        if (programTitle != null) {
            Text(
                text = programTitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * One row of the channel list: the number the remote responds to, a large logo and the name, plus
 * what is on now when the guide knows.
 */
@Composable
fun ChannelListItem(
    number: Int,
    name: String,
    logoUrl: String?,
    isFavorite: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    nowTitle: String? = null,
) {
    FocusableSurface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        contentPadding = 10.dp,
    ) { _ ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = number.toString(),
                style = MaterialTheme.typography.titleLarge,
                color = FocusYellow,
                textAlign = TextAlign.End,
                // Una sola línea y ancho mínimo para cuatro cifras: con 1644 canales, el número
                // se partía en dos renglones (164 / 4).
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.widthIn(min = 76.dp),
            )
            Spacer(Modifier.width(14.dp))
            RemoteImage(
                url = logoUrl,
                contentDescription = name,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(width = 72.dp, height = 46.dp)
                    .clip(RoundedCornerShape(6.dp)),
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = displayName(name),
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (nowTitle != null) {
                    Text(
                        text = nowTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (isFavorite) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = FocusYellow,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

/**
 * Ficha cuadrada de un canal: logo centrado sobre fondo tintado y el nombre debajo. Es la pieza de
 * la rejilla agrupada por categorías ("Televisión", "Fútbol"…) del acabado premium.
 */
@Composable
fun SquareLogoTile(
    name: String,
    logoUrl: String?,
    isFavorite: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        FocusableSurface(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
            shape = RoundedCornerShape(16.dp),
            contentPadding = 0.dp,
            focusedScale = CARD_FOCUSED_SCALE,
        ) { _ ->
            Box(Modifier.fillMaxSize().background(Tint.Card)) {
                RemoteImage(
                    url = logoUrl,
                    contentDescription = name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(18.dp),
                )
                if (isFavorite) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = FocusYellow,
                        modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(18.dp),
                    )
                }
            }
        }
        Text(
            text = displayName(name),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
    }
}

/** The digits being dialled, shown large in a corner as "1_" or "12_". */
@Composable
fun DialledNumberOverlay(digits: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.Black.copy(alpha = 0.82f))
            .padding(horizontal = 26.dp, vertical = 14.dp),
    ) {
        Text(
            text = "${digits}_",
            style = MaterialTheme.typography.displayMedium,
            color = FocusYellow,
        )
    }
}

/**
 * A plain sentence along the bottom. Callers keep it up for at least eight seconds, long enough
 * to be read without hurrying.
 */
@Composable
fun MessageBanner(message: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 22.dp, vertical = 14.dp),
    ) {
        Text(text = message, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun ProgressStripe(progress: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .background(Color.Black.copy(alpha = 0.6f)),
    ) {
        Box(
            Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(FocusYellow),
        )
    }
}

/** Images are decoded at the size they are drawn; full-size artwork exhausts a 1 GB box fast. */
@Composable
fun RemoteImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    if (url.isNullOrBlank()) {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Tv,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp),
            )
        }
        return
    }

    AsyncImage(
        model = ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
            .data(url)
            .crossfade(false)
            .build(),
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier,
    )
}
