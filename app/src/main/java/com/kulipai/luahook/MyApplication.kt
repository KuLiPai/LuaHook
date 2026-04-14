package com.kulipai.luahook

import android.app.Application
import com.kulipai.luahook.di.LuaHookModule
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
            modules(LuaHookModule)
        }
    }
}