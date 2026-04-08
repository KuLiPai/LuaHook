package com.kulipai.luahook.feature.main.di

import com.kulipai.luahook.feature.main.screen.apps.AppsPagerViewModel
import com.kulipai.luahook.feature.main.screen.setting.SettingPagerViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module


val mainModule = module {


    viewModel {
        AppsPagerViewModel(get(),get())
    }
    viewModel {
        SettingPagerViewModel(get())
    }
}
