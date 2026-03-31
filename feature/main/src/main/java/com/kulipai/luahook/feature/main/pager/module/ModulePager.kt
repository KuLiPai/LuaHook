package com.kulipai.luahook.feature.main.pager.module

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import com.kulipai.luahook.core.model.UiMode
import com.kulipai.luahook.core.navigation.Navigator
import com.kulipai.luahook.core.theme.currentUiMode

@Composable
fun ModulePager(
    navigator: Navigator,
    bottomInnerPadding: Dp
) {
    when (currentUiMode()) {
        UiMode.Miuix -> ModulePagerMiuix(navigator, bottomInnerPadding)
        UiMode.Material -> ModulePagerMaterial(navigator, bottomInnerPadding)
    }
}
