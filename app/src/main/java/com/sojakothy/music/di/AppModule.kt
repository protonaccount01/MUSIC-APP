package com.sojakothy.music.di

import android.content.Context
import androidx.work.WorkManager
import com.sojakothy.music.data.database.AppDatabase
import com.sojakothy.music.data.repository.MusicRepository
import com.sojakothy.music.data.repository.YouTubeRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.create(context)
    }

    @Provides
    @Singleton
    fun provideYouTubeRepository(): YouTubeRepository {
        return YouTubeRepository()
    }

    @Provides
    @Singleton
    fun provideMusicRepository(
        database: AppDatabase,
        youTubeRepository: YouTubeRepository
    ): MusicRepository {
        return MusicRepository(database, youTubeRepository)
    }

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager {
        return WorkManager.getInstance(context)
    }
}
