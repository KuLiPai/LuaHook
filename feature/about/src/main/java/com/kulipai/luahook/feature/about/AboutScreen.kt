package com.kulipai.luahook.feature.about

import androidx.compose.runtime.Composable
import com.kulipai.luahook.core.theme.LocalUiMode
import com.kulipai.luahook.core.theme.UiMode

@Composable
fun AboutScreen() {
    when (LocalUiMode.current) {
        UiMode.Miuix -> AboutScreenMiuix()
        UiMode.Material -> AboutScreenMaterial()
    }
}
