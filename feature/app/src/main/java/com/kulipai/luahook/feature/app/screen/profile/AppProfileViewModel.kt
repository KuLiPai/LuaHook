package com.kulipai.luahook.feature.app.screen.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kulipai.luahook.core.data.repository.AppsRepository
import com.kulipai.luahook.core.data.repository.AppsRepositoryState
import com.kulipai.luahook.core.model.AppInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AppProfileUiState(
    val appInfo: AppInfo? = null,
    val sameUidApps: List<AppInfo> = emptyList(),
    val isLoading: Boolean = true,
    val error: Throwable? = null,
)

class AppProfileViewModel(
    private val appsRepository: AppsRepository,
) : ViewModel() {

    private data class AppProfileTarget(
        val uid: Int,
        val packageName: String,
    )

    private val _uiState = MutableStateFlow(AppProfileUiState())
    val uiState: StateFlow<AppProfileUiState> = _uiState.asStateFlow()

    private var currentTarget: AppProfileTarget? = null

    init {
        viewModelScope.launch {
            appsRepository.state.collect { repositoryState ->
                publishState(repositoryState, isLoading = false)
            }
        }
    }

    fun load(uid: Int, packageName: String) {
        currentTarget = AppProfileTarget(uid, packageName)

        val currentState = appsRepository.state.value
        publishState(currentState, isLoading = !currentState.isLoaded)

        if (!currentState.isLoaded) {
            viewModelScope.launch {
                val result = appsRepository.loadApps()
                if (result.isFailure) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.exceptionOrNull(),
                    )
                }
            }
        }
    }

    private fun publishState(
        repositoryState: AppsRepositoryState,
        isLoading: Boolean,
    ) {
        val target = currentTarget ?: return
        val sameUidApps = repositoryState.apps.filter { appInfo -> appInfo.uid == target.uid }
        val appInfo = sameUidApps.find { info -> info.packageName == target.packageName }

        _uiState.value = AppProfileUiState(
            appInfo = appInfo,
            sameUidApps = sameUidApps,
            isLoading = isLoading,
            error = repositoryState.error,
        )
    }
}
