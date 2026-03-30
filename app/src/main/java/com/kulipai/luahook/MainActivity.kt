package com.kulipai.luahook

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.kulipai.luahook.core.navigation.LocalNavigator
import com.kulipai.luahook.core.navigation.Route
import com.kulipai.luahook.core.navigation.rememberNavigator
import com.kulipai.luahook.feature.about.AboutScreen
import com.kulipai.luahook.feature.main.MainScreen
import com.kulipai.luahook.feature.main.pager.setting.SettingsViewModel
import com.kulipai.luahook.core.theme.LuaHookTheme
import com.kulipai.luahook.core.theme.LocalUiMode
import com.kulipai.luahook.core.theme.UiMode
import org.koin.androidx.compose.koinViewModel

/**
 * @author kulipai
 * @date 2026/3/17
 */

class MainActivity : ComponentActivity() {

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: SettingsViewModel = koinViewModel()
            val uiMode by viewModel.uiMode.collectAsStateWithLifecycle()

            // 保持启动页显示，直到 uiMode 加载完成（不为 null）
            splashScreen.setKeepOnScreenCondition {
                uiMode == null
            }

            if (uiMode==null) return@setContent

            val navigator = rememberNavigator(Route.Main)

            // TODO)) 封装NavDisplay到core/navigation
            CompositionLocalProvider(
                LocalNavigator provides navigator,
                LocalUiMode provides uiMode!!,
            ) {
                LuaHookTheme {
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
                        else -> {}
                    }
                }
            }
        }
    }
}
