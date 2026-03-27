package com.kulipai.luahook.ui.screens.main.pager.module

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import com.kulipai.luahook.ui.navigation3.Navigator
import com.kulipai.luahook.ui.theme.LocalUiMode
import com.kulipai.luahook.ui.theme.UiMode

@Composable
fun ModulePager(
    navigator: Navigator,
    bottomInnerPadding: Dp
) {
    when (LocalUiMode.current) {
        UiMode.Miuix -> ModulePagerMiuix(navigator, bottomInnerPadding)
        UiMode.Material -> ModulePagerMaterial(navigator, bottomInnerPadding)
    }
}
