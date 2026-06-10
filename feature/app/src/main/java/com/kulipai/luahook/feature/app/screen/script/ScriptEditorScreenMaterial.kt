package com.kulipai.luahook.feature.app.screen.script

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
fun ScriptEditorScreenMaterial(
    packageName: String,
    scriptId: String,
) {
    val viewModel = koinViewModel<ScriptEditorViewModel>()
    val uiState by viewModel.uiState.collectAsState()
    val navigator = LocalNavigator.current
    val script = uiState.script

    LaunchedEffect(packageName, scriptId) {
        viewModel.load(packageName, scriptId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = script?.title ?: scriptId,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
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
                EditorTextButton("保存") { /* TODO */ }
                EditorTextButton("格式化") { /* TODO */ }
                IconButton(onClick = { /* TODO: Undo */ }) {
                    Icon(Icons.Filled.Undo, contentDescription = "撤回", modifier = Modifier.padding())
                }
                IconButton(onClick = { /* TODO: Redo */ }) {
                    Icon(Icons.Filled.Redo, contentDescription = "重做")
                }
                IconButton(onClick = { /* TODO: Settings */ }) {
                    Icon(Icons.Filled.Settings, contentDescription = "设置")
                }
                IconButton(onClick = { /* TODO: Search */ }) {
                    Icon(Icons.Filled.Search, contentDescription = "搜索")
                }
                EditorTextButton("AI") { /* TODO */ }
                EditorTextButton("日志") { /* TODO */ }
                EditorTextButton("悬浮日志") { /* TODO */ }
                EditorTextButton("运行") { /* TODO */ }
            }

            Divider()

            // Editor area placeholder
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp)
            ) {
                Text(
                    text = script?.content ?: "",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
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
