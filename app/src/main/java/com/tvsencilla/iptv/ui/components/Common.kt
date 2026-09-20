package com.tvsencilla.iptv.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SentimentNeutral
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.tvsencilla.iptv.R
import com.tvsencilla.iptv.ui.theme.FocusHalo
import com.tvsencilla.iptv.ui.theme.FocusRing
import com.tvsencilla.iptv.ui.theme.PremiumLook
import com.tvsencilla.iptv.ui.theme.ScreenBackground
import com.tvsencilla.iptv.ui.theme.overscan

/**
 * The single focusable building block for the whole app. Focus is shown three ways at once — a
 * thick yellow border, a lighter background and a slight enlargement — because one cue alone is
 * easy to miss on a large screen from across the room.
 */
@Composable
fun FocusableSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(if (PremiumLook) 18.dp else 14.dp),
    containerColor: Color = MaterialTheme.colorScheme.surface,
    focusedContainerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    /** Si se da, rellena la superficie con un degradado de color en lugar de un gris. */
    brush: Brush? = null,
    contentAlignment: Alignment = Alignment.CenterStart,
    contentPadding: Dp = 16.dp,
    /** Las tarjetas de una fila aguantan más aumento que un botón a todo lo ancho. */
    focusedScale: Float = FOCUSED_SCALE,
    content: @Composable (focused: Boolean) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) focusedScale else 1f,
        // Slow and gentle on purpose: nothing in this app should flicker or dart about.
        animationSpec = tween(durationMillis = 220),
        label = "focusScale",
    )

    Box(
        modifier = modifier
            .scale(scale)
            // Premium: la pieza enfocada "flota" con una sombra, como en Apple TV.
            .then(
                if (PremiumLook && focused) {
                    Modifier.shadow(elevation = 18.dp, shape = shape, ambientColor = Color.Black, spotColor = FocusHalo)
                } else {
                    Modifier
                },
            )
            .clip(shape)
            .then(
                if (brush != null) {
                    Modifier.background(brush).background(Color.White.copy(alpha = if (focused) 0.12f else 0f))
                } else {
                    Modifier.background(if (focused) focusedContainerColor else containerColor)
                },
            )
            .border(
                width = if (focused) 4.dp else 1.dp,
                color = if (focused) FocusRing else Color.White.copy(alpha = if (PremiumLook) 0.08f else 0.16f),
                shape = shape,
            )
            .onFocusChanged { focused = it.isFocused }
            .clickable(enabled = enabled, onClick = onClick)
            .padding(contentPadding),
        contentAlignment = contentAlignment,
    ) {
        content(focused)
    }
}

/** A large button that always pairs its icon with a written label. */
@Composable
fun BigButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    minHeight: Dp = 60.dp,
    brush: Brush? = null,
) {
    FocusableSurface(
        onClick = onClick,
        modifier = modifier.heightIn(min = minHeight),
        enabled = enabled,
        brush = brush,
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (icon != null) {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(26.dp))
                Spacer(Modifier.width(12.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun ScreenTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier,
    )
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

/** Every screen uses the same frame, so the title and the content never move between screens. */
@Composable
fun TvScreen(
    title: String?,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenBackground)
            .overscan(),
    ) {
        if (title != null) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ScreenTitle(title, modifier = Modifier.weight(1f))
                trailing?.invoke()
            }
        }
        content()
    }
}

/**
 * Waiting is announced in words rather than with a spinner: the brief rules out fast or restless
 * animation, and a sentence is what actually reassures someone that nothing is broken.
 */
@Composable
fun LoadingState(modifier: Modifier = Modifier, message: String = stringResource(R.string.common_loading)) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Default.HourglassEmpty,
            contentDescription = null,
            modifier = Modifier.size(52.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 20.dp),
        )
    }
}

/** Errors are always a plain sentence plus one obvious way out. */
@Composable
fun ErrorState(
    message: String,
    onRetry: (() -> Unit)?,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.WifiOff,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.error,
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 20.dp).widthIn(max = 620.dp),
        )
        if (onRetry != null) {
            BigButton(
                text = stringResource(R.string.common_retry),
                icon = Icons.Default.Refresh,
                onClick = onRetry,
                modifier = Modifier.padding(top = 24.dp).widthIn(min = 240.dp),
            )
        }
    }
}

@Composable
fun EmptyState(message: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Default.SentimentNeutral,
            contentDescription = null,
            modifier = Modifier.size(52.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 20.dp).widthIn(max = 620.dp),
        )
    }
}

/** Leve: en un botón a todo lo ancho, más aumento lo sacaría de los márgenes de la pantalla. */
private const val FOCUSED_SCALE = 1.02f
