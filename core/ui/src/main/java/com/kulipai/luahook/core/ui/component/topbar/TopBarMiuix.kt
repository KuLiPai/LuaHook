package com.kulipai.luahook.core.ui.component.topbar


import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeEffect
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

@Composable
fun TopBarMiuix(
    title: String,
    scrollBehavior: ScrollBehavior?,
    hazeState: HazeState,
    hazeStyle: HazeStyle,
    enableBlur: Boolean,
) {
    TopAppBar(
        modifier = if (enableBlur) {
            Modifier.hazeEffect(hazeState) {
                style = hazeStyle
                blurRadius = 30.dp
                noiseFactor = 0f
            }
        } else {
            Modifier
        },
        color = if (enableBlur) Color.Transparent else colorScheme.surface,
        title = title,
        actions = {
//            RebootListPopupMiuix(
//                modifier = Modifier.padding(end = 16.dp),
//            )
        },
        scrollBehavior = scrollBehavior
    )
}
