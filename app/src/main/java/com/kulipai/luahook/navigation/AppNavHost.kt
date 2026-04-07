package com.kulipai.luahook.navigation

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.kulipai.luahook.core.data.model.ThemeSettings
import com.kulipai.luahook.core.model.UiMode
import com.kulipai.luahook.core.navigation.LocalNavigator
import com.kulipai.luahook.core.navigation.Route
import com.kulipai.luahook.core.theme.LuaHookTheme
import com.kulipai.luahook.core.theme.currentUiMode
import com.kulipai.luahook.feature.about.navigation.aboutGraph
import com.kulipai.luahook.feature.main.navigation.mainGraph
import com.kulipai.luahook.rememberNavigator


@RequiresApi(Build.VERSION_CODES.S)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun AppNavHost(
    themeSettings: ThemeSettings,
) {
    val navigator = rememberNavigator(Route.Main)

    CompositionLocalProvider(
        LocalNavigator provides navigator,
    ) {
        LuaHookTheme(themeSettings = themeSettings) {
            val uiMode = currentUiMode()
            val navDisplay = @Composable {
                SharedTransitionLayout {
                    NavDisplay(
                        backStack = navigator.backStack,
                        entryDecorators = listOf(
                            rememberSaveableStateHolderNavEntryDecorator(),
                            rememberViewModelStoreNavEntryDecorator()
                        ),
                        onBack = {
                            navigator.pop()
                        },
                        entryProvider = appEntryProvider(this@SharedTransitionLayout),
                    )
                }
            }

            when (uiMode) {
                UiMode.Material -> androidx.compose.material3.Scaffold { navDisplay() }
                UiMode.Miuix -> top.yukonga.miuix.kmp.basic.Scaffold { navDisplay() }
            }
        }
    }
}


/**
 * 构建应用级路由注册器
 *
 * 按模块聚合 graph，避免全部 entry 混在同一个函数中。
 *
 * @return 应用级 EntryProvider
 * @author kulipai
 */
private fun appEntryProvider(sharedTransitionScope: SharedTransitionScope) = entryProvider {
    mainGraph()
    aboutGraph()
//    mainGraph(sharedTransitionScope)
//    goodsGraph()
//    authGraph()
//    userGraph(sharedTransitionScope)
//    orderGraph()
//    csGraph()
//    commonGraph()
//    marketGraph()
//    feedbackGraph()
//    launchGraph(sharedTransitionScope)
}
