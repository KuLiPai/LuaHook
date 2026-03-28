package com.kulipai.luahook.feature.main.pager.module

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import com.kulipai.luahook.ui.navigation3.Navigator
import com.kulipai.luahook.core.theme.LocalUiMode
import com.kulipai.luahook.core.theme.UiMode

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
