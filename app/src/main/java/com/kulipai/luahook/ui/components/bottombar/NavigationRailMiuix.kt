/*
 * This file is based on code from KernelSU
 * (https://github.com/tiann/KernelSU)
 *
 * Licensed under GPL-3.0
 *
 * Modifications by KuLiPai
 */

package com.kulipai.luahook.ui.components.bottombar

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kulipai.luahook.ui.theme.LocalEnableBlur
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeEffect
import top.yukonga.miuix.kmp.basic.NavigationRail
import top.yukonga.miuix.kmp.basic.NavigationRailItem
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun NavigationRailMiuix(
    hazeState: HazeState,
    hazeStyle: HazeStyle,
    modifier: Modifier = Modifier,
) {


    val mainState = LocalMainPagerState.current
    val enableBlur = LocalEnableBlur.current

    val items = BottomBarDestination.entries.map { destination ->
        Pair(stringResource(destination.label), destination.icon)
    }

    NavigationRail(
        modifier = modifier
            .fillMaxHeight()
            .then(
                if (enableBlur) {
                    Modifier.hazeEffect(hazeState) {
                        style = hazeStyle
                        blurRadius = 30.dp
                        noiseFactor = 0f
                    }
                } else Modifier
            ),
        color = if (enableBlur) Color.Transparent else MiuixTheme.colorScheme.surface,
    ) {
        items.forEachIndexed { index, (label, icon) ->
            NavigationRailItem(
                icon = icon,
                label = label,
                selected = mainState.selectedPage == index,
                onClick = {
                    mainState.animateToPage(index)
                }
            )
        }
    }
}
