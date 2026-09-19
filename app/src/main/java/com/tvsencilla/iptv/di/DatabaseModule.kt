package com.tvsencilla.iptv.di

import android.content.Context
import androidx.room.Room
import com.tvsencilla.iptv.data.local.AppDatabase
import com.tvsencilla.iptv.data.local.ChannelDao
import com.tvsencilla.iptv.data.local.EpgDao
import com.tvsencilla.iptv.data.local.FavoriteDao
import com.tvsencilla.iptv.data.local.LikeDao
import com.tvsencilla.iptv.data.local.ProgressDao
import com.tvsencilla.iptv.data.local.VodDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME)
            .addMigrations(AppDatabase.MIGRATION_1_2)
            // Solo como último recurso, si faltara una migración: casi todo es caché que se vuelve a
            // descargar, y el orden de favoritos también se guarda en el almacén de perfil.
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideChannelDao(database: AppDatabase): ChannelDao = database.channelDao()

    @Provides
    fun provideFavoriteDao(database: AppDatabase): FavoriteDao = database.favoriteDao()

    @Provides
    fun provideEpgDao(database: AppDatabase): EpgDao = database.epgDao()

    @Provides
    fun provideVodDao(database: AppDatabase): VodDao = database.vodDao()

    @Provides
    fun provideProgressDao(database: AppDatabase): ProgressDao = database.progressDao()

    @Provides
    fun provideLikeDao(database: AppDatabase): LikeDao = database.likeDao()
}
