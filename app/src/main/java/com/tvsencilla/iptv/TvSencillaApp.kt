package com.tvsencilla.iptv

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.tvsencilla.iptv.data.work.EpgUpdateWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlin.concurrent.thread
import okhttp3.OkHttpClient

@HiltAndroidApp
class TvSencillaApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    /** Lleva ya dentro el DNS cifrado de Cloudflare; lo compartimos con API, descargas y reproductor. */
    @Inject
    lateinit var httpClient: OkHttpClient

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        EpgUpdateWorker.schedule(this)
        calentarDnsCifrado()
    }

    /**
     * Abre la conexión con 1.1.1.1 nada más arrancar, en segundo plano, para que la primera
     * consulta real (portada, EPG, primer canal) no pague el coste del handshake TLS.
     */
    private fun calentarDnsCifrado() {
        thread(isDaemon = true, name = "doh-warmup") {
            runCatching { httpClient.dns.lookup("cloudflare-dns.com") }
        }
    }
}
