package com.tvsencilla.iptv.data.xtream

/**
 * Servidores espejo de la misma suscripción: cuando el usuario entra solo con usuario y
 * contraseña, se prueban en este orden hasta que uno responda.
 */
val XTREAM_FAILOVER_HOSTS = listOf(
    "http://1.vpn-esxtrapro.pro:80",
    "http://cdn11234.cdn-vad.vip:80",
    "http://smart-d24k.live:80",
    "http://vpn-ultrapro.info:80",
    "http://ca.vpn-inf.xyz:80",
)
