package com.tvsencilla.iptv.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ChannelEntity::class,
        CategoryEntity::class,
        FavoriteEntity::class,
        EpgProgramEntity::class,
        MovieEntity::class,
        SeriesEntity::class,
        EpisodeEntity::class,
        WatchProgressEntity::class,
        LikeEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun channelDao(): ChannelDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun epgDao(): EpgDao
    abstract fun vodDao(): VodDao
    abstract fun progressDao(): ProgressDao
    abstract fun likeDao(): LikeDao

    companion object {
        const val NAME = "tvsencilla.db"

        /**
         * Versión 2 añade "Me gusta". Es una migración de verdad, no un borrado: quien actualice
         * la app conserva sus favoritos y lo que tenía a medio ver.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `likes` (" +
                        "`itemId` TEXT NOT NULL, " +
                        "`kind` TEXT NOT NULL, " +
                        "`likedAtMillis` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`itemId`))",
                )
            }
        }

        /**
         * Versión 3: los canales que solo cambian de calidad (HD, FHD, SD) se guardan como uno
         * solo, con las demás versiones en esta columna nueva.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `channels` ADD COLUMN `qualityOptionsRaw` TEXT")
            }
        }
    }
}
