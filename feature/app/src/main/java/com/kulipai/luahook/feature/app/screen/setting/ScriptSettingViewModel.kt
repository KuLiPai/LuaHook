package com.kulipai.luahook.feature.app.screen.setting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kulipai.luahook.core.model.DemoContent
import com.kulipai.luahook.core.model.ScriptDefinition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ScriptSettingUiState(
    val isLoading: Boolean = false,
    val script: ScriptDefinition? = null,
    val autoStart: Boolean = false,
    val showInLauncher: Boolean = false,
    val logEnabled: Boolean = true,
    val floatingLogEnabled: Boolean = false,
)

class ScriptSettingViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ScriptSettingUiState())
    val uiState: StateFlow<ScriptSettingUiState> = _uiState.asStateFlow()

    fun load(packageName: String, scriptId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val script = withContext(Dispatchers.IO) {
                DemoContent.findScript(packageName, scriptId)
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    script = script,
                    autoStart = script.enabled,
                    logEnabled = true,
                )
            }
        }
    }

    fun setAutoStart(enabled: Boolean) {
        _uiState.update { it.copy(autoStart = enabled) }
    }

    fun setShowInLauncher(enabled: Boolean) {
        _uiState.update { it.copy(showInLauncher = enabled) }
    }

    fun setLogEnabled(enabled: Boolean) {
        _uiState.update { it.copy(logEnabled = enabled) }
    }

    fun setFloatingLogEnabled(enabled: Boolean) {
        _uiState.update { it.copy(floatingLogEnabled = enabled) }
    }
}
