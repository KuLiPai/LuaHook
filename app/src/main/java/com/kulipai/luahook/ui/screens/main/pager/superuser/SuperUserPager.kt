package com.kulipai.luahook.ui.screens.main.pager.superuser

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import com.kulipai.luahook.ui.navigation3.Navigator
import com.kulipai.luahook.ui.theme.LocalUiMode
import com.kulipai.luahook.ui.theme.UiMode

@Composable
fun SuperUserPager(
    navigator: Navigator,
    bottomInnerPadding: Dp
) {
    when (LocalUiMode.current) {
        UiMode.Miuix -> SuperUserPagerMiuix(navigator, bottomInnerPadding)
        UiMode.Material -> SuperUserPagerMaterial(navigator, bottomInnerPadding)
    }
}
