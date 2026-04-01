package com.kulipai.luahook.feature.about.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.kulipai.luahook.core.navigation.Route
import com.kulipai.luahook.feature.about.screen.AboutScreen

/**
 * 主模块导航图
 *
 * @author kulipai
 * @date 2026/4/1
 */
@OptIn(ExperimentalSharedTransitionApi::class)
fun EntryProviderScope<NavKey>.aboutGraph(
) {
    entry<Route.About> {
        AboutScreen()
    }
}
