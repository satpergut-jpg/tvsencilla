package com.tvsencilla.iptv.data.net

import java.net.InetAddress
import java.net.UnknownHostException
import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps

/**
 * DNS cifrado de Cloudflare (1.1.1.1 sobre HTTPS), al estilo de WARP pero solo para el tráfico de
 * esta app: se activa solo al arrancar, sin permisos ni pantallas.
 *
 * El operador deja de ver —y de poder falsear— los dominios que consultamos. Si la consulta cifrada
 * falla (red sin salida al 443, portal cautivo, DoH bloqueado), se cae al DNS del sistema para no
 * dejar la app sin conexión.
 */
class CloudflareDns(bootstrapClient: OkHttpClient) : Dns {

    private val doh: Dns = DnsOverHttps.Builder()
        .client(bootstrapClient)
        .url("https://cloudflare-dns.com/dns-query".toHttpUrl())
        // IPs fijas del resolver: así no hace falta el DNS del operador ni para llegar a Cloudflare.
        .bootstrapDnsHosts(
            InetAddress.getByName("1.1.1.1"),
            InetAddress.getByName("1.0.0.1"),
            InetAddress.getByName("2606:4700:4700::1111"),
            InetAddress.getByName("2606:4700:4700::1001"),
        )
        .includeIPv6(true)
        .post(true)
        .build()

    override fun lookup(hostname: String): List<InetAddress> = try {
        doh.lookup(hostname).ifEmpty { Dns.SYSTEM.lookup(hostname) }
    } catch (e: UnknownHostException) {
        Dns.SYSTEM.lookup(hostname)
    } catch (e: java.io.IOException) {
        Dns.SYSTEM.lookup(hostname)
    }
}
