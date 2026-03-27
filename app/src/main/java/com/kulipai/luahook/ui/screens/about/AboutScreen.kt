package com.kulipai.luahook.ui.screens.about

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import com.kulipai.luahook.ui.navigation3.Navigator
import com.kulipai.luahook.ui.screens.main.pager.module.ModulePagerMaterial
import com.kulipai.luahook.ui.screens.main.pager.module.ModulePagerMiuix
import com.kulipai.luahook.ui.theme.LocalUiMode
import com.kulipai.luahook.ui.theme.UiMode

@Composable
fun AboutScreen() {
    when (LocalUiMode.current) {
        UiMode.Miuix -> AboutScreenMiuix()
        UiMode.Material -> AboutScreenMaterial()
    }
}
