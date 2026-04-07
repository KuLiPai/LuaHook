package com.kulipai.luahook.feature.main.screen.apps

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import com.kulipai.luahook.core.model.UiMode
import com.kulipai.luahook.core.navigation.Navigator
import com.kulipai.luahook.core.theme.currentUiMode

@Composable
fun AppsPager(
    navigator: Navigator,
    bottomInnerPadding: Dp
) {
    when (currentUiMode()) {
        UiMode.Miuix -> AppsPagerMiuix(navigator, bottomInnerPadding)
        UiMode.Material -> AppsPagerMaterial(navigator, bottomInnerPadding)
    }
}
