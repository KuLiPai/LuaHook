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

/**
 * MCP 工具的工作区读写。只调用项目已有的 ProjectManager、WorkspaceFileManager、ScriptConfigHelper，不另建目录。
 * 根目录是 /data/local/tmp/LuaHook。
 */
object McpStore {
    /** 列出 /Project 下的项目，字段与项目卡片一致。 */
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

    /** 按 name 新建项目。scope 可以是 JSON 数组，也可以是逗号分隔的包名。 */
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

    /** 改 /Project/info.json 里该项目的启用状态。enabled 缺省为 true。 */
    fun setProjectEnabled(args: JSONObject): JSONObject {
        val name = requireName(args.optString("name"))
        ProjectManager.setProjectEnabled(name, args.optBoolean("enabled", true))
        return JSONObject().put("ok", true).put("name", name).put("enabled", args.optBoolean("enabled", true))
    }

    /** 删除整个项目目录，并把它从 info.json 里去掉。 */
    fun deleteProject(args: JSONObject): JSONObject {
        val name = requireName(args.optString("name"))
        ProjectManager.deleteProject(name)
        return JSONObject().put("ok", true).put("name", name)
    }

    /** 读项目里的相对路径文件，默认 main.lua。读不到时走 root cat。 */
    fun readProjectFile(args: JSONObject): JSONObject {
        val name = requireName(args.optString("name"))
        val file = requireFile(args.optString("file", "main.lua"))
        val text = WorkspaceFileManager.read("${WorkspaceFileManager.Project}/$name/$file")
        return JSONObject().put("name", name).put("file", file).put("content", text)
    }

    /** 写项目文件。项目目录不存在时返回 ok=false，不会顺手建项目。 */
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

    /** 读 /global.lua。这个脚本会对作用域内除本模块外的包执行。 */
    fun readGlobalScript(): JSONObject {
        return JSONObject().put("path", "/global.lua").put("content", WorkspaceFileManager.read("/global.lua"))
    }

    /** 覆盖 /global.lua。content 为空会把全局脚本清空。 */
    fun writeGlobalScript(args: JSONObject): JSONObject {
        val ok = WorkspaceFileManager.write("/global.lua", args.optString("content"))
        return JSONObject().put("ok", ok).put("path", "/global.lua")
    }

    /** 读 /apps.txt。只有列在这里的包才会加载 AppScript。 */
    fun readSelectedApps(): JSONObject {
        return JSONObject().put("packages", JSONArray(WorkspaceFileManager.readStringList("/apps.txt")))
    }

    /** 覆盖 /apps.txt。packages 可以是数组或逗号分隔字符串。这只改工作区，不改 LSPosed 作用域。 */
    fun writeSelectedApps(args: JSONObject): JSONObject {
        val packages = stringList(args.opt("packages"))
        WorkspaceFileManager.writeStringList("/apps.txt", packages)
        return JSONObject().put("ok", true).put("packages", JSONArray(packages))
    }

    /** 列出某个包在 AppConf 里的脚本名和元数据。 */
    fun listAppScripts(args: JSONObject): JSONObject {
        val pkg = requirePackage(args.optString("package"))
        val entries = JSONArray()
        ScriptConfigHelper.readConf(pkg).forEach { entry ->
            entries.put(JSONObject().put("name", entry.key).put("meta", entry.value?.toString() ?: ""))
        }
        return JSONObject().put("package", pkg).put("scripts", entries)
    }

    /**
     * 新建应用脚本。先用 ScriptConfigHelper 写 AppConf/<包名>.txt，再写 AppScript/<包名>/<名字>.lua。
     * 配置格式是 [启用, 描述, "v1.0"]。
     */
    fun createAppScript(args: JSONObject): JSONObject {
        val pkg = requirePackage(args.optString("package"))
        val name = requireName(args.optString("name"))
        val description = args.optString("description")
        ScriptConfigHelper.writeScriptConfig(pkg, name, description)
        val path = "${WorkspaceFileManager.AppScript}/$pkg/$name.lua"
        val ok = WorkspaceFileManager.write(path, args.optString("content", "-- $name\n"))
        return JSONObject().put("ok", ok).put("package", pkg).put("name", name)
    }

    /** 读 AppScript/<包名>/<名字>.lua。 */
    fun readAppScript(args: JSONObject): JSONObject {
        val pkg = requirePackage(args.optString("package"))
        val name = requireName(args.optString("name"))
        val path = "${WorkspaceFileManager.AppScript}/$pkg/$name.lua"
        return JSONObject().put("package", pkg).put("name", name).put("content", WorkspaceFileManager.read(path))
    }

    /** 覆盖已有应用脚本的内容，不改 AppConf 里的启用状态。 */
    fun writeAppScript(args: JSONObject): JSONObject {
        val pkg = requirePackage(args.optString("package"))
        val name = requireName(args.optString("name"))
        val path = "${WorkspaceFileManager.AppScript}/$pkg/$name.lua"
        val ok = WorkspaceFileManager.write(path, args.optString("content"))
        return JSONObject().put("ok", ok).put("package", pkg).put("name", name)
    }

    /** 改 AppConf 里该脚本的启用位，保留原来的描述。没有单独的启用接口，所以直接写 [enabled, 描述, "v1.0"]。 */
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

    /** 删掉 lua 文件，并从 AppConf 里移除这一项。 */
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

    /** 只读 logcat 里 tag 为 LuaXposed 的行，默认 200 行，最多 2000。不改、不清日志。 */
    fun readLogs(args: JSONObject): JSONObject {
        val lines = args.optInt("lines", 200).coerceIn(1, 2000)
        val text = when (val result = ShellManager.shell("logcat -d -s LuaXposed:V")) {
            is ShellResult.Success -> result.stdout.lineSequence().filter { it.isNotBlank() }.toList().takeLast(lines).joinToString("\n")
            is ShellResult.Error -> ""
        }
        return JSONObject().put("tag", "LuaXposed").put("content", text)
    }

    /** 列出可启动的已安装应用，给作用域勾选用。只读，不请求 LSPosed 作用域。 */
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

    /** 项目名和脚本名不能为空，也不能带路径。 */
    private fun requireName(name: String): String {
        val value = name.trim()
        if (value.isEmpty() || value.contains('/') || value.contains('\\') || value.contains("..")) {
            error("invalid name")
        }
        return value
    }

    /** 包名必须是 a.b 这种形式。 */
    private fun requirePackage(name: String): String {
        val value = name.trim()
        if (!value.matches(Regex("[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)+"))) error("invalid package")
        return value
    }

    /** 项目内相对路径，禁止 .. 和反斜杠。 */
    private fun requireFile(file: String): String {
        val name = file.trim().removePrefix("/")
        if (name.isEmpty() || name.contains("..") || name.contains('\\')) error("invalid file")
        return name
    }

    /** JSON 数组或逗号分隔字符串都收成包名列表。 */
    private fun stringList(raw: Any?): List<String> {
        return when (raw) {
            is JSONArray -> List(raw.length()) { raw.optString(it) }.filter { it.isNotBlank() }
            is String -> raw.split(',').map { it.trim() }.filter { it.isNotEmpty() }
            else -> emptyList()
        }
    }
}
