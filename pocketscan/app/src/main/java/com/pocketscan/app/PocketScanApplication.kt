package com.pocketscan.app

import android.app.Application
import com.pocketscan.app.di.AppContainer

class PocketScanApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
