package com.tvsencilla.iptv.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Typography

/**
 * Tamaños pensados para verse a tres metros, pero acotados para que quepan en una pantalla de
 * 960x540 dp, que es la medida habitual de un televisor. El suelo de 20sp sigue siendo grande
 * para los criterios de Android TV, y el ajuste "Tamaño de letra" lo sube a 24 o 29sp.
 */
private const val MIN_CONTENT_SP = 20f

private fun style(
    sizeSp: Float,
    weight: FontWeight,
    scale: Float,
    isContent: Boolean = true,
): TextStyle {
    val floor = if (isContent) MIN_CONTENT_SP else 0f
    val size = (sizeSp.coerceAtLeast(floor)) * scale
    return TextStyle(
        fontSize = size.sp,
        lineHeight = (size * 1.3f).sp,
        fontWeight = weight,
        // Premium: los títulos grandes se aprietan un poco, como en una tipografía de marca.
        letterSpacing = if (PremiumLook && sizeSp >= 25f) (-0.4f).sp else 0.sp,
    )
}

fun tvTypography(scale: Float): Typography = Typography(
    displayLarge = style(44f, FontWeight.Bold, scale),
    displayMedium = style(38f, FontWeight.Bold, scale),
    displaySmall = style(32f, FontWeight.Bold, scale),
    headlineLarge = style(34f, FontWeight.Bold, scale),
    headlineMedium = style(30f, FontWeight.SemiBold, scale),
    headlineSmall = style(26f, FontWeight.SemiBold, scale),
    titleLarge = style(28f, FontWeight.SemiBold, scale),
    titleMedium = style(25f, FontWeight.SemiBold, scale),
    titleSmall = style(22f, FontWeight.Medium, scale),
    bodyLarge = style(24f, FontWeight.Normal, scale),
    bodyMedium = style(22f, FontWeight.Normal, scale),
    bodySmall = style(20f, FontWeight.Normal, scale),
    labelLarge = style(23f, FontWeight.SemiBold, scale),
    labelMedium = style(21f, FontWeight.Medium, scale),
    // El único estilo por debajo del suelo: la insignia del número de canal, que no es texto.
    labelSmall = style(17f, FontWeight.Medium, scale, isContent = false),
)
