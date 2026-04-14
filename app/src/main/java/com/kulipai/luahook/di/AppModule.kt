package com.kulipai.luahook.di

import com.kulipai.luahook.core.data.di.dataModule
import com.kulipai.luahook.feature.app.di.appModule
import com.kulipai.luahook.feature.main.di.mainModule
import org.koin.dsl.module

/**
 * @author kulipai
 * @date 2026/3/20
 */

val LuaHookModule = module {


    // repository注入
    includes(dataModule)

    // feature/main注入
    includes(mainModule)

    includes(appModule)


}
