package com.kulipai.luahook.feature.project.pack

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kulipai.luahook.app.di.AppContainer
import com.kulipai.luahook.data.repository.ProjectRepository
import com.kulipai.luahook.feature.project.pack.engine.ApkPacker
import com.kulipai.luahook.feature.project.pack.engine.PackRequest
import com.kulipai.luahook.feature.project.pack.engine.PackStage
import com.kulipai.luahook.feature.project.pack.engine.signer.SignConfig
import com.kulipai.luahook.feature.project.pack.engine.slimmer.ArscBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

class ProjectPackViewModel(
    private val projectRepository: ProjectRepository = AppContainer.instance.projectRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProjectPackUiState())
    val uiState: StateFlow<ProjectPackUiState> = _uiState.asStateFlow()

    fun loadProject(projectName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingProject = true) }
            try {
                val projects = projectRepository.getProjects()
                val found = projects.find { it.name == projectName }
                val suggestedPkg = PackageNameValidator.suggestPackageName(projectName)
                _uiState.update {
                    it.copy(
                        isLoadingProject = false,
                        project = found,
                        suggestedPackageName = suggestedPkg
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingProject = false,
                        errorMessage = e.message ?: "Failed to load project"
                    )
                }
            }
        }
    }

    fun startPacking(
        context: Context,
        projectName: String,
        appName: String,
        packageName: String,
        versionName: String,
        versionCode: Int,
        description: String,
        author: String,
        scopes: List<String>,
        showLauncherIcon: Boolean,
        customIconPngBytes: ByteArray?,
        abiFilter: Set<String>,
        signConfig: SignConfig
    ) {
        if (_uiState.value.isPacking) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isPacking = true,
                    packStage = PackStage.PREPARE,
                    packProgress = 5,
                    packResult = null,
                    errorMessage = null
                )
            }

            try {
                val result = withContext(Dispatchers.IO) {
                    val projectFiles = projectRepository.collectProjectFiles(context, projectName)
                    val baseApkFile = File(context.applicationInfo.sourceDir)

                    val workDir = File(context.cacheDir, "apk_pack_work")
                    if (workDir.exists()) workDir.deleteRecursively()
                    workDir.mkdirs()

                    val outputDir = File(context.externalCacheDir ?: context.cacheDir, "packed_apks")
                    outputDir.mkdirs()
                    val safeFileName = "${appName.replace(Regex("[^a-zA-Z0-9_\\-\\u4e00-\\u9fa5]"), "_")}_v${versionName}.apk"
                    val outputApkFile = File(outputDir, safeFileName)

                    val finalIconBytes = customIconPngBytes ?: renderDefaultLauncherIconPng(context)
                    val resolvedAuthor = author.ifBlank { _uiState.value.project?.author ?: "" }

                    val request = PackRequest(
                        projectName = projectName,
                        appName = appName,
                        packageName = packageName,
                        versionName = versionName,
                        versionCode = versionCode,
                        description = description,
                        author = resolvedAuthor,
                        scopes = scopes,
                        showLauncherIcon = showLauncherIcon,
                        customIconPngBytes = finalIconBytes,
                        customIconResId = ArscBuilder.ICON_RESOURCE_ID,
                        customIconEntryPath = ArscBuilder.ICON_ENTRY_PATH,
                        abiFilter = abiFilter,
                        signConfig = signConfig,
                        baseApkFile = baseApkFile,
                        projectFiles = projectFiles,
                        workDir = workDir,
                        outputApkFile = outputApkFile
                    )

                    ApkPacker.pack(request) { stage, percent ->
                        _uiState.update { state ->
                            state.copy(packStage = stage, packProgress = percent)
                        }
                    }
                }

                _uiState.update {
                    it.copy(
                        isPacking = false,
                        packStage = PackStage.COMPLETE,
                        packProgress = 100,
                        packResult = result
                    )
                }
            } catch (e: Throwable) {
                _uiState.update {
                    it.copy(
                        isPacking = false,
                        errorMessage = e.message ?: e.toString()
                    )
                }
            }
        }
    }

    private fun renderDefaultLauncherIconPng(context: Context): ByteArray? {
        return try {
            val drawable = context.applicationInfo.loadIcon(context.packageManager)
            val size = 192
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, size, size)
            drawable.draw(canvas)
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
            baos.toByteArray()
        } catch (_: Throwable) {
            null
        }
    }

    fun clearPackResult() {
        _uiState.update { it.copy(packResult = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
