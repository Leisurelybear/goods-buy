package com.graincabinet.app.di

import android.content.Context
import androidx.room.Room
import com.graincabinet.app.data.db.AppDatabase
import com.graincabinet.app.data.db.CollectibleDao
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
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "grain_cabinet.db").build()

    @Provides
    @Singleton
    fun provideCollectibleDao(db: AppDatabase): CollectibleDao = db.collectibleDao()
}
