package com.kulipai.luahook.feature.main.screen.apps

import android.app.Application
import android.content.pm.ApplicationInfo
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kulipai.luahook.core.common.HanziToPinyin
import com.kulipai.luahook.core.data.repository.AppsRepository
import com.kulipai.luahook.core.model.AppInfo
import com.kulipai.luahook.core.ui.component.searchstatus.SearchStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class AppsPagerViewModel(
    private val application: Application,
    private val appsRepository: AppsRepository,
) : ViewModel() {

    companion object {
        private const val TAG = "AppsPagerViewModel"
    }

    private val _uiState = MutableStateFlow(AppsPagerUiState())
    val uiState: StateFlow<AppsPagerUiState> = _uiState.asStateFlow()

    private val refreshMutex = Mutex()
    var isNeedRefresh = false
        private set

    fun markNeedRefresh() {
        isNeedRefresh = true
    }

    fun setShowSystemApps(show: Boolean): Job {
        _uiState.update { it.copy(showSystemApps = show) }
        return viewModelScope.launch {
            rebuildVisibleApps()
        }
    }

    fun setShowOnlyPrimaryUserApps(show: Boolean): Job {
        _uiState.update { it.copy(showOnlyPrimaryUserApps = show) }
        return viewModelScope.launch {
            rebuildVisibleApps()
        }
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

    private fun filterAndSort(list: List<AppInfo>): List<AppInfo> {
        val currentState = _uiState.value

        return list.filter {
            if (it.packageName == application.packageName) return@filter false
            val userFilter = !currentState.showOnlyPrimaryUserApps || it.uid / 100000 == 0
            val isSystemApp = it.packageInfo.applicationInfo!!.flags.and(ApplicationInfo.FLAG_SYSTEM) != 0
            val typeFilter = it.uid == 2000 || currentState.showSystemApps || !isSystemApp
            userFilter && typeFilter
        }
    }

    private fun buildGroups(apps: List<AppInfo>): List<GroupedApps> {
        val comparator = compareBy<AppInfo> {
            when {
                else -> 2
            }
        }.thenBy { it.label.lowercase() }

        val groups = apps.groupBy { it.uid }.map { (uid, list) ->
            val sorted = list.sortedWith(comparator)
            val primary = pickPrimary(sorted)

            GroupedApps(
                uid = uid,
                apps = sorted,
                primary = primary,
            )
        }

        return groups.sortedWith(Comparator { a, b ->
            fun rank(groupedApps: GroupedApps): Int = when {
                groupedApps.apps.size > 1 -> 2
                else -> 3
            }

            val leftRank = rank(a)
            val rightRank = rank(b)
            if (leftRank != rightRank) return@Comparator leftRank - rightRank

            when (leftRank) {
                2 -> a.uid.compareTo(b.uid)
                else -> a.primary.label.lowercase().compareTo(b.primary.label.lowercase())
            }
        })
    }

    private suspend fun rebuildVisibleApps(
        sourceApps: List<AppInfo> = appsRepository.state.value.apps,
        userIds: List<Int> = appsRepository.state.value.userIds,
    ) {
        val (filtered, grouped) = withContext(Dispatchers.IO) {
            val list = filterAndSort(sourceApps)
            list to buildGroups(list)
        }

        _uiState.update {
            it.copy(
                appList = filtered,
                groupedApps = grouped,
                userIds = userIds,
            )
        }

        syncSearchResults(_uiState.value.searchStatus.searchText)
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
            _uiState.value.appList.filter {
                it.label.contains(text, true) ||
                    it.packageName.contains(text, true) ||
                    HanziToPinyin.getInstance().toPinyinString(it.label).contains(text, true)
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

    suspend fun fetchAppList() {
        refreshMutex.withLock {
            _uiState.update { it.copy(isRefreshing = true, error = null) }

            val result = appsRepository.loadApps(force = true)
            if (result.isFailure) {
                val error = result.exceptionOrNull()
                if (error != null) {
                    Log.e(TAG, "fetchAppList failed", error)
                }
                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        error = error,
                    )
                }
                return@withLock
            }

            val repositoryState = result.getOrThrow()
            rebuildVisibleApps(repositoryState.apps, repositoryState.userIds)

            _uiState.update { it.copy(isRefreshing = false, error = null) }
            isNeedRefresh = false
        }
    }

    private suspend fun refreshAppList(resort: Boolean = true) {
        if (!appsRepository.state.value.isLoaded) {
            fetchAppList()
            return
        }

        refreshMutex.withLock {
            val currentApps = appsRepository.state.value.apps
            if (currentApps.isEmpty()) return@withLock

            _uiState.update { it.copy(isRefreshing = true, error = null) }

            val result = appsRepository.refreshProfiles()
            if (result.isFailure) {
                val error = result.exceptionOrNull()
                if (error != null) {
                    Log.e(TAG, "refreshAppList failed", error)
                }
                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        error = error,
                    )
                }
                return@withLock
            }

            val repositoryState = result.getOrThrow()
            val updatedApps = repositoryState.apps
            if (resort) {
                rebuildVisibleApps(updatedApps, repositoryState.userIds)
            } else {
                val updatedMap = updatedApps.associateBy { it.packageName }
                val currentFiltered = _uiState.value.appList.map { updatedMap[it.packageName] ?: it }
                val currentGroups = _uiState.value.groupedApps.map { group ->
                    val newApps = group.apps.map { updatedMap[it.packageName] ?: it }
                    val primary = pickPrimary(newApps)
                    group.copy(
                        apps = newApps,
                        primary = primary,
                    )
                }

                _uiState.update {
                    it.copy(
                        appList = currentFiltered,
                        groupedApps = currentGroups,
                        userIds = repositoryState.userIds,
                    )
                }
                syncSearchResults(_uiState.value.searchStatus.searchText)
            }

            _uiState.update { it.copy(isRefreshing = false, error = null) }
            isNeedRefresh = false
        }
    }

    fun loadAppList(force: Boolean = false, resort: Boolean = true): Job {
        return viewModelScope.launch {
            if (force || !appsRepository.state.value.isLoaded || _uiState.value.appList.isEmpty()) {
                fetchAppList()
            } else {
                refreshAppList(resort)
            }
        }
    }
}

private val PREFERRED_PKG_BY_SUID = mapOf(
    "android.uid.system" to "android",
    "android.uid.phone" to "com.android.phone",
    "android.uid.bluetooth" to "com.android.bluetooth",
    "android.uid.nfc" to "com.android.nfc",
)

fun pickPrimary(apps: List<AppInfo>): AppInfo {
    if (apps.isEmpty()) throw IllegalArgumentException("apps must not be empty")

    val labeled = apps.filter { it.packageInfo.sharedUserLabel != 0 }
    if (labeled.isNotEmpty()) {
        return labeled.minWith(compareBy({ it.packageName.length }, { it.packageName }))
    }

    val groupedBySharedUserId = apps.groupBy { it.packageInfo.sharedUserId ?: "" }
        .filterKeys { it.startsWith("android.uid.") }
    if (groupedBySharedUserId.isEmpty()) return apps.first()

    val sharedUserId = groupedBySharedUserId.keys.minOf { it }
    val group = groupedBySharedUserId[sharedUserId] ?: apps
    val preferredPackageName = PREFERRED_PKG_BY_SUID[sharedUserId]
    preferredPackageName?.let { packageName ->
        group.firstOrNull { it.packageName == packageName }?.let { return it }
    }

    return group.minWith(compareBy({ it.packageName.length }, { it.packageName }))
}
