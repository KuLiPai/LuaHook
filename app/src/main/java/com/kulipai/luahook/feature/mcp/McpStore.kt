package com.kulipai.luahook.feature.mcp

import com.kulipai.luahook.app.di.AppContainer
import com.kulipai.luahook.core.file.WorkspaceStorage
import com.kulipai.luahook.core.pm.PackageUtils
import com.kulipai.luahook.core.shell.ShellResult
import com.kulipai.luahook.app.MyApplication
import java.io.File
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject

/**
 * MCP 工具的工作区读写。通过 AppContainer 调用仓储与存储接口，不另建目录。
 * 根目录是 /data/local/tmp/LuaHook。
 */
object McpStore {

    private val storage: WorkspaceStorage
        get() = AppContainer.instance.workspaceStorage

    /** 列出 /Project 下的项目，字段与项目卡片一致。 */
    fun listProjects(): JSONArray {
        val array = JSONArray()
        runBlocking { AppContainer.instance.projectRepository.getProjects() }.forEach { project ->
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
        val ok = runBlocking {
            AppContainer.instance.projectRepository.createProject(
                name,
                args.optString("description"),
                args.optString("author"),
                null,
                args.optString("icon"),
                stringList(args.opt("scope")),
                args.optString("launcher")
            )
        }
        return JSONObject().put("ok", ok).put("name", name)
    }

    /** 改 /Project/info.json 里该项目的启用状态。enabled 缺省为 true。 */
    fun setProjectEnabled(args: JSONObject): JSONObject {
        val name = requireName(args.optString("name"))
        val enabled = args.optBoolean("enabled", true)
        runBlocking {
            AppContainer.instance.projectRepository.setProjectEnabled(name, enabled)
        }
        return JSONObject().put("ok", true).put("name", name).put("enabled", enabled)
    }

    /** 删除整个项目目录，并把它从 info.json 里去掉。 */
    fun deleteProject(args: JSONObject): JSONObject {
        val name = requireName(args.optString("name"))
        runBlocking {
            AppContainer.instance.projectRepository.deleteProject(name)
        }
        return JSONObject().put("ok", true).put("name", name)
    }

    /** 读项目里的相对路径文件，默认 main.lua。读不到时走 root cat。 */
    fun readProjectFile(args: JSONObject): JSONObject {
        val name = requireName(args.optString("name"))
        val file = requireFile(args.optString("file", "main.lua"))
        val text = storage.read("${WorkspaceStorage.Project}/$name/$file")
        return JSONObject().put("name", name).put("file", file).put("content", text)
    }

    /** 写项目文件。项目目录不存在时返回 ok=false，不会顺手建项目。 */
    fun writeProjectFile(args: JSONObject): JSONObject {
        val name = requireName(args.optString("name"))
        val file = requireFile(args.optString("file", "main.lua"))
        val dir = "${WorkspaceStorage.DIR}${WorkspaceStorage.Project}/$name"
        if (!storage.directoryExists(dir)) {
            return JSONObject().put("ok", false).put("error", "project not found")
        }
        val ok = storage.write("${WorkspaceStorage.Project}/$name/$file", args.optString("content"))
        return JSONObject().put("ok", ok).put("name", name).put("file", file)
    }

    /** 读 /global.lua。这个脚本会对作用域内除本模块外的包执行。 */
    fun readGlobalScript(): JSONObject {
        return JSONObject().put("path", "/global.lua").put("content", storage.read("/global.lua"))
    }

    /** 覆盖 /global.lua。content 为空会把全局脚本清空。 */
    fun writeGlobalScript(args: JSONObject): JSONObject {
        val ok = storage.write("/global.lua", args.optString("content"))
        return JSONObject().put("ok", ok).put("path", "/global.lua")
    }

    /** 读 /apps.txt。只有列在这里的包才会加载 AppScript。 */
    fun readSelectedApps(): JSONObject {
        return JSONObject().put("packages", JSONArray(storage.readStringList("/apps.txt")))
    }

    /** 覆盖 /apps.txt。packages 可以是数组或逗号分隔字符串。这只改工作区，不改 LSPosed 作用域。 */
    fun writeSelectedApps(args: JSONObject): JSONObject {
        val packages = stringList(args.opt("packages"))
        storage.writeStringList("/apps.txt", packages)
        return JSONObject().put("ok", true).put("packages", JSONArray(packages))
    }

    /** 列出某个包在 AppConf 里的脚本名和元数据。 */
    fun listAppScripts(args: JSONObject): JSONObject {
        val pkg = requirePackage(args.optString("package"))
        val entries = JSONArray()
        runBlocking { AppContainer.instance.scriptRepository.getAppScripts(pkg) }.forEach { entry ->
            val meta = JSONArray().put(entry.enabled).put(entry.description).put(entry.version).toString()
            entries.put(JSONObject().put("name", entry.name).put("meta", meta))
        }
        return JSONObject().put("package", pkg).put("scripts", entries)
    }

    /**
     * 新建应用脚本。
     */
    fun createAppScript(args: JSONObject): JSONObject {
        val pkg = requirePackage(args.optString("package"))
        val name = requireName(args.optString("name"))
        val description = args.optString("description")
        val content = args.optString("content", "-- $name\n")
        val ok = runBlocking {
            AppContainer.instance.scriptRepository.createAppScript(pkg, name, description, content)
        }
        return JSONObject().put("ok", ok).put("package", pkg).put("name", name)
    }

    /** 读 AppScript/<包名>/<名字>.lua。 */
    fun readAppScript(args: JSONObject): JSONObject {
        val pkg = requirePackage(args.optString("package"))
        val name = requireName(args.optString("name"))
        val content = runBlocking { AppContainer.instance.scriptRepository.readAppScript(pkg, name) }
        return JSONObject().put("package", pkg).put("name", name).put("content", content)
    }

    /** 覆盖已有应用脚本的内容，不改 AppConf 里的启用状态。 */
    fun writeAppScript(args: JSONObject): JSONObject {
        val pkg = requirePackage(args.optString("package"))
        val name = requireName(args.optString("name"))
        val ok = runBlocking { AppContainer.instance.scriptRepository.saveAppScript(pkg, name, args.optString("content")) }
        return JSONObject().put("ok", ok).put("package", pkg).put("name", name)
    }

    /** 改 AppConf 里该脚本的启用位，保留原来的描述。 */
    fun setAppScriptEnabled(args: JSONObject): JSONObject {
        val pkg = requirePackage(args.optString("package"))
        val name = requireName(args.optString("name"))
        val enabled = args.optBoolean("enabled", true)
        val ok = runBlocking { AppContainer.instance.scriptRepository.setAppScriptEnabled(pkg, name, enabled) }
        return JSONObject().put("ok", ok).put("package", pkg).put("name", name).put("enabled", enabled)
    }

    /** 删掉 lua 文件，并从 AppConf 里移除这一项。 */
    fun deleteAppScript(args: JSONObject): JSONObject {
        val pkg = requirePackage(args.optString("package"))
        val name = requireName(args.optString("name"))
        val ok = runBlocking { AppContainer.instance.scriptRepository.deleteAppScript(pkg, name) }
        return JSONObject().put("ok", ok).put("package", pkg).put("name", name)
    }

    /** 只读 logcat 里 tag 为 LuaXposed 的行，默认 200 行，最多 2000。不改、不清日志。 */
    fun readLogs(args: JSONObject): JSONObject {
        val lines = args.optInt("lines", 200).coerceIn(1, 2000)
        val text = when (val result = AppContainer.instance.shellExecutor.exec("logcat -d -s LuaXposed:V")) {
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

    /** 从 assets/skill 读取文档，支持多种灵活相对路径解析（兼顾 Android Runtime 与 JVM 单元测试） */
    fun readSkillAsset(path: String): String {
        val clean = path.trim().removePrefix("/").removePrefix("skill/").removePrefix("/")
        val candidates = listOf(
            clean,
            "references/$clean",
            "references/source-docs/$clean",
            "references/source-docs/zh/$clean",
            "references/source-docs/zh/manual/$clean",
            "references/source-docs/zh/manual/native/$clean",
            "references/source-docs/zh/example/$clean",
            "$clean.md",
            "references/$clean.md",
            "references/source-docs/zh/$clean.md",
            "references/source-docs/zh/manual/$clean.md",
            "references/source-docs/zh/manual/native/$clean.md",
            "references/source-docs/zh/example/$clean.md"
        )
        val app = MyApplication.instanceOrNull
        for (candidate in candidates) {
            try {
                if (app != null) {
                    val text = app.assets.open("skill/$candidate").bufferedReader().use { it.readText() }
                    if (text.isNotBlank()) return text
                } else {
                    val file = listOf(
                        File("src/main/assets/skill/$candidate"),
                        File("app/src/main/assets/skill/$candidate")
                    ).firstOrNull { it.exists() && it.isFile }
                    if (file != null) {
                        val text = file.readText()
                        if (text.isNotBlank()) return text
                    }
                }
            } catch (_: Exception) {}
        }
        return ""
    }

    /** 获取完整的 Skill 规则 Markdown */
    fun getFullSkillMarkdown(): String {
        val skill = readSkillAsset("SKILL.md")
        val core = readSkillAsset("references/core-api.md")
        val hookZh = readSkillAsset("references/source-docs/zh/manual/hook.md")
        val fieldZh = readSkillAsset("references/source-docs/zh/manual/field.md")
        val patterns = readSkillAsset("references/patterns.md")
        val luajpp = readSkillAsset("references/luajpp-cheatsheet.md")
        val luajppZh = readSkillAsset("references/source-docs/zh/luaj++.md")
        val native = readSkillAsset("references/native-api.md")
        val nativePointer = readSkillAsset("references/source-docs/zh/manual/native/pointer.md")
        val nativeMemIo = readSkillAsset("references/source-docs/zh/manual/native/memory_io.md")
        val nativeHook = readSkillAsset("references/source-docs/zh/manual/native/hook.md")

        return listOf(
            skill, core, hookZh, fieldZh, patterns, luajpp, luajppZh, native, nativePointer, nativeMemIo, nativeHook
        ).filter { it.isNotBlank() }.joinToString("\n\n---\n\n").ifEmpty { defaultOverview() }
    }

    /** 工具 get_scripting_guide 的实现，按 topic 给出指南 */
    fun getScriptingGuide(args: JSONObject): JSONObject {
        val rawTopic = args.optString("topic", "overview").trim()
        val topic = rawTopic.lowercase()

        // 1. 优先匹配已定义的各模块主题关键词
        val keywordResult: Pair<String, String>? = when (topic) {
            "", "overview", "default" -> {
                val skill = readSkillAsset("SKILL.md")
                val core = readSkillAsset("references/core-api.md")
                val patterns = readSkillAsset("references/patterns.md")
                "LuaHook Scripting Overview" to if (skill.isNotEmpty()) {
                    "$skill\n\n---\n\n$core\n\n---\n\n$patterns"
                } else {
                    defaultOverview()
                }
            }
            "core", "core_api", "api" -> {
                val core = readSkillAsset("references/core-api.md").ifEmpty { defaultCoreApi() }
                val hookZh = readSkillAsset("references/source-docs/zh/manual/hook.md")
                val fieldZh = readSkillAsset("references/source-docs/zh/manual/field.md")
                val importZh = readSkillAsset("references/source-docs/zh/manual/import.md")
                val lpparamZh = readSkillAsset("references/source-docs/zh/manual/lpparam.md")
                val invokeZh = readSkillAsset("references/source-docs/zh/manual/invoke.md")
                "LuaHook Core API" to listOf(core, hookZh, fieldZh, importZh, lpparamZh, invokeZh)
                    .filter { it.isNotEmpty() }
                    .joinToString("\n\n---\n\n")
            }
            "hook", "hooks", "hookctor", "hookall", "replace" -> {
                "LuaHook Hook API Reference" to readSkillAsset("references/source-docs/zh/manual/hook.md")
            }
            "field", "fields", "il2cpp" -> {
                "LuaHook Field Operations" to readSkillAsset("references/source-docs/zh/manual/field.md")
            }
            "import", "imports", "findclass" -> {
                "LuaHook Class Loading & Imports" to readSkillAsset("references/source-docs/zh/manual/import.md")
            }
            "invoke", "call", "reflection" -> {
                "LuaHook Method Invocation (Invoke)" to readSkillAsset("references/source-docs/zh/manual/invoke.md")
            }
            "lpparam", "classloader" -> {
                "LuaHook LPParam & ClassLoader" to readSkillAsset("references/source-docs/zh/manual/lpparam.md")
            }
            "task", "async", "delay" -> {
                "LuaHook Async Task Helper" to readSkillAsset("references/source-docs/zh/manual/task.md")
            }
            "http", "network" -> {
                "LuaHook HTTP Network Helper" to readSkillAsset("references/source-docs/zh/manual/http.md")
            }
            "json" -> {
                "LuaHook JSON Helper" to readSkillAsset("references/source-docs/zh/manual/json.md")
            }
            "file", "io" -> {
                "LuaHook File IO Helper" to readSkillAsset("references/source-docs/zh/manual/file.md")
            }
            "patterns", "examples", "pattern", "template", "templates" -> {
                val patterns = readSkillAsset("references/patterns.md").ifEmpty { defaultPatterns() }
                val firstScript = readSkillAsset("references/source-docs/zh/firstscript.md")
                "LuaHook Script Patterns & Templates" to "$patterns\n\n---\n\n$firstScript"
            }
            "luajpp", "syntax", "sugar" -> {
                val luajpp = readSkillAsset("references/luajpp-cheatsheet.md").ifEmpty { defaultLuajpp() }
                val luajppZh = readSkillAsset("references/source-docs/zh/luaj++.md")
                "LuaJ++ Syntax Sugar & Java Interop" to "$luajpp\n\n---\n\n$luajppZh"
            }
            "native", "native_api" -> {
                val nativeSummary = readSkillAsset("references/native-api.md").ifEmpty { defaultNative() }
                val pointerZh = readSkillAsset("references/source-docs/zh/manual/native/pointer.md")
                val memIoZh = readSkillAsset("references/source-docs/zh/manual/native/memory_io.md")
                val memStrZh = readSkillAsset("references/source-docs/zh/manual/native/memory_string.md")
                val memAllocZh = readSkillAsset("references/source-docs/zh/manual/native/memory_alloc.md")
                val hookZh = readSkillAsset("references/source-docs/zh/manual/native/hook.md")
                val moduleZh = readSkillAsset("references/source-docs/zh/manual/native/module.md")
                val invokeZh = readSkillAsset("references/source-docs/zh/manual/native/invoke.md")
                val legacyZh = readSkillAsset("references/source-docs/zh/manual/native/legacy.md")
                "LuaHook Native & Memory API (Comprehensive)" to listOf(
                    nativeSummary, pointerZh, memIoZh, memStrZh, memAllocZh, hookZh, moduleZh, invokeZh, legacyZh
                ).filter { it.isNotEmpty() }.joinToString("\n\n---\n\n")
            }
            "native/pointer", "pointer", "luapointer" -> {
                "LuaHook Pointer (LuaPointer) Operations" to readSkillAsset("references/source-docs/zh/manual/native/pointer.md")
            }
            "native/memory", "memory", "memory_io" -> {
                val io = readSkillAsset("references/source-docs/zh/manual/native/memory_io.md")
                val str = readSkillAsset("references/source-docs/zh/manual/native/memory_string.md")
                val alloc = readSkillAsset("references/source-docs/zh/manual/native/memory_alloc.md")
                "LuaHook Native Memory Operations" to listOf(io, str, alloc).filter { it.isNotEmpty() }.joinToString("\n\n---\n\n")
            }
            "native/hook", "inline_hook" -> {
                "LuaHook Native Inline Hooking" to readSkillAsset("references/source-docs/zh/manual/native/hook.md")
            }
            "native/module", "module", "module_base" -> {
                "LuaHook Native Module & Symbol Operations" to readSkillAsset("references/source-docs/zh/manual/native/module.md")
            }
            "native/invoke", "native/new_function", "new_function", "native_function" -> {
                "LuaHook Native Function Invocation" to readSkillAsset("references/source-docs/zh/manual/native/invoke.md")
            }
            "native/legacy", "legacy" -> {
                "LuaHook Native Legacy APIs" to readSkillAsset("references/source-docs/zh/manual/native/legacy.md")
            }
            "all", "full" -> {
                "LuaHook Scripting Guide (Full)" to getFullSkillMarkdown()
            }
            else -> null
        }

        if (keywordResult != null && keywordResult.second.isNotEmpty()) {
            return JSONObject().put("topic", rawTopic).put("title", keywordResult.first).put("guide", keywordResult.second)
        }

        // 2. 支持直接按文件相对路径读取
        val directContent = readSkillAsset(rawTopic)
        if (directContent.isNotEmpty()) {
            val title = rawTopic.substringAfterLast("/").removeSuffix(".md")
                .replace('_', ' ')
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            return JSONObject().put("topic", rawTopic).put("title", "LuaHook Guide: $title").put("guide", directContent)
        }

        // 3. 未识别的 topic：不再静默降级为默认三合一，而是明确提示并返回合法 topic 列表
        val availableTopics = JSONArray()
            .put("overview")
            .put("all")
            .put("core")
            .put("patterns")
            .put("luajpp")
            .put("native")
            .put("hook")
            .put("field")
            .put("import")
            .put("invoke")
            .put("lpparam")
            .put("task")
            .put("http")
            .put("json")
            .put("file")
            .put("native/pointer")
            .put("native/memory")
            .put("native/hook")
            .put("native/invoke")
            .put("native/module")
            .put("native/legacy")

        return JSONObject()
            .put("topic", rawTopic)
            .put("error", "Topic '$rawTopic' not recognized.")
            .put("available_topics", availableTopics)
            .put("hint", "You can request any topic above, or specify a relative doc path under references/ (e.g. 'references/source-docs/zh/manual/field.md' or 'references/native-api.md').")
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
            .put(JSONObject()
                .put("uri", "luahook://skill/field")
                .put("name", "Field Operations")
                .put("description", "getField, setField, getStaticField, setStaticField for instance and static member access")
                .put("mimeType", "text/markdown"))
            .put(JSONObject()
                .put("uri", "luahook://skill/native/memory")
                .put("name", "Native Memory Operations")
                .put("description", "native.memory read/write/alloc/string operations")
                .put("mimeType", "text/markdown"))
        return JSONObject().put("resources", array)
    }

    /** MCP resources/read */
    fun readResource(uri: String): JSONObject {
        val text = when (val cleanUri = uri.trim()) {
            "luahook://skill/full" -> getFullSkillMarkdown()
            "luahook://skill/core-api" -> readSkillAsset("references/core-api.md").ifEmpty { defaultCoreApi() }
            "luahook://skill/patterns" -> readSkillAsset("references/patterns.md").ifEmpty { defaultPatterns() }
            "luahook://skill/luajpp" -> readSkillAsset("references/luajpp-cheatsheet.md").ifEmpty { defaultLuajpp() }
            "luahook://skill/native-api" -> readSkillAsset("references/native-api.md").ifEmpty { defaultNative() }
            "luahook://skill/field" -> readSkillAsset("references/source-docs/zh/manual/field.md")
            "luahook://skill/native/memory" -> readSkillAsset("references/source-docs/zh/manual/native/memory_io.md")
            else -> {
                val assetPath = cleanUri.removePrefix("luahook://skill/")
                val content = readSkillAsset(assetPath)
                if (content.isNotEmpty()) content else error("Resource not found: $uri")
            }
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
