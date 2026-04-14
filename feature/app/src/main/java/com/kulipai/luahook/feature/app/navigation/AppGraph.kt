package com.kulipai.luahook.feature.app.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.kulipai.luahook.core.navigation.Route
import com.kulipai.luahook.feature.app.screen.profile.AppProfileScreen

/**
 * app导航图
 *
 * @author kulipai
 * @date 2026/4/14
 */
@RequiresApi(Build.VERSION_CODES.P)
@OptIn(ExperimentalSharedTransitionApi::class)
fun EntryProviderScope<NavKey>.appGraph(

) {
    entry<Route.AppProfile> { (uid, packageName) ->
        AppProfileScreen(
            uid = uid,
            packageName = packageName
        )
    }

}
