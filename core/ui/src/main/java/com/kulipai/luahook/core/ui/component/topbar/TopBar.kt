package com.kulipai.luahook.core.ui.component.topbar

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import com.kulipai.luahook.core.model.UiMode
import com.kulipai.luahook.core.theme.currentUiMode
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import kotlin.String


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    title: String,
    materialScrollBehavior: TopAppBarScrollBehavior? = null,
    // miuix必填属性
    miuixScrollBehavior: ScrollBehavior? = null,
    hazeState: HazeState? = null,
    hazeStyle: HazeStyle? = null,
    enableBlur: Boolean = false
) {
    when (currentUiMode()) {
        UiMode.Material -> TopBarMaterial(
            title = title,
            scrollBehavior = materialScrollBehavior
        )
        UiMode.Miuix -> TopBarMiuix(
            title = title,
            scrollBehavior = miuixScrollBehavior,
            hazeState = hazeState!!,
            hazeStyle = hazeStyle!!,
            enableBlur = enableBlur,
        )
    }
}