package com.tvsencilla.iptv.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.tvsencilla.iptv.BuildConfig

/**
 * La edición completa lleva un acabado más cuidado, al estilo de Apple TV: negro profundo, grises
 * cálidos, colores de sistema por sección y el foco en blanco. La edición Directo conserva el
 * aspecto clásico de alto contraste con foco amarillo.
 */
val PremiumLook: Boolean = !BuildConfig.SOLO_DIRECTO

/** Deliberately near-black with strong accents: contrast matters more than subtlety here. */
val Ink = if (PremiumLook) Color(0xFF05070B) else Color(0xFF0B0F14)
val InkSurface = if (PremiumLook) Color(0xFF10141C) else Color(0xFF161C24)
val InkSurfaceVariant = if (PremiumLook) Color(0xFF232B3A) else Color(0xFF222B36)

/** Tercer nivel de superficie (base, elevada, flotante): paneles y tarjetas que "flotan". */
val InkFloating = if (PremiumLook) Color(0xFF2C3648) else Color(0xFF2A3441)

/** El único acento de la marca en el acabado premium: un turquesa que se reconoce de lejos. */
val Accent = Color(0xFF19C6E6)
val AccentDeep = Color(0xFF3B82F6)
val Snow = Color(0xFFFFFFFF)
val SnowDim = if (PremiumLook) Color(0xFFC7C7CC) else Color(0xFFCBD5E1)
val SkyBlue = if (PremiumLook) Accent else Color(0xFF4FA3FF)
val DeepBlue = if (PremiumLook) Color(0xFF03141A) else Color(0xFF00121F)

/** Colour for highlights (live now, selected episode…). Yellow on near-black reads at 3 metres. */
val FocusYellow = if (PremiumLook) Accent else Color(0xFFFFD400)

/** The focus ring: white in the premium look, as on Apple TV; yellow in the classic one. */
val FocusRing = if (PremiumLook) Color(0xFFFFFFFF) else FocusYellow

/** Halo de la pieza enfocada: color de marca en el premium, sombra negra en el clásico. */
val FocusHalo = if (PremiumLook) Accent else Color.Black
val LiveRed = if (PremiumLook) Color(0xFFFF453A) else Color(0xFFFF5A5A)
val SoftGreen = if (PremiumLook) Color(0xFF30D158) else Color(0xFF5BD98A)

/** Fondo de pantalla: un degradado muy suave de grafito a negro, en lugar de un color plano. */
val ScreenBackground: Brush = if (PremiumLook) {
    Brush.verticalGradient(listOf(Color(0xFF111827), Color(0xFF080B12), Color(0xFF05070B)))
} else {
    Brush.verticalGradient(listOf(Ink, Ink))
}

/** Colores de sistema de Apple, uno por destino de Inicio, siempre el mismo para cada uno. */
object Tint {
    private val Brand = Brush.linearGradient(listOf(Accent, AccentDeep))

    // En el premium todo comparte el degradado de marca: un solo color en lugar de cinco.
    val Live = if (PremiumLook) Brand else Brush.linearGradient(listOf(Color(0xFFFF375F), Color(0xFFFF9F0A)))
    val Movies = if (PremiumLook) Brand else Brush.linearGradient(listOf(Color(0xFF0A84FF), Color(0xFF5E5CE6)))
    val Series = if (PremiumLook) Brand else Brush.linearGradient(listOf(Color(0xFFBF5AF2), Color(0xFF5E5CE6)))
    val Search = if (PremiumLook) Brand else Brush.linearGradient(listOf(Color(0xFF30D158), Color(0xFF64D2FF)))
    val Neutral = Brush.linearGradient(listOf(Color(0xFF48484A), Color(0xFF2C2C2E)))

    /** Fondo de una tarjeta de canal cuando aún no hay imagen del programa. */
    val Card = Brush.linearGradient(listOf(Color(0xFF1B2436), Color(0xFF0E1420)))
}
