package com.kulipai.luahook.feature.main.components.infocard

import androidx.compose.runtime.Composable
import com.kulipai.luahook.core.model.UiMode
import com.kulipai.luahook.core.theme.currentUiMode


@Composable
fun InfoCard() {
    when(currentUiMode()) {
        UiMode.Miuix -> InfoCardMiuix()
        UiMode.Material -> InfoCardMaterial()
    }
}