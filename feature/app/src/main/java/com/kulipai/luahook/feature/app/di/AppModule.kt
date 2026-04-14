package com.kulipai.luahook.feature.app.di


import com.kulipai.luahook.feature.app.screen.profile.AppProfileViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * @author kulipai
 * @date 2026/4/14
 */

val appModule = module {
    viewModel {
        AppProfileViewModel(get())
    }
}
