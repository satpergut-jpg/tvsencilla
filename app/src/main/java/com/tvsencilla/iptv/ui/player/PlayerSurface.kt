package com.tvsencilla.iptv.ui.player

import android.graphics.Color as AndroidColor
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.tvsencilla.iptv.R
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import androidx.media3.ui.SubtitleView
import com.tvsencilla.iptv.ui.theme.LocalSubtitleScale

/**
 * The video surface itself. Media3's own controls are switched off because the app provides its
 * own, built to the same large-target rules as every other screen.
 */
@Composable
fun PlayerSurface(
    player: ExoPlayer?,
    modifier: Modifier = Modifier,
) {
    val subtitleScale = LocalSubtitleScale.current
    val hostView = LocalView.current

    // The remote is idle for long stretches while watching, so the screensaver has to be held off.
    DisposableEffect(hostView) {
        hostView.keepScreenOn = true
        onDispose { hostView.keepScreenOn = false }
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            PlayerView(context).apply {
                useController = false
                setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
                setBackgroundColor(AndroidColor.BLACK)
                // Focus stays with Compose, which owns all key handling.
                descendantFocusability = android.view.ViewGroup.FOCUS_BLOCK_DESCENDANTS
                isFocusable = false
                subtitleView?.applyLargeSubtitleStyle(subtitleScale)
            }
        },
        update = { view ->
            view.player = player
            view.subtitleView?.applyLargeSubtitleStyle(subtitleScale)
        },
        onRelease = { view -> view.player = null },
    )
}

/**
 * "Cargando…" escrito bajo la rueda del reproductor. Una rueda sola no dice nada a quien no sabe
 * qué significa, y cargar una película puede llevar bastantes segundos.
 */
@Composable
fun BufferingLabel(modifier: Modifier = Modifier) {
    Box(
        // Por encima de la rueda: debajo quedaba tapado por la barra del canal.
        modifier = modifier
            .padding(bottom = 150.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.75f))
            .padding(horizontal = 20.dp, vertical = 10.dp),
    ) {
        Text(
            text = stringResource(R.string.common_loading),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

/** Large text on a solid backing plate: the most legible combination over moving video. */
private fun SubtitleView.applyLargeSubtitleStyle(scale: Float) {
    setApplyEmbeddedStyles(false)
    setApplyEmbeddedFontSizes(false)
    setFractionalTextSize(SubtitleView.DEFAULT_TEXT_SIZE_FRACTION * scale)
    setStyle(
        CaptionStyleCompat(
            AndroidColor.WHITE,
            AndroidColor.argb(170, 0, 0, 0),
            AndroidColor.TRANSPARENT,
            CaptionStyleCompat.EDGE_TYPE_OUTLINE,
            AndroidColor.BLACK,
            null,
        ),
    )
    visibility = View.VISIBLE
}
