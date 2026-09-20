package com.tvsencilla.iptv.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.tvsencilla.iptv.BuildConfig
import com.tvsencilla.iptv.data.net.CloudflareDns
import com.tvsencilla.iptv.data.xtream.XtreamApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

private val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.appDataStore

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        // Cliente mínimo solo para hablar con el resolver de Cloudflare; no puede usar DoH él mismo.
        val bootstrap = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
        return bootstrap.newBuilder()
            .dns(CloudflareDns(bootstrap))
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .callTimeout(0, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .apply { if (BuildConfig.DEBUG) addInterceptor(redactingLogger()) }
            .build()
    }

    /**
     * Xtream puts the subscription password straight into the query string, so the URL can never
     * reach logcat as-is.
     */
    private fun redactingLogger(): HttpLoggingInterceptor {
        val logger = HttpLoggingInterceptor.Logger { message ->
            android.util.Log.d(
                "TvSencillaHttp",
                message
                    .replace(Regex("(?<=password=)[^&\\s]+"), "***")
                    .replace(Regex("(?<=username=)[^&\\s]+"), "***"),
            )
        }
        return HttpLoggingInterceptor(logger).apply { level = HttpLoggingInterceptor.Level.BASIC }
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        // Every call supplies an absolute @Url, since the host is the user's own provider.
        .baseUrl("http://localhost/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides
    @Singleton
    fun provideXtreamApi(retrofit: Retrofit): XtreamApi = retrofit.create(XtreamApi::class.java)
}
