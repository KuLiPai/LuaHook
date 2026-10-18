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

    /** 从 assets/skill 读取文档 */
    fun readSkillAsset(path: String): String {
        return try {
            MyApplication.instance.assets.open("skill/$path").bufferedReader().use { it.readText() }
        } catch (_: Exception) {
            ""
        }
    }

    /** 获取完整的 Skill 规则 Markdown */
    fun getFullSkillMarkdown(): String {
        val skill = readSkillAsset("SKILL.md")
        val core = readSkillAsset("references/core-api.md")
        val patterns = readSkillAsset("references/patterns.md")
        val luajpp = readSkillAsset("references/luajpp-cheatsheet.md")
        val native = readSkillAsset("references/native-api.md")

        return if (skill.isNotEmpty()) {
            buildString {
                append(skill)
                append("\n\n---\n\n")
                append(core)
                append("\n\n---\n\n")
                append(patterns)
                append("\n\n---\n\n")
                append(luajpp)
                append("\n\n---\n\n")
                append(native)
            }
        } else {
            defaultOverview()
        }
    }

    /** 工具 get_scripting_guide 的实现，按 topic 给出指南 */
    fun getScriptingGuide(args: JSONObject): JSONObject {
        val topic = args.optString("topic", "overview").lowercase()
        val (title, content) = when (topic) {
            "core_api", "core", "api" -> {
                "LuaHook Core API" to readSkillAsset("references/core-api.md").ifEmpty { defaultCoreApi() }
            }
            "patterns", "examples", "pattern" -> {
                "LuaHook Script Patterns" to readSkillAsset("references/patterns.md").ifEmpty { defaultPatterns() }
            }
            "luajpp", "syntax" -> {
                "LuaJ++ Syntax Sugar" to readSkillAsset("references/luajpp-cheatsheet.md").ifEmpty { defaultLuajpp() }
            }
            "native" -> {
                "LuaHook Native API" to readSkillAsset("references/native-api.md").ifEmpty { defaultNative() }
            }
            "all", "full" -> {
                "LuaHook Scripting Guide (Full)" to getFullSkillMarkdown()
            }
            else -> {
                val skill = readSkillAsset("SKILL.md")
                val core = readSkillAsset("references/core-api.md")
                val patterns = readSkillAsset("references/patterns.md")
                "LuaHook Scripting Overview" to if (skill.isNotEmpty()) {
                    "$skill\n\n---\n\n$core\n\n---\n\n$patterns"
                } else {
                    defaultOverview()
                }
            }
        }
        return JSONObject().put("topic", topic).put("title", title).put("guide", content)
    }

    /** MCP resources/list */
    fun listResources(): JSONObject {
        val array = JSONArray()
            .put(JSONObject()
                .put("uri", "luahook://skill/full")
                .put("name", "LuaHook Scripting Guide (Full)")
                .put("description", "Complete guide, rules, and best practices for authoring LuaHook scripts")
                .put("mimeType", "text/markdown"))
            .put(JSONObject()
                .put("uri", "luahook://skill/core-api")
                .put("name", "Core Hook APIs")
                .put("description", "Core Java-layer hook APIs: hook, hookctor, replace, lpparam, imports, invoke")
                .put("mimeType", "text/markdown"))
            .put(JSONObject()
                .put("uri", "luahook://skill/patterns")
                .put("name", "Script Patterns & Templates")
                .put("description", "Ready-to-use LuaHook script templates and real-world patterns")
                .put("mimeType", "text/markdown"))
            .put(JSONObject()
                .put("uri", "luahook://skill/luajpp")
                .put("name", "LuaJ++ Cheatsheet")
                .put("description", "LuaJ++ syntax sugar: switch/when, lambda, optional keywords, defer")
                .put("mimeType", "text/markdown"))
            .put(JSONObject()
                .put("uri", "luahook://skill/native-api")
                .put("name", "Native Hook API")
                .put("description", "Native C/C++ inline hooking, module base, LuaPointer, memory ops")
                .put("mimeType", "text/markdown"))
        return JSONObject().put("resources", array)
    }

    /** MCP resources/read */
    fun readResource(uri: String): JSONObject {
        val text = when (uri.trim()) {
            "luahook://skill/full" -> getFullSkillMarkdown()
            "luahook://skill/core-api" -> readSkillAsset("references/core-api.md").ifEmpty { defaultCoreApi() }
            "luahook://skill/patterns" -> readSkillAsset("references/patterns.md").ifEmpty { defaultPatterns() }
            "luahook://skill/luajpp" -> readSkillAsset("references/luajpp-cheatsheet.md").ifEmpty { defaultLuajpp() }
            "luahook://skill/native-api" -> readSkillAsset("references/native-api.md").ifEmpty { defaultNative() }
            else -> error("Resource not found: $uri")
        }
        val content = JSONObject()
            .put("uri", uri)
            .put("mimeType", "text/markdown")
            .put("text", text)
        return JSONObject().put("contents", JSONArray().put(content))
    }

    /** MCP prompts/list */
    fun listPrompts(): JSONObject {
        val genArgs = JSONArray()
            .put(JSONObject()
                .put("name", "target_class")
                .put("description", "Full Java class name (e.g. android.app.Activity or com.example.MyClass)")
                .put("required", true))
            .put(JSONObject()
                .put("name", "target_method")
                .put("description", "Target method or constructor name (use <init> for constructor)")
                .put("required", true))
            .put(JSONObject()
                .put("name", "param_types")
                .put("description", "Comma-separated list of parameter types (e.g. java.lang.String,int). Empty for 0 params.")
                .put("required", false))
            .put(JSONObject()
                .put("name", "timing")
                .put("description", "Hook timing: 'after', 'before', or 'replace'. Default: 'after'.")
                .put("required", false))

        val array = JSONArray()
            .put(JSONObject()
                .put("name", "luahook_expert")
                .put("description", "Configure the AI model with comprehensive LuaHook scripting guidelines and syntax rules")
                .put("arguments", JSONArray()))
            .put(JSONObject()
                .put("name", "generate_hook_script")
                .put("description", "Generate a complete, ready-to-paste LuaHook script for a specific Android target")
                .put("arguments", genArgs))

        return JSONObject().put("prompts", array)
    }

    /** MCP prompts/get */
    fun getPrompt(name: String, args: JSONObject): JSONObject {
        return when (name) {
            "luahook_expert" -> {
                val fullGuide = getFullSkillMarkdown()
                val promptText = "You are an expert LuaHook script developer for Android Xposed-style modification.\n" +
                        "When authoring or modifying LuaHook scripts, adhere strictly to the following framework conventions and syntax:\n\n" +
                        fullGuide
                JSONObject()
                    .put("description", "LuaHook developer system prompt")
                    .put("messages", JSONArray().put(
                        JSONObject()
                            .put("role", "user")
                            .put("content", JSONObject().put("type", "text").put("text", promptText))
                    ))
            }
            "generate_hook_script" -> {
                val targetClass = args.optString("target_class").trim()
                if (targetClass.isEmpty()) error("target_class is required")
                val targetMethod = args.optString("target_method").trim()
                if (targetMethod.isEmpty()) error("target_method is required")
                val paramTypes = args.optString("param_types").trim().let { raw ->
                    if (raw.isEmpty()) emptyList() else raw.split(',').map { it.trim() }.filter { it.isNotEmpty() }
                }
                val timing = args.optString("timing", "after").trim().lowercase()
                val paramsJson = paramTypes.joinToString(", ") { "\"${it.trim()}\"" }

                val promptText = "Write a production-ready LuaHook script for the following hook target:\n" +
                        "- Target Class: $targetClass\n" +
                        "- Target Method: $targetMethod\n" +
                        "- Parameter Types: [$paramsJson]\n" +
                        "- Timing: $timing\n\n" +
                        "Requirements:\n" +
                        "1. Use table-style hook: `hook { class = \"$targetClass\", method = \"$targetMethod\", params = {$paramsJson}, $timing = function(it) ... end }`\n" +
                        "2. Always include `params = {$paramsJson}` explicitly (even if empty `{}`).\n" +
                        "3. For instance methods, use `it.thisObject`. Access arguments via `it.args[0]`, `it.args[1]`.\n" +
                        "4. Prefer `imports` for class loading.\n"
                JSONObject()
                    .put("description", "Generate LuaHook script for $targetClass.$targetMethod")
                    .put("messages", JSONArray().put(
                        JSONObject()
                            .put("role", "user")
                            .put("content", JSONObject().put("type", "text").put("text", promptText))
                    ))
            }
            else -> error("unknown prompt $name")
        }
    }

    private fun defaultOverview() = """
# LuaHook Scripting Guide
LuaHook is an Android Xposed-style framework in Lua.
Core rules:
- Table-style hook: hook { class = "...", method = "...", params = {...}, before/after = function(it) end }
- Zero params: params = {} (always explicit)
- Callbacks: it.args[0], it.result, it.thisObject
- Classloader: lpparam.classLoader
- Imports: imports "android.widget.Toast"
""".trimIndent()

    private fun defaultCoreApi() = """
# LuaHook Core API
- hook { class = "...", method = "...", params = {...}, before = function(it) end, after = function(it) end }
- hookctor { class = "...", params = {...}, after = function(it) end }
- replace { class = "...", method = "...", params = {...}, replace = function(it) return ... end }
- lpparam: lpparam.classLoader, lpparam.packageName
- imports "pkg.ClassName"
""".trimIndent()

    private fun defaultPatterns() = """
# LuaHook Script Patterns
imports "android.widget.Toast"
hook {
  class = "android.app.Activity",
  method = "onCreate",
  params = {"android.os.Bundle"},
  after = function(it)
    Toast.makeText(it.thisObject, "Hello LuaHook!", Toast.LENGTH_SHORT).show()
  end
}
""".trimIndent()

    private fun defaultLuajpp() = """
# LuaJ++ Cheatsheet
- Optional keywords: then, do, in, function
- switch / when / case / default / end
- lambda a,b->a+b
- try / catch(e) / finally / end
""".trimIndent()

    private fun defaultNative() = """
# LuaHook Native API
- native.module_base("libxxx.so")
- native.hook(addr, { ret = "int", onEnter = function(ctx) end, onLeave = function(ret) return ret end })
""".trimIndent()
}
