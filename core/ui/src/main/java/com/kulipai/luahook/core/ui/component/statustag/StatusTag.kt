package com.kulipai.luahook.core.ui.component.statustag

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.kulipai.luahook.core.model.UiMode
import com.kulipai.luahook.core.theme.currentUiMode

@Composable
fun StatusTag(
    label: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color,
    contentColor: Color
) {
    when (currentUiMode()) {
        UiMode.Miuix -> StatusTagMiuix(label, backgroundColor, contentColor)
        UiMode.Material -> StatusTagMaterial(label, modifier, backgroundColor, contentColor)
    }
}
