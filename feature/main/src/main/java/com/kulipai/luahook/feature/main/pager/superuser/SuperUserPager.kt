package com.kulipai.luahook.feature.main.pager.superuser

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import com.kulipai.luahook.core.model.UiMode
import com.kulipai.luahook.core.navigation.Navigator
import com.kulipai.luahook.core.theme.currentUiMode

@Composable
fun SuperUserPager(
    navigator: Navigator,
    bottomInnerPadding: Dp
) {
    when (currentUiMode()) {
        UiMode.Miuix -> SuperUserPagerMiuix(navigator, bottomInnerPadding)
        UiMode.Material -> SuperUserPagerMaterial(navigator, bottomInnerPadding)
    }
}
