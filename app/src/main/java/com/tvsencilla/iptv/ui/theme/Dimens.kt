package com.tvsencilla.iptv.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/** Escala de espaciado: todo el acabado premium se apoya en estos pasos, no en números sueltos. */
object Space {
    val Xs = 4.dp
    val S = 8.dp
    val M = 12.dp
    val L = 16.dp
    val Xl = 24.dp
    val Xxl = 32.dp
}

/** Tres radios: chips y carátulas, tarjetas, y piezas grandes como el rail o los paneles. */
object Radius {
    val Small = RoundedCornerShape(8.dp)
    val Medium = RoundedCornerShape(16.dp)
    val Large = RoundedCornerShape(28.dp)
}
