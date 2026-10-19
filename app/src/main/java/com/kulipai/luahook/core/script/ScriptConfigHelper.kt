package com.kulipai.luahook.core.script

import com.kulipai.luahook.app.di.AppContainer
import com.kulipai.luahook.core.file.WorkspaceStorageImpl
import com.kulipai.luahook.core.shell.StandaloneShellExecutor
import com.kulipai.luahook.data.repository.ScriptRepository
import com.kulipai.luahook.data.repository.ScriptRepositoryImpl
import kotlinx.coroutines.runBlocking

/**
 * ScriptConfigHelper (绞杀者模式兼容桥梁)
 * 仅作为旧代码与 Hook 注入环境的兼容适配层保留，新代码请使用 [ScriptRepository]。
 */
@Deprecated(
    message = "Use ScriptRepository via AppContainer.instance.scriptRepository instead",
    replaceWith = ReplaceWith("AppContainer.instance.scriptRepository")
)
object ScriptConfigHelper {

    private var customRepository: ScriptRepository? = null

    // 在目标宿主应用（如 Xposed Hook 注入场景）或无 AppContainer 时的 Standalone 兜底
    private val defaultRepository: ScriptRepository by lazy {
        val container = AppContainer.instanceOrNull
        if (container != null) {
            container.scriptRepository
        } else {
            val standaloneExecutor = StandaloneShellExecutor()
            ScriptRepositoryImpl(
                WorkspaceStorageImpl(standaloneExecutor),
                standaloneExecutor
            )
        }
    }

    fun bindRepository(repository: ScriptRepository) {
        customRepository = repository
    }

    private fun getRepository(): ScriptRepository {
        return customRepository
            ?: AppContainer.instanceOrNull?.scriptRepository
            ?: defaultRepository
    }

    fun readConf(packageName: String): MutableList<MutableMap.MutableEntry<String, Any?>> = runBlocking {
        getRepository().readConf(packageName)
    }

    fun writeScriptConfig(packageName: String, name: String, description: String): Unit = runBlocking {
        getRepository().writeScriptConfig(packageName, name, description)
    }
}
