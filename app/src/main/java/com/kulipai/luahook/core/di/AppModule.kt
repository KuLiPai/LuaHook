package com.kulipai.luahook.core.di

import com.kulipai.luahook.core.data.datastore.UserSettingsDataSource
import com.kulipai.luahook.core.data.repository.SettingsRepositoryImpl
import com.kulipai.luahook.core.data.repository.SettingsRepository
import com.kulipai.luahook.feature.main.screen.setting.SettingsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * @author kulipai
 * @date 2026/3/20
 */

val appModule = module {

    // TODO)) 注入在各个模块中具体实现
    single { UserSettingsDataSource(get()) }

    single<SettingsRepository> {
        SettingsRepositoryImpl(get())
    }


    viewModel {
        SettingsViewModel(get())
    }
}
