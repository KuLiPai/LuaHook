package com.kulipai.luahook.core.ui.component.donatecard

import androidx.compose.runtime.Composable
import com.kulipai.luahook.core.model.UiMode
import com.kulipai.luahook.core.theme.currentUiMode


@Composable
fun DonateCard() {
    when(currentUiMode()) {
        UiMode.Miuix -> DonateCardMiuix()
        UiMode.Material -> DonateCardMaterial()
    }
}