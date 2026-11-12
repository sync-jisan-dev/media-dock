package com.mediadock.app.di

import android.content.Context
import androidx.room.Room
import com.mediadock.app.core.database.MediaDockDatabase
import com.mediadock.app.core.database.dao.DownloadDao
import com.mediadock.app.core.database.dao.HistoryDao
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
    fun provideMediaDockDatabase(@ApplicationContext context: Context): MediaDockDatabase {
        return Room.databaseBuilder(
            context,
            MediaDockDatabase::class.java,
            "mediadock_db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideDownloadDao(database: MediaDockDatabase): DownloadDao = database.downloadDao()

    @Provides
    fun provideHistoryDao(database: MediaDockDatabase): HistoryDao = database.historyDao()
}
