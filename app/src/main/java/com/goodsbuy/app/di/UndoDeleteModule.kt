package com.goodsbuy.app.di

import com.goodsbuy.app.util.UndoDeleteController
import com.goodsbuy.app.util.UndoDeleteManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class UndoDeleteModule {
    @Binds
    @Singleton
    abstract fun bindUndoDeleteController(manager: UndoDeleteManager): UndoDeleteController
}
