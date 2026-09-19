package com.tvsencilla.iptv.di

import com.tvsencilla.iptv.data.m3u.M3uContentProvider
import com.tvsencilla.iptv.data.repository.ChannelRepositoryImpl
import com.tvsencilla.iptv.data.repository.EpgRepositoryImpl
import com.tvsencilla.iptv.data.repository.SourceRepositoryImpl
import com.tvsencilla.iptv.data.repository.VodRepositoryImpl
import com.tvsencilla.iptv.data.settings.SettingsDataStore
import com.tvsencilla.iptv.data.sync.SimulatedFavoritesSync
import com.tvsencilla.iptv.data.xtream.XtreamContentProvider
import com.tvsencilla.iptv.domain.provider.ContentProvider
import com.tvsencilla.iptv.domain.repository.ChannelRepository
import com.tvsencilla.iptv.domain.repository.EpgRepository
import com.tvsencilla.iptv.domain.repository.FavoritesSyncRepository
import com.tvsencilla.iptv.domain.repository.SettingsRepository
import com.tvsencilla.iptv.domain.repository.SourceRepository
import com.tvsencilla.iptv.domain.repository.VodRepository
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsDataStore): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindSourceRepository(impl: SourceRepositoryImpl): SourceRepository

    @Binds
    @Singleton
    abstract fun bindChannelRepository(impl: ChannelRepositoryImpl): ChannelRepository

    @Binds
    @Singleton
    abstract fun bindEpgRepository(impl: EpgRepositoryImpl): EpgRepository

    @Binds
    @Singleton
    abstract fun bindVodRepository(impl: VodRepositoryImpl): VodRepository

    @Binds
    @Singleton
    abstract fun bindFavoritesSync(impl: SimulatedFavoritesSync): FavoritesSyncRepository

    /** Adding a new protocol means adding one more binding here and nothing else. */
    @Binds
    @IntoSet
    abstract fun bindM3uProvider(impl: M3uContentProvider): ContentProvider

    @Binds
    @IntoSet
    abstract fun bindXtreamProvider(impl: XtreamContentProvider): ContentProvider
}
