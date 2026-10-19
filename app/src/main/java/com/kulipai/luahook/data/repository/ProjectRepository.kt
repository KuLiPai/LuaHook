package com.kulipai.luahook.data.repository

import android.content.Context
import android.net.Uri
import com.kulipai.luahook.core.file.WorkspaceStorage
import com.kulipai.luahook.core.project.Project
import com.kulipai.luahook.core.shell.ShellExecutor
import com.kulipai.luahook.data.repository.ProjectRepository.Companion.INFO_JSON
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.luaj.Globals
import org.luaj.lib.jse.JsePlatform
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

interface ProjectRepository {
    suspend fun getProjects(): List<Project>
    suspend fun createProject(
        name: String,
        description: String,
        author: String,
        iconPath: String?,
        iconUnicode: String?,
        scope: List<String>,
        launcher: String
    ): Boolean
    suspend fun setProjectEnabled(name: String, enabled: Boolean)
    suspend fun deleteProject(name: String)
    suspend fun importProject(context: Context, uri: Uri): Boolean
    suspend fun exportProject(context: Context, name: String, uri: Uri): Boolean

    companion object {
        const val INFO_JSON = "/Project/info.json"
    }
}

class ProjectRepositoryImpl(
    private val workspaceStorage: WorkspaceStorage,
    private val shellExecutor: ShellExecutor,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ProjectRepository {

    override suspend fun getProjects(): List<Project> = withContext(ioDispatcher) {
        val projects = mutableListOf<Project>()
        val infoMap = workspaceStorage.readMap(INFO_JSON) // Name -> Boolean

        infoMap.forEach { (name, enabledObj) ->
            val isEnabled = enabledObj as? Boolean ?: false
            val projectRelativePath = "${WorkspaceStorage.Project}/$name"
            val projectFullPath = "${WorkspaceStorage.DIR}$projectRelativePath"
            val initPath = "$projectRelativePath/init.lua"

            if (workspaceStorage.directoryExists(projectFullPath)) {
                var icon = "icon.png"
                var author = ""
                var desc = ""
                val scopeList = mutableListOf<String>()
                var launcher = ""

                try {
                    val luaCode = workspaceStorage.read(initPath)
                    if (luaCode.isNotEmpty()) {
                        try {
                            val globals: Globals = JsePlatform.standardGlobals()
                            val chunk = globals.load(luaCode)
                            chunk.call()

                            icon = globals.get("icon").optjstring("icon.png")
                            author = globals.get("author").optjstring("")
                            desc = globals.get("description").optjstring("")
                            launcher = globals.get("launcher").optjstring("")

                            val scopeLua = globals.get("scope")
                            if (scopeLua.isstring()) {
                                scopeList.add(scopeLua.tojstring())
                            } else if (scopeLua.istable()) {
                                val len = scopeLua.length()
                                for (i in 1..len) {
                                    scopeList.add(scopeLua.get(i).tojstring())
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                projects.add(Project(name, isEnabled, icon, author, desc, scopeList, launcher))
            }
        }
        projects
    }

    override suspend fun createProject(
        name: String,
        description: String,
        author: String,
        iconPath: String?,
        iconUnicode: String?,
        scope: List<String>,
        launcher: String
    ): Boolean = withContext(ioDispatcher) {
        if (name.isEmpty()) return@withContext false

        val projectRelativePath = "${WorkspaceStorage.Project}/$name"
        val projectFullPath = "${WorkspaceStorage.DIR}$projectRelativePath"

        if (workspaceStorage.directoryExists(projectFullPath)) return@withContext false

        if (!workspaceStorage.ensureDirectoryExists("${WorkspaceStorage.DIR}${WorkspaceStorage.Project}")) return@withContext false
        if (!workspaceStorage.ensureDirectoryExists(projectFullPath)) return@withContext false

        // Double Check
        if (!workspaceStorage.directoryExists(projectFullPath)) return@withContext false

        // Handle Icon
        var iconValue = "icon.png"
        if (!iconUnicode.isNullOrEmpty()) {
            iconValue = iconUnicode // e.g. "\u8989"
        } else if (iconPath != null) {
            val destIcon = "$projectFullPath/icon.png"
            workspaceStorage.copy(iconPath, destIcon)
            workspaceStorage.ensureReadable(destIcon)
        }

        // Generate init.lua
        val scopeLua = if (scope.contains("all")) "\"all\"" else {
            "{\n" + scope.joinToString(",\n") { "    \"$it\"" } + "\n}"
        }

        val initLua = """
name = "$name"
description = "$description"
author = "$author"
icon = "$iconValue"
launcher = "$launcher"
scope = ${scopeLua.trimIndent()}
""".trimIndent()

        if (!workspaceStorage.write("$projectRelativePath/init.lua", initLua)) return@withContext false

        // Create empty main.lua
        if (!workspaceStorage.write(
                "$projectRelativePath/main.lua",
                "-- Main entry for $name\n\nprint(\"Hello from $name\")"
            )
        ) return@withContext false

        // Update info.json
        val infoMap = workspaceStorage.readMap(INFO_JSON)
        infoMap[name] = true // Enabled by default
        workspaceStorage.writeMap(INFO_JSON, infoMap)
    }

    override suspend fun setProjectEnabled(name: String, enabled: Boolean) = withContext(ioDispatcher) {
        val infoMap = workspaceStorage.readMap(INFO_JSON)
        infoMap[name] = enabled
        workspaceStorage.writeMap(INFO_JSON, infoMap)
        Unit
    }

    override suspend fun deleteProject(name: String) = withContext(ioDispatcher) {
        val projectDir = "${WorkspaceStorage.Project}/$name"
        workspaceStorage.rm(projectDir)

        val infoMap = workspaceStorage.readMap(INFO_JSON)
        infoMap.remove(name)
        workspaceStorage.writeMap(INFO_JSON, infoMap)
        Unit
    }

    override suspend fun importProject(context: Context, uri: Uri): Boolean = withContext(ioDispatcher) {
        val cacheFile = File(context.externalCacheDir ?: context.cacheDir, "import_temp.zip")
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                cacheFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext false
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }

        val tempZip = "${WorkspaceStorage.DIR}/import_temp.zip"
        val tempDir = "${WorkspaceStorage.DIR}/import_temp_dir"

        workspaceStorage.delete(tempZip)
        workspaceStorage.delete(tempDir)
        workspaceStorage.copy(cacheFile.absolutePath, tempZip)
        workspaceStorage.ensureDirectoryExists(tempDir)
        workspaceStorage.unzip(tempZip, tempDir)

        var root = tempDir
        if (!checkFiles(root)) {
            val lines = workspaceStorage.listFiles(tempDir)
            if (lines.size == 1) {
                val sub = "$tempDir/${lines[0]}"
                if (checkFiles(sub)) {
                    root = sub
                } else {
                    cleanupImport(tempZip, tempDir, cacheFile)
                    return@withContext false
                }
            } else {
                cleanupImport(tempZip, tempDir, cacheFile)
                return@withContext false
            }
        }

        val initLua = workspaceStorage.read(root.removePrefix(WorkspaceStorage.DIR) + "/init.lua")
        val name = parseName(initLua)
        if (name.isEmpty()) {
            cleanupImport(tempZip, tempDir, cacheFile)
            return@withContext false
        }

        val dest = "${WorkspaceStorage.DIR}${WorkspaceStorage.Project}/$name"
        if (workspaceStorage.directoryExists(dest)) {
            cleanupImport(tempZip, tempDir, cacheFile)
            return@withContext false
        }

        workspaceStorage.move(root, dest)
        workspaceStorage.chmod(dest, "777", recursive = true)

        val infoMap = workspaceStorage.readMap(INFO_JSON)
        infoMap[name] = true
        workspaceStorage.writeMap(INFO_JSON, infoMap)

        cleanupImport(tempZip, tempDir, cacheFile)
        true
    }

    private fun cleanupImport(zip: String, dir: String, cacheFile: File? = null) {
        workspaceStorage.delete(zip)
        workspaceStorage.delete(dir)
        try {
            cacheFile?.delete()
        } catch (_: Exception) {}
    }

    private fun checkFiles(dir: String): Boolean {
        val init = "$dir/init.lua"
        val main = "$dir/main.lua"
        return workspaceStorage.directoryExists(init) && workspaceStorage.directoryExists(main)
    }

    private fun parseName(lua: String): String {
        return try {
            val globals = JsePlatform.standardGlobals()
            val chunk = globals.load(lua)
            chunk.call()
            globals.get("name").optjstring("")
        } catch (e: Exception) {
            ""
        }
    }

    override suspend fun exportProject(context: Context, name: String, uri: Uri): Boolean = withContext(ioDispatcher) {
        val projectRelativePath = "${WorkspaceStorage.Project}/$name"
        val projectFullPath = "${WorkspaceStorage.DIR}$projectRelativePath"

        if (!workspaceStorage.directoryExists(projectFullPath)) return@withContext false

        val cacheDir = File(context.externalCacheDir ?: context.cacheDir, "export_temp")
        if (cacheDir.exists()) cacheDir.deleteRecursively()
        cacheDir.mkdirs()

        val destDir = File(cacheDir, name)

        // Copy using workspaceStorage.copy
        workspaceStorage.copy(projectFullPath, cacheDir.absolutePath)
        workspaceStorage.chmod(cacheDir.absolutePath, "777", recursive = true)

        if (!destDir.exists()) return@withContext false

        try {
            context.contentResolver.openOutputStream(uri)?.use { output ->
                ZipOutputStream(output).use { zipOut ->
                    zipFolder(destDir, "", zipOut)
                }
            } ?: return@withContext false
            cacheDir.deleteRecursively()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun zipFolder(folder: File, parentPath: String, zipOut: ZipOutputStream) {
        val files = folder.listFiles() ?: return
        for (file in files) {
            val entryPath = if (parentPath.isEmpty()) file.name else "$parentPath/${file.name}"
            if (file.isDirectory) {
                zipFolder(file, entryPath, zipOut)
            } else {
                zipOut.putNextEntry(ZipEntry(entryPath))
                file.inputStream().use { input ->
                    input.copyTo(zipOut)
                }
                zipOut.closeEntry()
            }
        }
    }
}
