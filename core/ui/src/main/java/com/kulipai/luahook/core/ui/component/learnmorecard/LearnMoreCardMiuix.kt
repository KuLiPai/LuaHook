package com.kulipai.luahook.core.ui.component.learnmorecard


import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import com.kulipai.luahook.core.ui.R
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Link
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme


@Composable
fun LearnMoreCardMiuix() {
    val uriHandler = LocalUriHandler.current
    val url = stringResource(R.string.home_learn_luahook_url)

    Card(
        modifier = Modifier
            .fillMaxWidth(),
    ) {
        BasicComponent(
            title = stringResource(R.string.home_learn_luahook),
            summary = stringResource(R.string.home_click_to_learn_luahook),
            endActions = {
                Icon(
                    imageVector = MiuixIcons.Link,
                    tint = colorScheme.onSurface,
                    contentDescription = null
                )
            },
            onClick = {
                uriHandler.openUri(url)
            }
        )
    }
}
