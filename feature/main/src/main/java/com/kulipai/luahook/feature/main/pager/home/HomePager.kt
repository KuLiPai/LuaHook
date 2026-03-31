package com.kulipai.luahook.feature.main.pager.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import com.kulipai.luahook.core.model.UiMode
import com.kulipai.luahook.core.navigation.Navigator
import com.kulipai.luahook.core.theme.currentUiMode

@Composable
fun HomePager(
    navigator: Navigator,
    bottomInnerPadding: Dp
) {
    when (currentUiMode()) {
        UiMode.Miuix -> HomePagerMiuix(navigator, bottomInnerPadding)
        UiMode.Material -> HomePagerMaterial(navigator, bottomInnerPadding)
    }
}
