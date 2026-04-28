package com.sojakothy.music.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.sojakothy.music.data.models.DownloadTask
import com.sojakothy.music.data.models.Song

@Database(
    entities = [Song::class, DownloadTask::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun downloadDao(): DownloadDao

    companion object {
        const val DATABASE_NAME = "sojakothy_music.db"

        fun create(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
