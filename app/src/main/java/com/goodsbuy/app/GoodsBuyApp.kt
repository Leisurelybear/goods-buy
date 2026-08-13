package com.goodsbuy.app

import android.app.Application
import com.goodsbuy.app.ui.preferences.PreferencesRepository
import com.goodsbuy.app.util.AppLogger
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class GoodsBuyApp : Application() {

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    override fun onCreate() {
        super.onCreate()
        AppLogger.init(this, preferencesRepository.loggingEnabled)
        AppLogger.i("App", "Application started, logging=${preferencesRepository.loggingEnabled}")
    }
}
