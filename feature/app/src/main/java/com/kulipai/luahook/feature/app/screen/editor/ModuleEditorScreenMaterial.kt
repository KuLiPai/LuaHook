package com.kulipai.luahook.feature.app.screen.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kulipai.luahook.core.navigation.LocalNavigator
import com.kulipai.luahook.core.model.DemoContent
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModuleEditorScreenMaterial(
    moduleId: String,
) {
    val viewModel = koinViewModel<ModuleEditorViewModel>()
    val uiState by viewModel.uiState.collectAsState()
    val navigator = LocalNavigator.current
    val module = uiState.module

    LaunchedEffect(moduleId) {
        viewModel.load(moduleId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = module?.name ?: moduleId,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.toggleDrawer() }) {
                        Icon(Icons.Filled.Menu, contentDescription = "文件列表")
                    }
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
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
                    EditorTextButton("保存") { /* TODO */ }
                    EditorTextButton("格式化") { /* TODO */ }
                    IconButton(onClick = { /* TODO */ }) { Icon(Icons.Filled.Undo, "撤回") }
                    IconButton(onClick = { /* TODO */ }) { Icon(Icons.Filled.Redo, "重做") }
                    IconButton(onClick = { /* TODO */ }) { Icon(Icons.Filled.Settings, "设置") }
                    IconButton(onClick = { /* TODO */ }) { Icon(Icons.Filled.Search, "搜索") }
                    EditorTextButton("AI") { /* TODO */ }
                    EditorTextButton("日志") { /* TODO */ }
                    EditorTextButton("运行") { /* TODO */ }
                }

                Divider()

                // File tabs
                if (uiState.openTabs.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        uiState.openTabs.forEach { tabPath ->
                            val fileName = tabPath.substringAfterLast("/")
                            val isActive = tabPath == uiState.activeTab
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                        else androidx.compose.ui.graphics.Color.Transparent,
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
                                        color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    )
                                    IconButton(
                                        onClick = { viewModel.closeTab(tabPath) },
                                        modifier = Modifier.padding()
                                    ) {
                                        Text("×", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                                    }
                                }
                            }
                        }
                    }
                    Divider()
                }

                // Editor area
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(12.dp)
                ) {
                    Text(
                        text = if (uiState.activeTab != null) uiState.activeFileContent else "选择文件以编辑",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        color = if (uiState.activeTab != null) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.outline,
                    )
                }

                Divider()

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
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                // Side drawer
                if (uiState.isDrawerOpen) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .width(260.dp)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = "文件列表",
                                style = MaterialTheme.typography.titleSmall,
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
                                        style = MaterialTheme.typography.bodyMedium,
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
private fun EditorTextButton(
    label: String,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
