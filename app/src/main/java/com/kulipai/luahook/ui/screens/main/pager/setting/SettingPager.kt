package com.kulipai.luahook.ui.screens.main.pager.setting

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import com.kulipai.luahook.ui.navigation3.Navigator
import com.kulipai.luahook.ui.theme.LocalUiMode
import com.kulipai.luahook.ui.theme.UiMode

@Composable
fun SettingPager(
    navigator: Navigator,
    bottomInnerPadding: Dp
) {
    when (LocalUiMode.current) {
        UiMode.Miuix -> SettingPagerMiuix(navigator, bottomInnerPadding)
        UiMode.Material -> SettingPagerMaterial(navigator, bottomInnerPadding)
    }
}
