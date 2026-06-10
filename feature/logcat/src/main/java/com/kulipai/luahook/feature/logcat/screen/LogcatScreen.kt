package com.kulipai.luahook.feature.logcat.screen

import androidx.compose.runtime.Composable
import com.kulipai.luahook.core.model.UiMode
import com.kulipai.luahook.core.theme.currentUiMode

@Composable
fun LogcatScreen() {
    when (currentUiMode()) {
        UiMode.Miuix -> LogcatScreenMiuix()
        UiMode.Material -> LogcatScreenMaterial()
    }
}
