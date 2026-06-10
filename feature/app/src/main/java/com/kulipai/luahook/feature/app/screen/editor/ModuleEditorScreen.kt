package com.kulipai.luahook.feature.app.screen.editor

import androidx.compose.runtime.Composable
import com.kulipai.luahook.core.model.UiMode
import com.kulipai.luahook.core.theme.currentUiMode

@Composable
fun ModuleEditorScreen(
    moduleId: String,
) {
    when (currentUiMode()) {
        UiMode.Miuix -> ModuleEditorScreenMiuix(moduleId)
        UiMode.Material -> ModuleEditorScreenMaterial(moduleId)
    }
}
