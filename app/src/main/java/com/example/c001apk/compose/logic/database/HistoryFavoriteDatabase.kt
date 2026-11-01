package com.example.c001apk.compose.logic.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.SkipQueryVerification
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.c001apk.compose.logic.dao.HistoryFavoriteDao
import com.example.c001apk.compose.logic.model.FavoriteFolder
import com.example.c001apk.compose.logic.model.FeedEntity


@SkipQueryVerification
@Database(version = 3, entities = [FeedEntity::class, FavoriteFolder::class])
abstract class HistoryFavoriteDatabase : RoomDatabase() {
    abstract fun historyFavoriteDao(): HistoryFavoriteDao

    companion object {
        /** 1 -> 2：新增收藏夹字段与收藏夹表（历史/收藏两个库共用）。 */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE FeedEntity ADD COLUMN title TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE FeedEntity ADD COLUMN folder TEXT NOT NULL DEFAULT ''")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS FavoriteFolder (" +
                        "name TEXT NOT NULL, " +
                        "createdAt INTEGER NOT NULL, " +
                        "PRIMARY KEY(name))"
                )
            }
        }

        /**
         * 2 -> 3：v7 升级到 v8 时实体默认值变化但版本号没变，触发
         * 「Room cannot verify the data integrity」。这里整表重建，
         * 保证列定义与当前实体完全一致，并保留已有收藏/历史数据。
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `FeedEntity_new` (" +
                        "`id` TEXT NOT NULL, " +
                        "`uid` TEXT NOT NULL, " +
                        "`uname` TEXT NOT NULL, " +
                        "`avatar` TEXT NOT NULL, " +
                        "`device` TEXT NOT NULL, " +
                        "`message` TEXT NOT NULL, " +
                        "`pubDate` TEXT NOT NULL, " +
                        "`time` TEXT NOT NULL, " +
                        "`folder` TEXT NOT NULL DEFAULT '', " +
                        "`title` TEXT NOT NULL DEFAULT '', " +
                        "PRIMARY KEY(`id`))"
                )
                db.execSQL(
                    "INSERT OR REPLACE INTO `FeedEntity_new` " +
                        "(`id`,`uid`,`uname`,`avatar`,`device`,`message`,`pubDate`,`time`,`folder`,`title`) " +
                        "SELECT `id`,`uid`,`uname`,`avatar`,`device`,`message`,`pubDate`,`time`,`folder`,`title` " +
                        "FROM `FeedEntity`"
                )
                db.execSQL("DROP TABLE `FeedEntity`")
                db.execSQL("ALTER TABLE `FeedEntity_new` RENAME TO `FeedEntity`")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `FavoriteFolder` (" +
                        "`name` TEXT NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`name`))"
                )
            }
        }
    }
}
