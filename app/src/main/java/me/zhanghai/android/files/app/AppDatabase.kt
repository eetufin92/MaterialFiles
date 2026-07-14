/*
 * Copyright (c) 2026 eetufin92 <eetufin92@gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.files.app

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import me.zhanghai.android.files.file.cache.FileCacheDao
import me.zhanghai.android.files.file.cache.FileCacheEntity
import me.zhanghai.android.files.file.cache.FileSearchEntity

@Database(
    entities = [FileCacheEntity::class, FileSearchEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun fileCacheDao(): FileCacheDao

    companion object {
        private const val DB_NAME = "materialfiles.db"

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(application, AppDatabase::class.java, DB_NAME)
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { instance = it }
            }
    }
}
