package com.kulipai.luahook.feature.main.components.learnmorecard

import androidx.compose.runtime.Composable
import com.kulipai.luahook.core.model.UiMode
import com.kulipai.luahook.core.theme.currentUiMode

@Composable
fun LearnMoreCard() {
    when(currentUiMode()) {
        UiMode.Miuix -> LearnMoreCardMiuix()
        UiMode.Material -> LearnMoreCardMaterial()
    }
}