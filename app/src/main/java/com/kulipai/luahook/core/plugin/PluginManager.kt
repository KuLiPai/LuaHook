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

    fun isMcpEnabled(): Boolean {
        val plugin = mcpPlugin() ?: return false
        return plugin.enabled && plugin.entry == "mcp"
    }

    fun mcpPort(): Int {
        val port = mcpPlugin()?.port ?: DEFAULT_PORT
        return if (port in 1..65535) port else DEFAULT_PORT
    }

    fun onWorkspaceReady(context: Context) {
        apply(context)
    }

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
