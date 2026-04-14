package com.kulipai.luahook.feature.app.screen.profile

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import com.kulipai.luahook.core.model.UiMode
import com.kulipai.luahook.core.theme.currentEnableBlur
import com.kulipai.luahook.core.theme.currentUiMode

/**
 * @author kulipai
 * @date 2026/4/14
 */

@RequiresApi(Build.VERSION_CODES.P)
@Composable
fun AppProfileScreen(uid: Int, packageName: String) {
    when (currentUiMode()) {
        UiMode.Miuix -> AppProfileScreenMiuix(uid, packageName)
        UiMode.Material -> AppProfileScreenMaterial(uid, packageName)
    }
}
