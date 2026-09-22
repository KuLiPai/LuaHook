package com.kulipai.luahook.core.plugin

import android.content.Context
import android.content.Intent
import com.kulipai.luahook.core.file.WorkspaceFileManager
import com.kulipai.luahook.mcp.McpForegroundService
import org.luaj.LuaValue
import org.luaj.lib.jse.JsePlatform

data class PluginInfo(
    val id: String,
    val name: String,
    val description: String,
    val enabled: Boolean,
    val entry: String,
    val port: Int,
)

object PluginManager {
    const val DEFAULT_PORT = 24555
    private const val MCP_ID = "mcp"

    /** 没有 /Plugin/mcp/init.lua 时写一份默认配置：启用，端口 24555，entry 必须是 mcp。 */
    fun ensureMcpPlugin() {
        val relative = "${WorkspaceFileManager.Plugin}/$MCP_ID/init.lua"
        val existing = WorkspaceFileManager.read(relative)
        if (existing.isNotBlank()) return
        WorkspaceFileManager.ensureDirectoryExists(
            "${WorkspaceFileManager.DIR}${WorkspaceFileManager.Plugin}/$MCP_ID"
        )
        WorkspaceFileManager.write(
            relative,
            """
            name = "mcp"
            description = "LuaHook MCP"
            enabled = true
            -- 1-65535，改完后重新打开应用即换端口
            port = 24555
            entry = "mcp"
            """.trimIndent()
        )
    }

    /** 用 Lua 执行 init.lua，读出名称、开关和端口。文件空或语法坏就当没有这个插件。 */
    fun mcpPlugin(): PluginInfo? {
        val text = WorkspaceFileManager.read("${WorkspaceFileManager.Plugin}/$MCP_ID/init.lua")
        if (text.isBlank()) return null
        return try {
            val globals = JsePlatform.standardGlobals()
            globals.load(text).call()
            PluginInfo(
                id = MCP_ID,
                name = globals.get("name").optjstring("mcp"),
                description = globals.get("description").optjstring(""),
                enabled = globals.get("enabled").optboolean(false),
                entry = globals.get("entry").optjstring(""),
                port = globals.get("port").optint(DEFAULT_PORT),
            )
        } catch (_: Exception) {
            null
        }
    }

    /** 只有 enabled 且 entry 为 mcp 才启动服务。 */
    fun isMcpEnabled(): Boolean {
        val plugin = mcpPlugin() ?: return false
        return plugin.enabled && plugin.entry == "mcp"
    }

    /** 端口必须在 1 到 65535，否则退回 24555。 */
    fun mcpPort(): Int {
        val port = mcpPlugin()?.port ?: DEFAULT_PORT
        return if (port in 1..65535) port else DEFAULT_PORT
    }

    /** Shell 和工作区都准备好之后再启动 MCP，避免插件文件还没读到就判断未启用。 */
    fun onWorkspaceReady(context: Context) {
        apply(context)
    }

    /** 改开关和端口，写回 init.lua。端口非法直接失败。 */
    fun update(enabled: Boolean, port: Int): Boolean {
        if (port !in 1..65535) return false
        val current = mcpPlugin()
        val name = luaString(current?.name ?: "mcp")
        val description = luaString(current?.description ?: "LuaHook MCP")
        WorkspaceFileManager.ensureDirectoryExists(
            "${WorkspaceFileManager.DIR}${WorkspaceFileManager.Plugin}/$MCP_ID"
        )
        return WorkspaceFileManager.write(
            "${WorkspaceFileManager.Plugin}/$MCP_ID/init.lua",
            """
            name = "$name"
            description = "$description"
            enabled = $enabled
            port = $port
            entry = "mcp"
            """.trimIndent()
        )
    }

    /** 按当前配置启动或停掉前台服务。 */
    fun apply(context: Context) {
        val intent = Intent(context, McpForegroundService::class.java)
        if (!isMcpEnabled()) {
            context.stopService(intent)
            return
        }
        try {
            context.startForegroundService(intent)
        } catch (_: Exception) {
            context.startService(intent)
        }
    }

    private fun luaString(value: String): String {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ")
    }
}

private fun LuaValue.optboolean(default: Boolean): Boolean {
    return if (isnil()) default else if (isboolean()) toboolean() else default
}

private fun LuaValue.optint(default: Int): Int {
    return if (isnumber()) toint() else default
}
