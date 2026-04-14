package com.kulipai.luahook.core.ui.component.warningcard


import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kulipai.luahook.core.theme.isInDarkTheme
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.isDynamicColor
import top.yukonga.miuix.kmp.utils.PressFeedbackType

@Composable
fun WarningCardMiuix(
    message: String,
    color: Color? = null,
    onClick: (() -> Unit)? = null,
) {
    Card(
        onClick = {
            onClick?.invoke()
        },
        colors = CardDefaults.defaultColors(
            color = color ?: when {
                isDynamicColor -> colorScheme.errorContainer
                isInDarkTheme() -> Color(0XFF310808)
                else -> Color(0xFFF8E2E2)
            }
        ),
        showIndication = onClick != null,
        pressFeedbackType = PressFeedbackType.Tilt
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = message,
                color = if (isDynamicColor) colorScheme.onErrorContainer else Color(0xFFF72727),
                fontSize = 14.sp
            )
        }
    }
}
