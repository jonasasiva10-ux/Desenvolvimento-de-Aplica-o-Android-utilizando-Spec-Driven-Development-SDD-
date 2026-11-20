package com.example

import android.app.Application
import com.example.di.patchPulseModules
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class PatchPulseApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (GlobalContext.getOrNull() == null) {
            startKoin {
                androidLogger(if (BuildConfig.DEBUG) Level.INFO else Level.ERROR)
                androidContext(this@PatchPulseApplication)
                modules(patchPulseModules)
            }
        }
    }
}
