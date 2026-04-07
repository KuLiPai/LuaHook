package com.kulipai.luahook.feature.main.components.warningcard

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.kulipai.luahook.core.model.UiMode
import com.kulipai.luahook.core.theme.currentUiMode

@Composable
fun WarningCard(
    message: String,
    color: Color? = null,
    onClick: (() -> Unit)? = null,
) {
    when(currentUiMode()) {
        UiMode.Miuix -> WarningCardMiuix(
            message = message,
            color = color,
            onClick = onClick
        )
        UiMode.Material -> WarningCardMaterial(
            message = message,
            color = color ?: MaterialTheme.colorScheme.error,
            onClick = onClick
        )
    }
}