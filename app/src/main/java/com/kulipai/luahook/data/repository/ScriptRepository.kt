package com.kulipai.luahook.data.repository

import com.kulipai.luahook.core.file.WorkspaceFileManager
import com.kulipai.luahook.core.file.WorkspaceStorage
import com.kulipai.luahook.core.shell.ShellExecutor
import com.kulipai.luahook.core.shell.ShellResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import java.io.File

data class AppScriptConfig(
    val name: String,
    val enabled: Boolean,
    val description: String = "",
    val version: String = "v1.0"
)

interface ScriptRepository {
    suspend fun readGlobalScript(): String
    suspend fun saveGlobalScript(content: String): Boolean

    suspend fun readConf(packageName: String): MutableList<MutableMap.MutableEntry<String, Any?>>
    suspend fun writeScriptConfig(packageName: String, name: String, description: String)
    suspend fun getAppScripts(packageName: String): List<AppScriptConfig>
    suspend fun readAppScript(packageName: String, name: String): String
    suspend fun saveAppScript(packageName: String, name: String, content: String): Boolean
    suspend fun createAppScript(packageName: String, name: String, description: String, content: String? = null): Boolean
    suspend fun deleteAppScript(packageName: String, name: String): Boolean
    suspend fun setAppScriptEnabled(packageName: String, name: String, enabled: Boolean): Boolean

    suspend fun readScript(relativePath: String): String
    suspend fun saveScript(relativePath: String, content: String): Boolean

    suspend fun parseParameters(content: String): WorkspaceFileManager.FileParameters?
    suspend fun forceStopApp(packageName: String): Boolean
    suspend fun restartApp(packageName: String): Boolean

    suspend fun prepareShareScript(
        cacheDir: File,
        packageName: String,
        scriptName: String,
        description: String,
        author: String
    ): File?
}

class ScriptRepositoryImpl(
    private val workspaceStorage: WorkspaceStorage,
    private val shellExecutor: ShellExecutor,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ScriptRepository {

    override suspend fun readGlobalScript(): String = withContext(ioDispatcher) {
        workspaceStorage.read("/global.lua")
    }

    override suspend fun saveGlobalScript(content: String): Boolean = withContext(ioDispatcher) {
        workspaceStorage.write("/global.lua", content)
    }

    override suspend fun readConf(packageName: String): MutableList<MutableMap.MutableEntry<String, Any?>> = withContext(ioDispatcher) {
        val path = "${WorkspaceFileManager.AppConf}/$packageName.txt"
        val list = workspaceStorage.readMap(path).entries.toMutableList()
        transformBooleanValuesToJsonArrayInMaps(list)
        list
    }

    override suspend fun writeScriptConfig(packageName: String, name: String, description: String): Unit = withContext(ioDispatcher) {
        val path = "${WorkspaceFileManager.AppConf}/$packageName.txt"
        val map = workspaceStorage.readMap(path)
        map[name] = JSONArray().put(true).put(description).put("v1.0")
        workspaceStorage.writeMap(path, map)
        workspaceStorage.ensureDirectoryExists("${WorkspaceFileManager.DIR}${WorkspaceFileManager.AppScript}/$packageName")
    }

    override suspend fun getAppScripts(packageName: String): List<AppScriptConfig> = withContext(ioDispatcher) {
        val confList = readConf(packageName)
        confList.mapNotNull { entry ->
            val name = entry.key
            when (val value = entry.value) {
                is JSONArray -> {
                    val enabled = value.optBoolean(0, true)
                    val desc = value.optString(1, "")
                    val ver = value.optString(2, "v1.0")
                    AppScriptConfig(name, enabled, desc, ver)
                }
                is Boolean -> {
                    AppScriptConfig(name, value, "", "v1.0")
                }
                is Array<*> -> {
                    val enabled = (value.getOrNull(0) as? Boolean) ?: true
                    val desc = (value.getOrNull(1) as? String) ?: ""
                    val ver = (value.getOrNull(2) as? String) ?: "v1.0"
                    AppScriptConfig(name, enabled, desc, ver)
                }
                else -> null
            }
        }
    }

    override suspend fun readAppScript(packageName: String, name: String): String = withContext(ioDispatcher) {
        val path = "${WorkspaceFileManager.AppScript}/$packageName/$name.lua"
        workspaceStorage.read(path)
    }

    override suspend fun saveAppScript(packageName: String, name: String, content: String): Boolean = withContext(ioDispatcher) {
        val path = "${WorkspaceFileManager.AppScript}/$packageName/$name.lua"
        workspaceStorage.write(path, content)
    }

    override suspend fun createAppScript(
        packageName: String,
        name: String,
        description: String,
        content: String?
    ): Boolean = withContext(ioDispatcher) {
        writeScriptConfig(packageName, name, description)
        val path = "${WorkspaceFileManager.AppScript}/$packageName/$name.lua"
        val initialText = content ?: "-- $name\n"
        workspaceStorage.write(path, initialText)
    }

    override suspend fun deleteAppScript(packageName: String, name: String): Boolean = withContext(ioDispatcher) {
        val scriptPath = "${WorkspaceFileManager.AppScript}/$packageName/$name.lua"
        workspaceStorage.rm(scriptPath)
        val confPath = "${WorkspaceFileManager.AppConf}/$packageName.txt"
        val map = workspaceStorage.readMap(confPath)
        map.remove(name)
        workspaceStorage.writeMap(confPath, map)
    }

    override suspend fun setAppScriptEnabled(packageName: String, name: String, enabled: Boolean): Boolean = withContext(ioDispatcher) {
        val confPath = "${WorkspaceFileManager.AppConf}/$packageName.txt"
        val map = workspaceStorage.readMap(confPath)
        val current = map[name]
        val description = when (current) {
            is JSONArray -> current.optString(1, "")
            is Array<*> -> (current.getOrNull(1) as? String) ?: ""
            else -> ""
        }
        val version = when (current) {
            is JSONArray -> current.optString(2, "v1.0")
            is Array<*> -> (current.getOrNull(2) as? String) ?: "v1.0"
            else -> "v1.0"
        }
        map[name] = JSONArray().put(enabled).put(description).put(version)
        workspaceStorage.writeMap(confPath, map)
    }

    override suspend fun readScript(relativePath: String): String = withContext(ioDispatcher) {
        workspaceStorage.read(relativePath)
    }

    override suspend fun saveScript(relativePath: String, content: String): Boolean = withContext(ioDispatcher) {
        workspaceStorage.write(relativePath, content)
    }

    override suspend fun parseParameters(content: String): WorkspaceFileManager.FileParameters? = withContext(ioDispatcher) {
        workspaceStorage.parseParameters(content)
    }

    override suspend fun forceStopApp(packageName: String): Boolean = withContext(ioDispatcher) {
        val result = shellExecutor.exec("am force-stop $packageName")
        result is ShellResult.Success
    }

    override suspend fun restartApp(packageName: String): Boolean = forceStopApp(packageName)

    override suspend fun prepareShareScript(
        cacheDir: File,
        packageName: String,
        scriptName: String,
        description: String,
        author: String
    ): File? = withContext(ioDispatcher) {
        try {
            val relPath = "${WorkspaceFileManager.AppScript}/$packageName/$scriptName.lua"
            val originalContent = workspaceStorage.read(relPath)
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            val destinationFile = File(cacheDir, "$scriptName.lua")
            val headerContent = "-- name: $scriptName\n-- descript: $description\n-- package: $packageName\n-- author: $author\n\n"
            val mergedContent = headerContent + originalContent
            destinationFile.writeText(mergedContent)
            destinationFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun transformBooleanValuesToJsonArrayInMaps(
        dataList: MutableList<MutableMap.MutableEntry<String, Any?>>
    ) {
        for (entry in dataList) {
            val value = entry.value
            if (value is Boolean) {
                try {
                    val newArray = JSONArray(arrayOf<Any?>(value, "", "v1.0"))
                    entry.setValue(newArray)
                } catch (_: JSONException) {
                } catch (_: Exception) {
                }
            }
        }
    }
}
