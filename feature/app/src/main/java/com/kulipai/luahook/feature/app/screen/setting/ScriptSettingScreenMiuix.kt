package com.kulipai.luahook.feature.app.screen.setting

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.kulipai.luahook.core.navigation.LocalNavigator
import com.kulipai.luahook.core.theme.currentEnableBlur
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeSource
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun ScriptSettingScreenMiuix(
    packageName: String,
    scriptId: String,
) {
    val viewModel = koinViewModel<ScriptSettingViewModel>()
    val uiState by viewModel.uiState.collectAsState()
    val navigator = LocalNavigator.current
    val enableBlur = currentEnableBlur()
    val scrollBehavior = MiuixScrollBehavior()
    val script = uiState.script

    LaunchedEffect(packageName, scriptId) {
        viewModel.load(packageName, scriptId)
    }

    val hazeState = HazeState()
    val hazeStyle = if (enableBlur) {
        HazeStyle(
            backgroundColor = colorScheme.surface,
            tint = HazeTint(colorScheme.surface.copy(0.8f))
        )
    } else {
        HazeStyle.Unspecified
    }

    Scaffold(
        topBar = {
            TopAppBar(
                color = if (enableBlur) androidx.compose.ui.graphics.Color.Transparent else colorScheme.surface,
                title = script?.title ?: "脚本设置",
                navigationIcon = {
                    IconButton(
                        modifier = Modifier.padding(start = 16.dp),
                        onClick = { navigator.pop() }
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Back,
                            contentDescription = null,
                            tint = colorScheme.onBackground
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        popupHost = { },
        contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout)
            .only(WindowInsetsSides.Horizontal)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxHeight()
                .scrollEndHaptic()
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .let { if (enableBlur) it.hazeSource(state = hazeState) else it }
                .padding(horizontal = 12.dp),
            contentPadding = innerPadding,
            overscrollEffect = null,
        ) {
            item {
                Card(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 12.dp)
                        .fillMaxWidth(),
                ) {
                    SwitchPreference(
                        title = "自动启动",
                        summary = "应用启动时自动运行此脚本",
                        checked = uiState.autoStart,
                        onCheckedChange = { viewModel.setAutoStart(it) },
                    )
                    SwitchPreference(
                        title = "显示在启动器",
                        summary = "在应用启动器中显示快捷方式",
                        checked = uiState.showInLauncher,
                        onCheckedChange = { viewModel.setShowInLauncher(it) },
                    )
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .padding(bottom = 12.dp)
                        .fillMaxWidth(),
                ) {
                    SwitchPreference(
                        title = "日志输出",
                        summary = "在日志页面显示此脚本的运行日志",
                        checked = uiState.logEnabled,
                        onCheckedChange = { viewModel.setLogEnabled(it) },
                    )
                    SwitchPreference(
                        title = "悬浮日志",
                        summary = "运行时在屏幕上显示悬浮日志窗口",
                        checked = uiState.floatingLogEnabled,
                        onCheckedChange = { viewModel.setFloatingLogEnabled(it) },
                    )
                }
            }

            item {
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}
