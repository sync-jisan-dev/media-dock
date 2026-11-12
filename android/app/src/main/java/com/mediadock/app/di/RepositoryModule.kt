package com.mediadock.app.di

import com.mediadock.app.data.repository.DownloadRepositoryImpl
import com.mediadock.app.data.repository.MediaRepositoryImpl
import com.mediadock.app.data.repository.SettingsRepositoryImpl
import com.mediadock.app.domain.repository.DownloadRepository
import com.mediadock.app.domain.repository.MediaRepository
import com.mediadock.app.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMediaRepository(impl: MediaRepositoryImpl): MediaRepository

    @Binds
    @Singleton
    abstract fun bindDownloadRepository(impl: DownloadRepositoryImpl): DownloadRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}
