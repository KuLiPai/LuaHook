package com.kulipai.luahook.ui.screens.main.pager.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import com.kulipai.luahook.ui.navigation3.Navigator
import com.kulipai.luahook.ui.theme.LocalUiMode
import com.kulipai.luahook.ui.theme.UiMode

@Composable
fun HomePager(
    navigator: Navigator,
    bottomInnerPadding: Dp
) {
    when (LocalUiMode.current) {
        UiMode.Miuix -> HomePagerMiuix(navigator, bottomInnerPadding)
        UiMode.Material -> HomePagerMaterial(navigator, bottomInnerPadding)
    }
}
