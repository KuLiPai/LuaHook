package com.kulipai.luahook.feature.main.pager.setting

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import com.kulipai.luahook.core.model.UiMode
import com.kulipai.luahook.core.navigation.Navigator
import com.kulipai.luahook.core.theme.currentUiMode

@Composable
fun SettingPager(
    navigator: Navigator,
    bottomInnerPadding: Dp
) {
    when (currentUiMode()) {
        UiMode.Miuix -> SettingPagerMiuix(navigator, bottomInnerPadding)
        UiMode.Material -> SettingPagerMaterial(navigator, bottomInnerPadding)
    }
}
