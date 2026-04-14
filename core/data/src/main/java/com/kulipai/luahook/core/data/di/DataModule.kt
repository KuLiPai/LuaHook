package com.kulipai.luahook.core.data.di

import com.kulipai.luahook.core.data.datastore.UserSettingsDataSource
import com.kulipai.luahook.core.data.repository.AppsRepository
import com.kulipai.luahook.core.data.repository.AppsRepositoryImpl
import com.kulipai.luahook.core.data.repository.SettingsRepository
import com.kulipai.luahook.core.data.repository.SettingsRepositoryImpl
import org.koin.dsl.module

val dataModule = module {
    single { UserSettingsDataSource(get()) }

    single<AppsRepository> { AppsRepositoryImpl(get()) }

    single<SettingsRepository> {
        SettingsRepositoryImpl(get())
    }
}
