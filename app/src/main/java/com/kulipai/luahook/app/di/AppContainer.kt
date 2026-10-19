package com.kulipai.luahook.app.di

import android.content.Context
import com.kulipai.luahook.app.MyApplication
import com.kulipai.luahook.core.file.WorkspaceFileManager
import com.kulipai.luahook.core.file.WorkspaceStorage
import com.kulipai.luahook.core.file.WorkspaceStorageImpl
import com.kulipai.luahook.core.project.ProjectManager
import com.kulipai.luahook.core.shell.ShellExecutor
import com.kulipai.luahook.core.shell.ShellExecutorImpl
import com.kulipai.luahook.core.shell.ShellManager
import com.kulipai.luahook.core.shell.ShellMode
import com.kulipai.luahook.core.script.ScriptConfigHelper
import com.kulipai.luahook.data.repository.InstalledAppsRepository
import com.kulipai.luahook.data.repository.InstalledAppsRepositoryImpl
import com.kulipai.luahook.data.repository.LogcatRepository
import com.kulipai.luahook.data.repository.LogcatRepositoryImpl
import com.kulipai.luahook.data.repository.ProjectRepository
import com.kulipai.luahook.data.repository.ProjectRepositoryImpl
import com.kulipai.luahook.data.repository.ScriptRepository
import com.kulipai.luahook.data.repository.ScriptRepositoryImpl
import com.kulipai.luahook.feature.mcp.McpManager
import com.kulipai.luahook.feature.logcat.LogcatDelegate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppContainer(private val context: Context) {

    companion object {
        val instance: AppContainer
            get() = MyApplication.container

        val instanceOrNull: AppContainer?
            get() = MyApplication.containerOrNull
    }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val shellExecutor: ShellExecutor by lazy {
        ShellExecutorImpl(context, applicationScope)
    }

    val workspaceStorage: WorkspaceStorage by lazy {
        WorkspaceStorageImpl(shellExecutor)
    }

    val installedAppsRepository: InstalledAppsRepository by lazy {
        InstalledAppsRepositoryImpl(context)
    }

    val logcatRepository: LogcatRepository by lazy {
        LogcatRepositoryImpl(shellExecutor)
    }

    val projectRepository: ProjectRepository by lazy {
        ProjectRepositoryImpl(workspaceStorage, shellExecutor)
    }

    val scriptRepository: ScriptRepository by lazy {
        ScriptRepositoryImpl(workspaceStorage, shellExecutor)
    }

    fun initialize() {
        // 绑定遗留单例的静态委托对象
        WorkspaceFileManager.bindStorage(workspaceStorage)
        ShellManager.bindExecutor(shellExecutor)
        LogcatDelegate.bindRepository(logcatRepository)
        ProjectManager.bindRepository(projectRepository)
        ScriptConfigHelper.bindRepository(scriptRepository)

        // 1. 同步 Legacy ShellManager LiveData 状态
        syncLegacyShellManager()

        // 2. 编排工作区就绪与 MCP 服务启动（在 IO 线程执行以防阻塞主线程）
        orchestrateWorkspaceAndMcp()

        // 3. 启动 Shell 检测流程
        shellExecutor.init()
    }

    private fun syncLegacyShellManager() {
        applicationScope.launch {
            shellExecutor.mode.collectLatest { mode ->
                val legacyMode = when (mode) {
                    ShellMode.ROOT -> ShellManager.Mode.ROOT
                    ShellMode.SHIZUKU -> ShellManager.Mode.SHIZUKU
                    ShellMode.SHIZUKU_FALLBACK -> ShellManager.Mode.SHIZUKU_FALLBACK
                    ShellMode.NONE -> ShellManager.Mode.NONE
                }
                ShellManager.mode.postValue(legacyMode)
            }
        }
    }

    private fun orchestrateWorkspaceAndMcp() {
        applicationScope.launch {
            shellExecutor.mode.collectLatest { mode ->
                if (mode != ShellMode.NONE) {
                    // 当获取到 ROOT、SHIZUKU 或 SHIZUKU_FALLBACK 权限后，在 IO 线程按序初始化工作区并通知 MCP
                    withContext(Dispatchers.IO) {
                        workspaceStorage.init(context)
                        McpManager.onWorkspaceReady(context)
                    }
                }
            }
        }
    }
}
