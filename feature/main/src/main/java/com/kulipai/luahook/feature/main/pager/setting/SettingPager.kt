package com.kulipai.luahook.feature.main.pager.setting

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import com.kulipai.luahook.core.navigation.Navigator
import com.kulipai.luahook.core.theme.LocalUiMode
import com.kulipai.luahook.core.theme.UiMode

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
