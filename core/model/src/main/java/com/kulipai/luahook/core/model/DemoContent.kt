package com.kulipai.luahook.core.model

object DemoContent {
    val homeOverview = HomeOverview(
        isPrBuild = true,
        moduleEnabled = true,
        runtimeMode = "Shizuku",
        enabledScriptApps = 3,
        enabledModuleCount = 2,
        hasNewVersion = true,
        androidVersion = "Android 15 / API 35",
        appVersion = "4.2.0-dev",
        lsposedVersion = "LSPosed 1.9.3",
        selinuxStatus = "Enforcing",
        scriptStorage = "/data/user/0/com.kulipai.luahook/files/scripts"
    )

    val scriptApps = listOf(
        ScriptApp(
            appName = "微信",
            packageName = "com.tencent.mm",
            versionName = "8.0.58",
            uid = 10123,
            scriptCount = 2,
            systemApp = false,
            enabled = true,
            scripts = listOf(
                ScriptDefinition(
                    id = "wechat_auto_reply",
                    title = "自动回复助手",
                    description = "监听指定会话并在命中规则后自动回复。",
                    enabled = true,
                    content = """
                        local rules = {
                            { keyword = "测试", reply = "LuaHook 已连接" },
                            { keyword = "状态", reply = "当前脚本运行正常" },
                        }

                        function onMessage(session, text)
                            for _, rule in ipairs(rules) do
                                if string.find(text, rule.keyword) then
                                    reply(session, rule.reply)
                                    log("reply " .. rule.keyword)
                                    return true
                                end
                            end
                            return false
                        end
                    """.trimIndent()
                ),
                ScriptDefinition(
                    id = "wechat_debug_panel",
                    title = "调试面板",
                    description = "向悬浮日志输出联系人与消息事件，便于排查脚本行为。",
                    enabled = false,
                    content = """
                        function onConversationOpened(session)
                            log("open conversation: " .. session)
                        end

                        function onMessage(session, text)
                            floating_log(session .. ": " .. text)
                            return false
                        end
                    """.trimIndent()
                )
            )
        ),
        ScriptApp(
            appName = "QQ",
            packageName = "com.tencent.mobileqq",
            versionName = "9.1.60",
            uid = 10131,
            scriptCount = 1,
            systemApp = false,
            enabled = true,
            scripts = listOf(
                ScriptDefinition(
                    id = "qq_message_hook",
                    title = "消息拦截示例",
                    description = "对命中关键字的消息做标记，并打印日志。",
                    enabled = true,
                    content = """
                        local block_words = { "广告", "抽奖" }

                        function shouldBlock(text)
                            for _, value in ipairs(block_words) do
                                if string.find(text, value) then
                                    return true
                                end
                            end
                            return false
                        end

                        function onMessage(session, text)
                            if shouldBlock(text) then
                                log("blocked text in " .. session)
                                return true
                            end
                            return false
                        end
                    """.trimIndent()
                )
            )
        ),
        ScriptApp(
            appName = "系统设置",
            packageName = "com.android.settings",
            versionName = "15-2026.03",
            uid = 1000,
            scriptCount = 1,
            systemApp = true,
            enabled = false,
            scripts = listOf(
                ScriptDefinition(
                    id = "settings_trace",
                    title = "设置页日志采样",
                    description = "记录关键页面进入日志，验证 Hook 时机。",
                    enabled = false,
                    content = """
                        function onActivityResumed(name)
                            log("settings resumed: " .. name)
                        end
                    """.trimIndent()
                )
            )
        )
    )

    val modules = listOf(
        DemoModule(
            id = "clipboard-plus",
            name = "Clipboard Plus",
            info = "增强剪贴板与历史记录管理",
            version = "1.2.4",
            author = "Kulipai",
            description = "提供剪贴板去重、敏感词清洗以及跨应用同步脚本入口。",
            enabled = true,
            canRun = true,
            hasSettings = true,
            hasConfig = true,
            files = listOf(
                ModuleFile("module.json", "{\n  \"id\": \"clipboard-plus\",\n  \"version\": \"1.2.4\"\n}"),
                ModuleFile("scripts", "", isDirectory = true),
                ModuleFile(
                    "scripts/main.lua",
                    """
                    local clipboard = require("clipboard")

                    function onCopy(text)
                        if #text > 1000 then
                            return text:sub(1, 1000)
                        end
                        return text
                    end

                    clipboard.register(onCopy)
                    """.trimIndent()
                ),
                ModuleFile("assets", "", isDirectory = true),
                ModuleFile("assets/config.json", "{\n  \"historyLimit\": 30,\n  \"sync\": true\n}")
            )
        ),
        DemoModule(
            id = "starter-pack",
            name = "Starter Pack",
            info = "脚本模板与公共函数集合",
            version = "0.9.1",
            author = "LuaHook Team",
            description = "内置通用日志、配置加载与快捷命令，适合作为新模块骨架。",
            enabled = true,
            canRun = true,
            hasSettings = false,
            hasConfig = true,
            files = listOf(
                ModuleFile("README.md", "# Starter Pack\n\n用于初始化 LuaHook 模块。"),
                ModuleFile("templates", "", isDirectory = true),
                ModuleFile(
                    "templates/default.lua",
                    """
                    local M = {}

                    function M.setup()
                        log("starter pack ready")
                    end

                    return M
                    """.trimIndent()
                )
            )
        ),
        DemoModule(
            id = "notify-cleaner",
            name = "Notify Cleaner",
            info = "通知过滤与静默规则",
            version = "2.0.0-beta",
            author = "Open Module Community",
            description = "为高频通知应用提供细粒度规则过滤，并支持自定义配置文件。",
            enabled = false,
            canRun = false,
            hasSettings = true,
            hasConfig = true,
            files = listOf(
                ModuleFile("module.json", "{\n  \"id\": \"notify-cleaner\",\n  \"beta\": true\n}"),
                ModuleFile(
                    "rules.lua",
                    """
                    local rules = {
                        package = "com.example.demo",
                        keywords = { "促销", "积分" },
                    }

                    return rules
                    """.trimIndent()
                )
            )
        )
    )

    val logEntries = listOf(
        LogEntry("16:03:11.152", "I", "LuaHook", "initialize runtime in shizuku mode"),
        LogEntry("16:03:11.338", "D", "ScriptLoader", "scan 3 apps and 4 scripts"),
        LogEntry("16:03:18.902", "I", "wechat_auto_reply", "rule matched: 状态"),
        LogEntry("16:03:20.122", "W", "ModuleEditor", "assets/config.json changed but not saved"),
        LogEntry("16:03:22.004", "E", "notify-cleaner", "failed to parse keyword rule at line 4"),
    )

    val editorSymbols = listOf(
        "{", "}", "(", ")", "[", "]", "=", "==", "~=", "\"", "'", ",", ".", ":", "local",
        "function", "return", "if", "then", "end"
    )

    fun findScriptApp(packageName: String): ScriptApp {
        return scriptApps.firstOrNull { it.packageName == packageName } ?: scriptApps.first()
    }

    fun findScript(packageName: String, scriptId: String): ScriptDefinition {
        return findScriptApp(packageName).scripts.firstOrNull { it.id == scriptId }
            ?: findScriptApp(packageName).scripts.first()
    }

    fun findModule(moduleId: String): DemoModule {
        return modules.firstOrNull { it.id == moduleId } ?: modules.first()
    }
}
