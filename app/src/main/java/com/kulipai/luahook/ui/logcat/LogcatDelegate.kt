package com.kulipai.luahook.ui.logcat

import com.kulipai.luahook.app.di.AppContainer
import com.kulipai.luahook.core.shell.StandaloneShellExecutor
import com.kulipai.luahook.data.repository.LogcatRepository
import com.kulipai.luahook.data.repository.LogcatRepositoryImpl

/**
 * 读取系统日志的工具类 (绞杀者模式兼容桥梁)
 * 仅作为旧代码与 UI 层的适配层保留，新代码请使用 [LogcatRepository]。
 */
@Deprecated(
    message = "Use AppContainer.instance.logcatRepository instead",
    replaceWith = ReplaceWith("AppContainer.instance.logcatRepository")
)
object LogcatDelegate {

    private var customRepository: LogcatRepository? = null

    private val defaultRepository: LogcatRepository by lazy {
        val executor = AppContainer.instanceOrNull?.shellExecutor ?: StandaloneShellExecutor()
        LogcatRepositoryImpl(executor)
    }

    fun bindRepository(repository: LogcatRepository) {
        customRepository = repository
    }

    private fun getRepository(): LogcatRepository {
        return AppContainer.instanceOrNull?.logcatRepository ?: customRepository ?: defaultRepository
    }

    suspend fun getSystemLogsByTagSince(tag: String, since: String? = null): List<String> {
        return getRepository().getSystemLogsByTagSince(tag, since)
    }

    suspend fun readSystemLogsByTagSince(tag: String, since: String? = null): Result<List<String>> {
        return getRepository().readSystemLogsByTagSince(tag, since)
    }

    fun getCurrentLogcatTimeFormat(): String {
        return getRepository().getCurrentLogcatTimeFormat()
    }
}
