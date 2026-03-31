package com.kulipai.luahook.core.di

import com.kulipai.luahook.core.data.datastore.UserPreferencesDataSource
import com.kulipai.luahook.core.data.repository.SettingsRepositoryImpl
import com.kulipai.luahook.core.data.repository.SettingsRepository
import com.kulipai.luahook.core.theme.ThemePreferenceManager
import com.kulipai.luahook.feature.main.pager.setting.SettingsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * @author kulipai
 * @date 2026/3/20
 */

val appModule = module {

    single { UserPreferencesDataSource(get()) }

    single<SettingsRepository> {
        SettingsRepositoryImpl(get())
    }

    single { ThemePreferenceManager(get()) }

    viewModel {
        SettingsViewModel(get())
    }
}
