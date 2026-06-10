package com.kulipai.luahook.feature.app.screen.script

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

data class ScriptEditorUiState(
    val isLoading: Boolean = false,
    val script: ScriptDefinition? = null,
    val content: String = "",
)

class ScriptEditorViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ScriptEditorUiState())
    val uiState: StateFlow<ScriptEditorUiState> = _uiState.asStateFlow()

    fun load(packageName: String, scriptId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // TODO: Replace with actual script loading from data layer
            val script = withContext(Dispatchers.IO) {
                DemoContent.findScript(packageName, scriptId)
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    script = script,
                    content = script.content,
                )
            }
        }
    }

    fun updateContent(newContent: String) {
        _uiState.update { it.copy(content = newContent) }
    }

    fun save() {
        // TODO: Save script content to data layer
    }
}
