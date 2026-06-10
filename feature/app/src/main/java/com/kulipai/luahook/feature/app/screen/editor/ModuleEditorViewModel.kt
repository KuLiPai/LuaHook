package com.kulipai.luahook.feature.app.screen.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kulipai.luahook.core.model.DemoContent
import com.kulipai.luahook.core.model.DemoModule
import com.kulipai.luahook.core.model.ModuleFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ModuleEditorUiState(
    val isLoading: Boolean = false,
    val module: DemoModule? = null,
    val files: List<ModuleFile> = emptyList(),
    val openTabs: List<String> = emptyList(),
    val activeTab: String? = null,
    val activeFileContent: String = "",
    val isDrawerOpen: Boolean = false,
)

class ModuleEditorViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ModuleEditorUiState())
    val uiState: StateFlow<ModuleEditorUiState> = _uiState.asStateFlow()

    fun load(moduleId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val module = withContext(Dispatchers.IO) {
                DemoContent.findModule(moduleId)
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    module = module,
                    files = module.files,
                )
            }
        }
    }

    fun openFile(path: String) {
        val file = _uiState.value.files.find { it.path == path } ?: return
        val currentTabs = _uiState.value.openTabs

        _uiState.update {
            val newTabs = if (path in currentTabs) currentTabs else currentTabs + path
            it.copy(
                openTabs = newTabs,
                activeTab = path,
                activeFileContent = file.content,
                isDrawerOpen = false,
            )
        }
    }

    fun closeTab(path: String) {
        val currentTabs = _uiState.value.openTabs
        val currentActive = _uiState.value.activeTab
        val newTabs = currentTabs - path

        val newActive = when {
            currentActive == path && newTabs.isNotEmpty() -> newTabs.last()
            currentActive == path -> null
            else -> currentActive
        }

        val newContent = if (newActive != null) {
            _uiState.value.files.find { it.path == newActive }?.content ?: ""
        } else ""

        _uiState.update {
            it.copy(
                openTabs = newTabs,
                activeTab = newActive,
                activeFileContent = newContent,
            )
        }
    }

    fun closeOtherTabs(keepPath: String) {
        _uiState.update {
            it.copy(
                openTabs = listOf(keepPath),
                activeTab = keepPath,
                activeFileContent = it.files.find { f -> f.path == keepPath }?.content ?: "",
            )
        }
    }

    fun toggleDrawer() {
        _uiState.update { it.copy(isDrawerOpen = !it.isDrawerOpen) }
    }

    fun updateContent(newContent: String) {
        _uiState.update { it.copy(activeFileContent = newContent) }
    }

    fun createFile(name: String, isDirectory: Boolean) {
        // TODO: Implement file creation
    }

    fun importFile() {
        // TODO: Implement file import
    }
}
