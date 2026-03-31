package com.kulipai.luahook.navigation

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.kulipai.luahook.core.model.ThemePreference
import com.kulipai.luahook.core.model.UiMode
import com.kulipai.luahook.core.navigation.LocalNavigator
import com.kulipai.luahook.core.navigation.Route
import com.kulipai.luahook.core.theme.LuaHookTheme
import com.kulipai.luahook.core.theme.currentUiMode
import com.kulipai.luahook.feature.about.AboutScreen
import com.kulipai.luahook.feature.main.MainScreen
import com.kulipai.luahook.rememberNavigator


@RequiresApi(Build.VERSION_CODES.S)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun AppNavHost(
    themePreference: ThemePreference,
) {
    val navigator = rememberNavigator(Route.Main)

    CompositionLocalProvider(
        LocalNavigator provides navigator,
    ) {
        LuaHookTheme(themePreference = themePreference) {
            val uiMode = currentUiMode()
            val navDisplay = @Composable {
                NavDisplay(
                    backStack = navigator.backStack,
                    entryDecorators = listOf(
                        rememberSaveableStateHolderNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator()
                    ),
                    onBack = {
                        navigator.pop()
                    },
                    entryProvider = entryProvider {
                        entry<Route.Main> { MainScreen() }
                        entry<Route.About> { AboutScreen() }

                    }
                )
            }

            when (uiMode) {
                UiMode.Material -> androidx.compose.material3.Scaffold { navDisplay() }
                UiMode.Miuix -> top.yukonga.miuix.kmp.basic.Scaffold { navDisplay() }
            }
        }
    }
}
