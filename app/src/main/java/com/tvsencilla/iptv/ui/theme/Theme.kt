package com.tvsencilla.iptv.ui.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import com.tvsencilla.iptv.domain.model.FontSizeOption
import com.tvsencilla.iptv.domain.model.SubtitleSizeOption

private val colors = darkColorScheme(
    primary = SkyBlue,
    onPrimary = DeepBlue,
    secondary = FocusYellow,
    onSecondary = Ink,
    background = Ink,
    onBackground = Snow,
    surface = InkSurface,
    onSurface = Snow,
    surfaceVariant = InkSurfaceVariant,
    onSurfaceVariant = SnowDim,
    error = LiveRed,
    onError = Ink,
    border = FocusRing,
)

/** Subtitle scaling is separate from the UI text scale, because viewers tune them differently. */
val LocalSubtitleScale = staticCompositionLocalOf { SubtitleSizeOption.LARGE.scale }

@Composable
fun TvSencillaTheme(
    fontSize: FontSizeOption = FontSizeOption.NORMAL,
    subtitleSize: SubtitleSizeOption = SubtitleSizeOption.LARGE,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = colors,
        typography = tvTypography(fontSize.scale),
    ) {
        // Sin esto, todo texto o icono sin color explícito sale negro sobre el fondo oscuro:
        // el color por defecto de TV Material es negro.
        CompositionLocalProvider(
            LocalSubtitleScale provides subtitleSize.scale,
            LocalContentColor provides Snow,
            content = content,
        )
    }
}

/**
 * Television overscan: roughly 5% of the panel can be cut off by the set itself, so nothing is
 * drawn in that margin.
 */
val OverscanPadding = PaddingValues(horizontal = 32.dp, vertical = 18.dp)

fun Modifier.overscan(): Modifier = padding(OverscanPadding)
