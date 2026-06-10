package com.kulipai.luahook.feature.app.screen.script

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Redo
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kulipai.luahook.core.navigation.LocalNavigator
import com.kulipai.luahook.core.theme.currentEnableBlur
import com.kulipai.luahook.core.model.DemoContent
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeSource
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import androidx.compose.foundation.layout.size
import top.yukonga.miuix.kmp.basic.HorizontalDivider

@Composable
fun ScriptEditorScreenMiuix(
    packageName: String,
    scriptId: String,
) {
    val viewModel = koinViewModel<ScriptEditorViewModel>()
    val uiState by viewModel.uiState.collectAsState()
    val navigator = LocalNavigator.current
    val enableBlur = currentEnableBlur()
    val context = LocalContext.current

    LaunchedEffect(packageName, scriptId) {
        viewModel.load(packageName, scriptId)
    }

    val hazeState = remember { HazeState() }
    val hazeStyle = if (enableBlur) {
        HazeStyle(
            backgroundColor = colorScheme.surface,
            tint = HazeTint(colorScheme.surface.copy(0.8f))
        )
    } else {
        HazeStyle.Unspecified
    }

    val script = uiState.script

    Scaffold(
        topBar = {
            TopAppBar(
                color = if (enableBlur) Color.Transparent else colorScheme.surface,
                title = script?.title ?: scriptId,
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
            )
        },
        popupHost = { },
        contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout)
            .only(WindowInsetsSides.Horizontal)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ToolbarButton("保存") { /* TODO: Save */ }
                ToolbarButton("格式化") { /* TODO: Format */ }
                IconButton(onClick = { /* TODO: Undo */ }) {
                    Icon(
                        Icons.Rounded.Undo,
                        contentDescription = "撤回",
                        tint = colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = { /* TODO: Redo */ }) {
                    Icon(
                        Icons.Rounded.Redo,
                        contentDescription = "重做",
                        tint = colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = { /* TODO: Settings */ }) {
                    Icon(
                        Icons.Rounded.Settings,
                        contentDescription = "设置",
                        tint = colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = { /* TODO: Search */ }) {
                    Icon(
                        Icons.Rounded.Search,
                        contentDescription = "搜索",
                        tint = colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
                ToolbarButton("AI") { /* TODO: AI */ }
                ToolbarButton("日志") { /* TODO: Log */ }
                ToolbarButton("悬浮日志") { /* TODO: Floating log */ }
                ToolbarButton("运行") { /* TODO: Run */ }
            }

            HorizontalDivider(
                thickness = 0.5.dp,
                color = colorScheme.outline.copy(alpha = 0.5f)
            )

            // Editor area - placeholder for SoraEditor
            // TODO: Integrate SoraEditor via AndroidView
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp)
            ) {
                Text(
                    text = script?.content ?: "",
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    fontSize = 14.sp,
                    color = colorScheme.onSurface,
                )
            }

            HorizontalDivider(
                thickness = 0.5.dp,
                color = colorScheme.outline.copy(alpha = 0.5f)
            )

            // Symbol bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DemoContent.editorSymbols.forEach { symbol ->
                    Text(
                        text = symbol,
                        modifier = Modifier.padding(horizontal = 4.dp),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun ToolbarButton(
    label: String,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = colorScheme.primary
        )
    }
}
