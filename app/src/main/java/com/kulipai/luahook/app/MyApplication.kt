package com.kulipai.luahook.app

import android.app.Application
import com.kulipai.luahook.core.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

/**
 * @author kulipai
 * @date 2026/3/20
 */

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@MyApplication)
            modules(appModule)
        }
    }
}