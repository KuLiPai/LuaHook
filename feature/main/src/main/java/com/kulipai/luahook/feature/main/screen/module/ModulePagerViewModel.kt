package com.kulipai.luahook.feature.main.screen.module

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kulipai.luahook.core.common.HanziToPinyin
import com.kulipai.luahook.core.ui.component.searchstatus.SearchStatus
import com.kulipai.luahook.core.model.DemoModule
import com.kulipai.luahook.core.model.DemoContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ModulePagerViewModel(
    private val application: Application,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ModulePagerUiState())
    val uiState: StateFlow<ModulePagerUiState> = _uiState.asStateFlow()

    var isNeedRefresh = false
        private set

    fun markNeedRefresh() {
        isNeedRefresh = true
    }

    fun updateSearchStatus(status: SearchStatus) {
        _uiState.update { it.copy(searchStatus = status) }
    }

    suspend fun updateSearchText(text: String) {
        _uiState.update {
            it.copy(searchStatus = it.searchStatus.copy(searchText = text))
        }
        syncSearchResults(text)
    }

    private suspend fun syncSearchResults(text: String) {
        if (text.isEmpty()) {
            _uiState.update {
                it.copy(
                    searchStatus = it.searchStatus.copy(resultStatus = SearchStatus.ResultStatus.DEFAULT),
                    searchResults = emptyList(),
                )
            }
            return
        }

        _uiState.update {
            it.copy(searchStatus = it.searchStatus.copy(resultStatus = SearchStatus.ResultStatus.LOAD))
        }

        val result = withContext(Dispatchers.IO) {
            _uiState.value.moduleList.filter {
                it.name.contains(text, true) ||
                        it.description.contains(text, true) ||
                        it.author.contains(text, true) ||
                        it.id.contains(text, true) ||
                        HanziToPinyin.getInstance().toPinyinString(it.name).contains(text, true)
            }
        }

        _uiState.update {
            it.copy(
                searchResults = result,
                searchStatus = it.searchStatus.copy(
                    resultStatus = if (result.isEmpty()) {
                        SearchStatus.ResultStatus.EMPTY
                    } else {
                        SearchStatus.ResultStatus.SHOW
                    }
                )
            )
        }
    }

    fun loadModuleList(force: Boolean = false): Job {
        return viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null) }

            // TODO: Replace with actual module loading from data layer
            val modules = withContext(Dispatchers.IO) {
                DemoContent.modules
            }

            _uiState.update {
                it.copy(
                    moduleList = modules,
                    isRefreshing = false,
                    error = null
                )
            }

            syncSearchResults(_uiState.value.searchStatus.searchText)
            isNeedRefresh = false
        }
    }

    fun toggleModule(moduleId: String, enabled: Boolean) {
        _uiState.update { state ->
            val updatedList = state.moduleList.map { module ->
                if (module.id == moduleId) module.copy(enabled = enabled) else module
            }
            state.copy(moduleList = updatedList)
        }
    }

    fun deleteModule(moduleId: String) {
        _uiState.update { state ->
            state.copy(moduleList = state.moduleList.filter { it.id != moduleId })
        }
    }
}
