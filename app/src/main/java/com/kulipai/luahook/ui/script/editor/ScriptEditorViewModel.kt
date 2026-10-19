package com.kulipai.luahook.ui.script.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kulipai.luahook.R
import com.kulipai.luahook.app.di.AppContainer
import com.kulipai.luahook.core.file.WorkspaceEventManager
import com.kulipai.luahook.core.file.WorkspaceFileManager
import com.kulipai.luahook.data.repository.ScriptRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.luaj.lib.jse.JsePlatform
import java.io.File

sealed class ScriptEditorMode {
    abstract val relativePath: String
    abstract val title: String

    object Global : ScriptEditorMode() {
        override val relativePath: String = "/global.lua"
        override val title: String = "Global"
    }

    data class AppSpecific(
        val packageName: String,
        val scriptName: String,
        val appName: String = "",
        val scriptDescription: String = ""
    ) : ScriptEditorMode() {
        override val relativePath: String = "${WorkspaceFileManager.AppScript}/$packageName/$scriptName.lua"
        override val title: String = scriptName
    }

    data class ProjectScript(
        val projectName: String,
        val filePath: String
    ) : ScriptEditorMode() {
        override val relativePath: String = "${WorkspaceFileManager.Project}/$projectName/${filePath.trimStart('/')}"
        override val title: String = filePath.substringAfterLast("/")
    }

    data class Custom(
        override val relativePath: String,
        override val title: String = relativePath.substringAfterLast("/")
    ) : ScriptEditorMode()
}

data class ScriptEditorUiState(
    val title: String = "",
    val mode: ScriptEditorMode? = null,
    val isLoading: Boolean = false,
    val isModified: Boolean = false,
    val syntaxError: String? = null,
    val lastSavedContent: String = "",
    val canRun: Boolean = false
)

sealed class ScriptEditorEffect {
    data class InitContent(val content: String) : ScriptEditorEffect()
    data class ReloadContent(val content: String) : ScriptEditorEffect()
    data class FileConflict(val diskContent: String) : ScriptEditorEffect()
    data class ShowToast(val message: String) : ScriptEditorEffect()
    data class ShowToastRes(val resId: Int) : ScriptEditorEffect()
    data class LaunchApp(val packageName: String) : ScriptEditorEffect()
    data class ShareFile(val file: File) : ScriptEditorEffect()
    object SaveSuccess : ScriptEditorEffect()
}

class ScriptEditorViewModel(
    private val scriptRepository: ScriptRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val syntaxValidator: (String) -> String? = { code ->
        try {
            val c = "=".repeat(99)
            val err = JsePlatform.standardGlobals()
                .load("_,err = load([$c[$code]$c]);return err")
                .call()
            if (err == null || err.toString() == "nil") null else err.toString()
        } catch (t: Throwable) {
            t.message ?: "Syntax error"
        }
    }
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScriptEditorUiState())
    val uiState: StateFlow<ScriptEditorUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<ScriptEditorEffect>(extraBufferCapacity = 64)
    val effect: SharedFlow<ScriptEditorEffect> = _effect.asSharedFlow()

    private var currentMode: ScriptEditorMode? = null
    private var lastSavedContent: String = ""
    private var currentEditorContent: String = ""
    private var isInitialized = false
    private var fileObserverJob: Job? = null
    private var syntaxValidationJob: Job? = null

    fun init(mode: ScriptEditorMode) {
        if (isInitialized && currentMode == mode) return
        currentMode = mode
        isInitialized = true

        val canRun = mode is ScriptEditorMode.AppSpecific || mode is ScriptEditorMode.ProjectScript
        _uiState.update {
            it.copy(
                title = mode.title,
                mode = mode,
                isLoading = true,
                canRun = canRun
            )
        }

        observeFileEvents(mode.relativePath)

        viewModelScope.launch(ioDispatcher) {
            val content = scriptRepository.readScript(mode.relativePath)
            lastSavedContent = content
            currentEditorContent = content
            _uiState.update {
                it.copy(
                    isLoading = false,
                    lastSavedContent = content,
                    isModified = false
                )
            }
            _effect.emit(ScriptEditorEffect.InitContent(content))
            if (content.isNotEmpty()) {
                validateSyntaxInternal(content)
            }
        }
    }

    private fun observeFileEvents(expectedPath: String) {
        fileObserverJob?.cancel()
        fileObserverJob = viewModelScope.launch {
            WorkspaceEventManager.fileEvents.collect { changedPath ->
                val normChanged = changedPath.trimStart('/')
                val normExpected = expectedPath.trimStart('/')
                if (normChanged == normExpected) {
                    checkExternalChangeInternal(currentContent = null, quiet = false)
                }
            }
        }
    }

    fun isContentModified(currentText: String): Boolean {
        return currentText != lastSavedContent
    }

    fun onContentChanged(newContent: String) {
        currentEditorContent = newContent
        val modified = newContent != lastSavedContent
        if (_uiState.value.isModified != modified) {
            _uiState.update { it.copy(isModified = modified) }
        }
        validateSyntaxDebounced(newContent)
    }

    fun validateSyntaxDebounced(code: String, delayMs: Long = 300L) {
        syntaxValidationJob?.cancel()
        syntaxValidationJob = viewModelScope.launch(ioDispatcher) {
            delay(delayMs)
            validateSyntaxInternal(code)
        }
    }

    fun validateSyntax(code: String) {
        syntaxValidationJob?.cancel()
        viewModelScope.launch(ioDispatcher) {
            validateSyntaxInternal(code)
        }
    }

    private fun validateSyntaxInternal(code: String) {
        val errString = try {
            syntaxValidator(code)
        } catch (t: Throwable) {
            t.message ?: "Syntax error"
        }
        _uiState.update { it.copy(syntaxError = errString) }
    }

    fun save(content: String, showToast: Boolean = true) {
        val mode = currentMode ?: return
        viewModelScope.launch(ioDispatcher) {
            val previousSaved = lastSavedContent
            lastSavedContent = content
            currentEditorContent = content

            val success = withContext(NonCancellable) {
                scriptRepository.saveScript(mode.relativePath, content)
            }

            if (success) {
                _uiState.update {
                    it.copy(
                        lastSavedContent = content,
                        isModified = false
                    )
                }
                if (showToast) {
                    _effect.emit(ScriptEditorEffect.SaveSuccess)
                }
            } else {
                lastSavedContent = previousSaved
                _uiState.update { it.copy(isModified = currentEditorContent != previousSaved) }
                _effect.emit(ScriptEditorEffect.ShowToast("保存失败"))
            }
        }
    }

    fun forceSave(content: String) {
        save(content, showToast = true)
    }

    fun checkExternalChange(currentEditorContent: String, quiet: Boolean = false) {
        this.currentEditorContent = currentEditorContent
        viewModelScope.launch(ioDispatcher) {
            checkExternalChangeInternal(currentEditorContent, quiet)
        }
    }

    private suspend fun checkExternalChangeInternal(currentContent: String?, quiet: Boolean) {
        val mode = currentMode ?: return
        val diskText = scriptRepository.readScript(mode.relativePath)
        if (diskText == lastSavedContent) return

        val textToCompare = currentContent ?: currentEditorContent
        val modified = textToCompare != lastSavedContent

        if (!modified) {
            lastSavedContent = diskText
            currentEditorContent = diskText
            _uiState.update {
                it.copy(
                    lastSavedContent = diskText,
                    isModified = false
                )
            }
            _effect.emit(ScriptEditorEffect.ReloadContent(diskText))
            validateSyntaxInternal(diskText)
            if (!quiet) {
                _effect.emit(ScriptEditorEffect.ShowToastRes(R.string.msg_file_reloaded_from_disk))
            }
        } else {
            _effect.emit(ScriptEditorEffect.FileConflict(diskText))
        }
    }

    fun applyDiskContent(diskContent: String, showToast: Boolean = true) {
        lastSavedContent = diskContent
        currentEditorContent = diskContent
        _uiState.update {
            it.copy(
                lastSavedContent = diskContent,
                isModified = false
            )
        }
        viewModelScope.launch {
            _effect.emit(ScriptEditorEffect.ReloadContent(diskContent))
            validateSyntax(diskContent)
            if (showToast) {
                _effect.emit(ScriptEditorEffect.ShowToastRes(R.string.msg_file_reloaded_from_disk))
            }
        }
    }

    fun runScript(currentContent: String) {
        val mode = currentMode ?: return
        viewModelScope.launch(ioDispatcher) {
            if (currentContent != lastSavedContent) {
                lastSavedContent = currentContent
                currentEditorContent = currentContent
                withContext(NonCancellable) {
                    scriptRepository.saveScript(mode.relativePath, currentContent)
                }
                _uiState.update { it.copy(lastSavedContent = currentContent, isModified = false) }
            }

            when (mode) {
                is ScriptEditorMode.AppSpecific -> {
                    scriptRepository.forceStopApp(mode.packageName)
                    _effect.emit(ScriptEditorEffect.LaunchApp(mode.packageName))
                }
                is ScriptEditorMode.ProjectScript -> {
                    // Project script run support
                }
                is ScriptEditorMode.Global -> {
                    _effect.emit(ScriptEditorEffect.ShowToastRes(R.string.save_ok))
                }
                else -> {}
            }
        }
    }

    fun prepareShare(cacheDir: File, author: String) {
        val mode = currentMode as? ScriptEditorMode.AppSpecific ?: return
        viewModelScope.launch(ioDispatcher) {
            val file = scriptRepository.prepareShareScript(
                cacheDir = cacheDir,
                packageName = mode.packageName,
                scriptName = mode.scriptName,
                description = mode.scriptDescription,
                author = author
            )
            if (file != null) {
                _effect.emit(ScriptEditorEffect.ShareFile(file))
            } else {
                _effect.emit(ScriptEditorEffect.ShowToast("文件导出失败"))
            }
        }
    }

    companion object {
        fun provideFactory(
            scriptRepository: ScriptRepository = AppContainer.instance.scriptRepository,
            ioDispatcher: CoroutineDispatcher = Dispatchers.IO
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ScriptEditorViewModel(scriptRepository, ioDispatcher) as T
            }
        }
    }
}
