package com.kulipai.luahook.core.project

import android.content.Context
import android.net.Uri
import com.kulipai.luahook.app.di.AppContainer
import com.kulipai.luahook.core.file.WorkspaceStorageImpl
import com.kulipai.luahook.core.shell.StandaloneShellExecutor
import com.kulipai.luahook.data.repository.ProjectRepository
import com.kulipai.luahook.data.repository.ProjectRepositoryImpl
import kotlinx.coroutines.runBlocking

data class Project(
    val name: String,
    val isEnabled: Boolean,
    val icon: String, // Path or unicode
    val author: String = "",
    val description: String = "",
    val scope: List<String> = emptyList(), // "all" or list of packages
    val launcher: String = "" // Package to launch for testing
)

/**
 * ProjectManager (绞杀者模式兼容桥梁)
 * 仅作为旧代码与兼容适配层保留，新代码请使用 [ProjectRepository]。
 */
@Deprecated(
    message = "Use ProjectRepository via AppContainer.instance.projectRepository instead",
    replaceWith = ReplaceWith("AppContainer.instance.projectRepository")
)
object ProjectManager {

    const val INFO_JSON = "/Project/info.json"

    private var customRepository: ProjectRepository? = null

    // 在目标宿主应用或无 AppContainer 时的 Standalone 兜底
    private val defaultRepository: ProjectRepository by lazy {
        val container = AppContainer.instanceOrNull
        if (container != null) {
            ProjectRepositoryImpl(container.workspaceStorage, container.shellExecutor)
        } else {
            val standaloneExecutor = StandaloneShellExecutor()
            ProjectRepositoryImpl(
                WorkspaceStorageImpl(standaloneExecutor),
                standaloneExecutor
            )
        }
    }

    fun bindRepository(repository: ProjectRepository) {
        customRepository = repository
    }

    private fun getRepository(): ProjectRepository {
        return customRepository
            ?: AppContainer.instanceOrNull?.projectRepository
            ?: defaultRepository
    }

    fun getProjects(): List<Project> = runBlocking {
        getRepository().getProjects()
    }

    fun createProject(
        name: String,
        description: String,
        author: String,
        iconPath: String?,
        iconUnicode: String?,
        scope: List<String>,
        launcher: String
    ): Boolean = runBlocking {
        getRepository().createProject(name, description, author, iconPath, iconUnicode, scope, launcher)
    }

    fun setProjectEnabled(name: String, enabled: Boolean) = runBlocking {
        getRepository().setProjectEnabled(name, enabled)
    }

    fun deleteProject(name: String) = runBlocking {
        getRepository().deleteProject(name)
    }

    fun importProject(context: Context, uri: Uri): Boolean = runBlocking {
        getRepository().importProject(context, uri)
    }

    fun exportProject(context: Context, name: String, uri: Uri): Boolean = runBlocking {
        getRepository().exportProject(context, name, uri)
    }
}
