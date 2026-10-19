package com.kulipai.luahook.core.file

import android.content.Context
import com.kulipai.luahook.app.di.AppContainer
import com.kulipai.luahook.core.shell.StandaloneShellExecutor

/**
 * /data/local/tmp/LuaHook/ 文件管理系统 (绞杀者模式兼容桥梁)
 * 仅作为旧代码与 UI 层的适配层保留，新代码请使用 [WorkspaceStorage]。
 */
@Deprecated(
    message = "Use AppContainer.instance.workspaceStorage instead",
    replaceWith = ReplaceWith("AppContainer.instance.workspaceStorage")
)
object WorkspaceFileManager {

    const val DIR: String = "/data/local/tmp/LuaHook"
    const val Project: String = "/Project"
    const val AppConf: String = "/AppConf"
    const val AppScript: String = "/AppScript"
    const val Plugin: String = "/Plugin"

    data class FileParameters(
        val name: String? = null,
        val descript: String? = null,
        val packageName: String? = null,
        val author: String? = null,
        val otherParams: Map<String, String> = emptyMap()
    )

    private var customStorage: WorkspaceStorage? = null

    // 在目标宿主应用（如 Xposed Hook 注入场景）无 AppContainer 时的 Standalone 兜底
    private val standaloneStorage by lazy {
        WorkspaceStorageImpl(StandaloneShellExecutor())
    }

    fun bindStorage(storage: WorkspaceStorage) {
        customStorage = storage
    }

    private fun getStorage(): WorkspaceStorage {
        return AppContainer.instanceOrNull?.workspaceStorage ?: customStorage ?: standaloneStorage
    }

    fun read(relativePath: String): String = getStorage().read(relativePath)

    fun write(file: String, content: String): Boolean = getStorage().write(file, content)

    fun writeMap(file: String, data: MutableMap<String, Any?>): Boolean = getStorage().writeMap(file, data)

    fun readMap(file: String): MutableMap<String, Any?> = getStorage().readMap(file)

    fun writeStringList(path: String, list: List<String>) = getStorage().writeStringList(path, list)

    fun readStringList(path: String): MutableList<String> = getStorage().readStringList(path)

    fun rm(file: String): Boolean = getStorage().rm(file)

    fun init(context: Context) = getStorage().init(context)

    fun directoryExists(path: String): Boolean = getStorage().directoryExists(path)

    fun ensureDirectoryExists(path: String): Boolean = getStorage().ensureDirectoryExists(path)

    fun ensureReadable(path: String) = getStorage().ensureReadable(path)

    fun parseParameters(fileContent: String): FileParameters? = getStorage().parseParameters(fileContent)
}