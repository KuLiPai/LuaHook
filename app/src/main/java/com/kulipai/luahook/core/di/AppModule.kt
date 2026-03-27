package com.kulipai.luahook.core.di

import com.kulipai.luahook.data.repository.SettingsRepository
import com.kulipai.luahook.ui.screens.main.pager.setting.SettingsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * @author kulipai
 * @date 2026/3/20
 */

val appModule = module {

    // Repository（自动注入 context）
    single {
        SettingsRepository(get())
    }

    // ViewModel
    viewModel {
        SettingsViewModel(get())
    }
}