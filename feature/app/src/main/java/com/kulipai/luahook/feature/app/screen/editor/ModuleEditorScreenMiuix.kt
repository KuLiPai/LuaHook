package com.kulipai.luahook.feature.app.screen.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Menu
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kulipai.luahook.core.navigation.LocalNavigator
import com.kulipai.luahook.core.theme.currentEnableBlur
import com.kulipai.luahook.core.model.DemoContent
import com.kulipai.luahook.core.model.ModuleFile
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun ModuleEditorScreenMiuix(
    moduleId: String,
) {
    val viewModel = koinViewModel<ModuleEditorViewModel>()
    val uiState by viewModel.uiState.collectAsState()
    val navigator = LocalNavigator.current
    val enableBlur = currentEnableBlur()

    LaunchedEffect(moduleId) {
        viewModel.load(moduleId)
    }

    val scrollBehavior = MiuixScrollBehavior()
    val module = uiState.module

    Scaffold(
        topBar = {
            TopAppBar(
                title = module?.name ?: moduleId,
                navigationIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            modifier = Modifier.padding(start = 8.dp),
                            onClick = { viewModel.toggleDrawer() }
                        ) {
                            Icon(
                                Icons.Rounded.Menu,
                                contentDescription = "文件列表",
                                tint = colorScheme.onBackground
                            )
                        }
                        IconButton(
                            modifier = Modifier.padding(start = 0.dp),
                            onClick = { navigator.pop() }
                        ) {
                            Icon(
                                imageVector = MiuixIcons.Back,
                                contentDescription = null,
                                tint = colorScheme.onBackground
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        popupHost = { },
        contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout)
            .only(WindowInsetsSides.Horizontal)
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Toolbar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ToolbarButton("保存") { /* TODO */ }
                    ToolbarButton("格式化") { /* TODO */ }
                    IconButton(onClick = { /* TODO: Undo */ }) {
                        Icon(Icons.Rounded.Undo, "撤回", tint = colorScheme.onSurface, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = { /* TODO: Redo */ }) {
                        Icon(Icons.Rounded.Redo, "重做", tint = colorScheme.onSurface, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = { /* TODO: Settings */ }) {
                        Icon(
                            Icons.Rounded.Settings,
                            "设置",
                            tint = colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = { /* TODO: Search */ }) {
                        Icon(
                            Icons.Rounded.Search,
                            "搜索",
                            tint = colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    ToolbarButton("AI") { /* TODO */ }
                    ToolbarButton("日志") { /* TODO */ }
                    ToolbarButton("运行") { /* TODO */ }
                }

                HorizontalDivider(thickness = 0.5.dp, color = colorScheme.outline.copy(alpha = 0.5f))

                // File tabs
                if (uiState.openTabs.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .background(colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        uiState.openTabs.forEach { tabPath ->
                            val fileName = tabPath.substringAfterLast("/")
                            val isActive = tabPath == uiState.activeTab
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isActive) colorScheme.primaryContainer.copy(alpha = 0.5f)
                                        else Color.Transparent,
                                        androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                                    )
                                    .clickable { viewModel.openFile(tabPath) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = fileName,
                                        fontSize = 13.sp,
                                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (isActive) colorScheme.primary else colorScheme.onSurfaceVariantSummary,
                                    )
                                    IconButton(
                                        onClick = { viewModel.closeTab(tabPath) },
                                        modifier = Modifier.size(16.dp)
                                    ) {
                                        Text("×", fontSize = 12.sp, color = colorScheme.onSurfaceVariantSummary)
                                    }
                                }
                            }
                        }
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = colorScheme.outline.copy(alpha = 0.3f))
                }

                // Editor area
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(12.dp)
                ) {
                    Text(
                        text = if (uiState.activeTab != null) uiState.activeFileContent else "选择文件以编辑",
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontSize = 14.sp,
                        color = if (uiState.activeTab != null) colorScheme.onSurface else colorScheme.onSurfaceVariantSummary,
                    )
                }

                HorizontalDivider(thickness = 0.5.dp, color = colorScheme.outline.copy(alpha = 0.5f))

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

                // Side drawer (file tree)
                if (uiState.isDrawerOpen) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(260.dp)
                            .background(colorScheme.surface.copy(alpha = 0.95f))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = "文件列表",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colorScheme.onSurface,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            uiState.files.forEach { file ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (!file.isDirectory) {
                                                viewModel.openFile(file.path)
                                            }
                                        }
                                        .padding(vertical = 8.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (file.isDirectory) "📁 ${file.path}" else "📄 ${file.path}",
                                        fontSize = 14.sp,
                                        color = colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
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
