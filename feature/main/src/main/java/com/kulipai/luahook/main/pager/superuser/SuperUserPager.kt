package com.kulipai.luahook.main.pager.superuser

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import com.kulipai.luahook.ui.navigation3.Navigator
import com.kulipai.luahook.core.theme.LocalUiMode
import com.kulipai.luahook.core.theme.UiMode

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
