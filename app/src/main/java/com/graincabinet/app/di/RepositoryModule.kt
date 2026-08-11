package com.graincabinet.app.di

import com.graincabinet.app.data.repository.CollectibleRepositoryImpl
import com.graincabinet.app.domain.repository.CollectibleRepository
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
    abstract fun bindCollectibleRepository(
        impl: CollectibleRepositoryImpl
    ): CollectibleRepository
}
