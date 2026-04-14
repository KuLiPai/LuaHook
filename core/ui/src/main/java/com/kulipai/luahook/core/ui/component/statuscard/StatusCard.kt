package com.kulipai.luahook.core.ui.component.statuscard

import androidx.compose.runtime.Composable
import com.kulipai.luahook.core.model.UiMode
import com.kulipai.luahook.core.theme.currentUiMode

@Composable
fun StatusCard(
    //    kernelVersion: KernelVersion,
    ksuVersion: Int?,
    lkmMode: Boolean?,
    isSafeMode: Boolean = true,
    isLateLoadMode: Boolean = true,
//    isSELinuxPermissive: Boolean,
    superuserCount: Int = 100,
    moduleCount: Int = 213980,
    onClickInstall: () -> Unit = {},
//    onClickJailbreak: () -> Unit = {},
    onClickSuperuser: () -> Unit = {},
    onclickModule: () -> Unit = {},
) {
    when (currentUiMode()) {
        UiMode.Miuix -> StatusCardMiuix(
            ksuVersion = ksuVersion,
            lkmMode = lkmMode,
            isSafeMode = isSafeMode,
            isLateLoadMode = isLateLoadMode,
            superuserCount = superuserCount,
            moduleCount = moduleCount,
            onClickInstall = onClickInstall,
            onClickSuperuser = onClickSuperuser,
            onclickModule = onclickModule,
        )

        UiMode.Material -> StatusCardMaterial(
            ksuVersion = ksuVersion,
            lkmMode = lkmMode,
            isSafeMode = isSafeMode,
            isLateLoadMode = isLateLoadMode,
            superuserCount = superuserCount,
            moduleCount = moduleCount,
            onClickInstall = onClickInstall,
            onClickSuperuser = onClickSuperuser,
            onclickModule = onclickModule,
        )
    }

}