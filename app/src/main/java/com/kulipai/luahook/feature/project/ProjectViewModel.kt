package com.kulipai.luahook.feature.project

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kulipai.luahook.app.di.AppContainer
import com.kulipai.luahook.core.file.WorkspaceEventManager
import com.kulipai.luahook.core.file.WorkspaceStorage
import com.kulipai.luahook.core.file.WorkspaceStorageImpl
import com.kulipai.luahook.core.project.Project
import com.kulipai.luahook.core.shell.StandaloneShellExecutor
import com.kulipai.luahook.data.repository.ProjectRepository
import com.kulipai.luahook.data.repository.ProjectRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProjectUiState(
    val projects: List<Project> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class ProjectViewModel(
    private val projectRepository: ProjectRepository
) : ViewModel() {

    // 默认无参构造器供 ViewModelProvider / by viewModels() 实例化
    constructor() : this(
        resolveRepository()
    )

    private val _uiState = MutableStateFlow(ProjectUiState(isLoading = true))
    val uiState: StateFlow<ProjectUiState> = _uiState.asStateFlow()

    init {
        loadProjects()
        observeFileEvents()
    }

    private fun observeFileEvents() {
        viewModelScope.launch {
            WorkspaceEventManager.fileEvents.collect { relativePath ->
                if (relativePath == ProjectRepository.INFO_JSON || relativePath.startsWith("${WorkspaceStorage.Project}/")) {
                    loadProjects()
                }
            }
        }
    }

    fun loadProjects() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val projects = projectRepository.getProjects()
                _uiState.update { it.copy(projects = projects, isLoading = false, errorMessage = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Failed to load projects") }
            }
        }
    }

    fun setProjectEnabled(name: String, enabled: Boolean) {
        viewModelScope.launch {
            try {
                projectRepository.setProjectEnabled(name, enabled)
                _uiState.update { state ->
                    state.copy(
                        projects = state.projects.map {
                            if (it.name == name) it.copy(isEnabled = enabled) else it
                        }
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
                loadProjects()
            }
        }
    }

    fun deleteProject(name: String, onResult: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            try {
                projectRepository.deleteProject(name)
                loadProjects()
                onResult?.invoke(true)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
                onResult?.invoke(false)
            }
        }
    }

    fun importProject(context: Context, uri: Uri, onResult: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val success = projectRepository.importProject(context, uri)
                if (success) {
                    loadProjects()
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
                onResult?.invoke(success)
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
                onResult?.invoke(false)
            }
        }
    }

    fun exportProject(context: Context, name: String, uri: Uri, onResult: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            try {
                val success = projectRepository.exportProject(context, name, uri)
                onResult?.invoke(success)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
                onResult?.invoke(false)
            }
        }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    companion object {
        private fun resolveRepository(): ProjectRepository {
            val container = AppContainer.instanceOrNull
            return if (container != null) {
                container.projectRepository
            } else {
                val standalone = StandaloneShellExecutor()
                ProjectRepositoryImpl(WorkspaceStorageImpl(standalone), standalone)
            }
        }

        fun provideFactory(
            repository: ProjectRepository? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ProjectViewModel(repository ?: resolveRepository()) as T
            }
        }
    }
}
