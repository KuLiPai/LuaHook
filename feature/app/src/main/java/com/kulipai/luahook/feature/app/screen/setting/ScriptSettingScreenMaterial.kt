package com.kulipai.luahook.feature.app.screen.setting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.kulipai.luahook.core.navigation.LocalNavigator
import com.kulipai.luahook.core.ui.component.material.SegmentedColumn
import com.kulipai.luahook.core.ui.component.material.SegmentedSwitchItem
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptSettingScreenMaterial(
    packageName: String,
    scriptId: String,
) {
    val viewModel = koinViewModel<ScriptSettingViewModel>()
    val uiState by viewModel.uiState.collectAsState()
    val navigator = LocalNavigator.current
    val script = uiState.script

    LaunchedEffect(packageName, scriptId) {
        viewModel.load(packageName, scriptId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(script?.title ?: "脚本设置") },
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                SegmentedColumn(
                    modifier = Modifier.fillMaxWidth(),
                    content = listOf(
                        {
                            SegmentedSwitchItem(
                                title = "自动启动",
                                summary = "应用启动时自动运行此脚本",
                                checked = uiState.autoStart,
                                onCheckedChange = { viewModel.setAutoStart(it) },
                            )
                        },
                        {
                            SegmentedSwitchItem(
                                title = "显示在启动器",
                                summary = "在应用启动器中显示快捷方式",
                                checked = uiState.showInLauncher,
                                onCheckedChange = { viewModel.setShowInLauncher(it) },
                            )
                        },
                    )
                )
            }

            item {
                SegmentedColumn(
                    modifier = Modifier.fillMaxWidth(),
                    content = listOf(
                        {
                            SegmentedSwitchItem(
                                title = "日志输出",
                                summary = "在日志页面显示此脚本的运行日志",
                                checked = uiState.logEnabled,
                                onCheckedChange = { viewModel.setLogEnabled(it) },
                            )
                        },
                        {
                            SegmentedSwitchItem(
                                title = "悬浮日志",
                                summary = "运行时在屏幕上显示悬浮日志窗口",
                                checked = uiState.floatingLogEnabled,
                                onCheckedChange = { viewModel.setFloatingLogEnabled(it) },
                            )
                        },
                    )
                )
            }

            item {
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
