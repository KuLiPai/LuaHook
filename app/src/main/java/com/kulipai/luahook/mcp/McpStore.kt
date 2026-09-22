package com.kulipai.luahook.mcp

import com.kulipai.luahook.core.file.WorkspaceFileManager
import com.kulipai.luahook.core.pm.PackageUtils
import com.kulipai.luahook.core.project.ProjectManager
import com.kulipai.luahook.core.script.ScriptConfigHelper
import com.kulipai.luahook.core.shell.ShellManager
import com.kulipai.luahook.core.shell.ShellResult
import com.kulipai.luahook.app.MyApplication
import org.json.JSONArray
import org.json.JSONObject

object McpStore {
    fun listProjects(): JSONArray {
        val array = JSONArray()
        ProjectManager.getProjects().forEach { project ->
            array.put(
                JSONObject()
                    .put("name", project.name)
                    .put("enabled", project.isEnabled)
                    .put("author", project.author)
                    .put("description", project.description)
                    .put("scope", JSONArray(project.scope))
                    .put("launcher", project.launcher)
            )
        }
        return array
    }

    fun createProject(args: JSONObject): JSONObject {
        val name = requireName(args.optString("name"))
        val ok = ProjectManager.createProject(
            name,
            args.optString("description"),
            args.optString("author"),
            null,
            args.optString("icon"),
            stringList(args.opt("scope")),
            args.optString("launcher")
        )
        return JSONObject().put("ok", ok).put("name", name)
    }

    fun setProjectEnabled(args: JSONObject): JSONObject {
        val name = requireName(args.optString("name"))
        ProjectManager.setProjectEnabled(name, args.optBoolean("enabled", true))
        return JSONObject().put("ok", true).put("name", name).put("enabled", args.optBoolean("enabled", true))
    }

    fun deleteProject(args: JSONObject): JSONObject {
        val name = requireName(args.optString("name"))
        ProjectManager.deleteProject(name)
        return JSONObject().put("ok", true).put("name", name)
    }

    fun readProjectFile(args: JSONObject): JSONObject {
        val name = requireName(args.optString("name"))
        val file = requireFile(args.optString("file", "main.lua"))
        val text = WorkspaceFileManager.read("${WorkspaceFileManager.Project}/$name/$file")
        return JSONObject().put("name", name).put("file", file).put("content", text)
    }

    fun writeProjectFile(args: JSONObject): JSONObject {
        val name = requireName(args.optString("name"))
        val file = requireFile(args.optString("file", "main.lua"))
        val dir = "${WorkspaceFileManager.DIR}${WorkspaceFileManager.Project}/$name"
        if (!WorkspaceFileManager.directoryExists(dir)) {
            return JSONObject().put("ok", false).put("error", "project not found")
        }
        val ok = WorkspaceFileManager.write("${WorkspaceFileManager.Project}/$name/$file", args.optString("content"))
        return JSONObject().put("ok", ok).put("name", name).put("file", file)
    }

    fun readGlobalScript(): JSONObject {
        return JSONObject().put("path", "/global.lua").put("content", WorkspaceFileManager.read("/global.lua"))
    }

    fun writeGlobalScript(args: JSONObject): JSONObject {
        val ok = WorkspaceFileManager.write("/global.lua", args.optString("content"))
        return JSONObject().put("ok", ok).put("path", "/global.lua")
    }

    fun readSelectedApps(): JSONObject {
        return JSONObject().put("packages", JSONArray(WorkspaceFileManager.readStringList("/apps.txt")))
    }

    fun writeSelectedApps(args: JSONObject): JSONObject {
        val packages = stringList(args.opt("packages"))
        WorkspaceFileManager.writeStringList("/apps.txt", packages)
        return JSONObject().put("ok", true).put("packages", JSONArray(packages))
    }

    fun listAppScripts(args: JSONObject): JSONObject {
        val pkg = requirePackage(args.optString("package"))
        val entries = JSONArray()
        ScriptConfigHelper.readConf(pkg).forEach { entry ->
            entries.put(JSONObject().put("name", entry.key).put("meta", entry.value?.toString() ?: ""))
        }
        return JSONObject().put("package", pkg).put("scripts", entries)
    }

    fun createAppScript(args: JSONObject): JSONObject {
        val pkg = requirePackage(args.optString("package"))
        val name = requireName(args.optString("name"))
        val description = args.optString("description")
        ScriptConfigHelper.writeScriptConfig(pkg, name, description)
        val path = "${WorkspaceFileManager.AppScript}/$pkg/$name.lua"
        val ok = WorkspaceFileManager.write(path, args.optString("content", "-- $name\n"))
        return JSONObject().put("ok", ok).put("package", pkg).put("name", name)
    }

    fun readAppScript(args: JSONObject): JSONObject {
        val pkg = requirePackage(args.optString("package"))
        val name = requireName(args.optString("name"))
        val path = "${WorkspaceFileManager.AppScript}/$pkg/$name.lua"
        return JSONObject().put("package", pkg).put("name", name).put("content", WorkspaceFileManager.read(path))
    }

    fun writeAppScript(args: JSONObject): JSONObject {
        val pkg = requirePackage(args.optString("package"))
        val name = requireName(args.optString("name"))
        val path = "${WorkspaceFileManager.AppScript}/$pkg/$name.lua"
        val ok = WorkspaceFileManager.write(path, args.optString("content"))
        return JSONObject().put("ok", ok).put("package", pkg).put("name", name)
    }

    fun setAppScriptEnabled(args: JSONObject): JSONObject {
        val pkg = requirePackage(args.optString("package"))
        val name = requireName(args.optString("name"))
        val enabled = args.optBoolean("enabled", true)
        val path = "${WorkspaceFileManager.AppConf}/$pkg.txt"
        val map = WorkspaceFileManager.readMap(path)
        val current = map[name]
        val description = when (current) {
            is org.json.JSONArray -> current.optString(1, "")
            else -> args.optString("description")
        }
        map[name] = org.json.JSONArray().put(enabled).put(description).put("v1.0")
        val ok = WorkspaceFileManager.writeMap(path, map)
        return JSONObject().put("ok", ok).put("package", pkg).put("name", name).put("enabled", enabled)
    }

    fun deleteAppScript(args: JSONObject): JSONObject {
        val pkg = requirePackage(args.optString("package"))
        val name = requireName(args.optString("name"))
        WorkspaceFileManager.rm("${WorkspaceFileManager.AppScript}/$pkg/$name.lua")
        val path = "${WorkspaceFileManager.AppConf}/$pkg.txt"
        val map = WorkspaceFileManager.readMap(path)
        map.remove(name)
        WorkspaceFileManager.writeMap(path, map)
        return JSONObject().put("ok", true).put("package", pkg).put("name", name)
    }

    fun readLogs(args: JSONObject): JSONObject {
        val lines = args.optInt("lines", 200).coerceIn(1, 2000)
        val text = when (val result = ShellManager.shell("logcat -d -s LuaXposed:V")) {
            is ShellResult.Success -> result.stdout.lineSequence().filter { it.isNotBlank() }.toList().takeLast(lines).joinToString("\n")
            is ShellResult.Error -> ""
        }
        return JSONObject().put("tag", "LuaXposed").put("content", text)
    }

    fun listInstalledApps(): JSONArray {
        val array = JSONArray()
        PackageUtils.getInstalledApps(MyApplication.instance).forEach { app ->
            array.put(
                JSONObject()
                    .put("name", app.appName)
                    .put("package", app.packageName)
                    .put("versionName", app.versionName)
                    .put("versionCode", app.versionCode)
            )
        }
        return array
    }

    private fun requireName(name: String): String {
        val value = name.trim()
        if (value.isEmpty() || value.contains('/') || value.contains('\\') || value.contains("..")) {
            error("invalid name")
        }
        return value
    }

    private fun requirePackage(name: String): String {
        val value = name.trim()
        if (!value.matches(Regex("[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)+"))) error("invalid package")
        return value
    }

    private fun requireFile(file: String): String {
        val name = file.trim().removePrefix("/")
        if (name.isEmpty() || name.contains("..") || name.contains('\\')) error("invalid file")
        return name
    }

    private fun stringList(raw: Any?): List<String> {
        return when (raw) {
            is JSONArray -> List(raw.length()) { raw.optString(it) }.filter { it.isNotBlank() }
            is String -> raw.split(',').map { it.trim() }.filter { it.isNotEmpty() }
            else -> emptyList()
        }
    }
}
