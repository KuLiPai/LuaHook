package com.kulipai.luahook.mcp

import android.content.Context
import android.content.Intent
import androidx.core.content.edit
import com.kulipai.luahook.app.MyApplication
import com.kulipai.luahook.data.model.McpInfo

object McpManager {
    const val DEFAULT_PORT = 24555
    private const val PREFS_NAME = "settings"
    private const val KEY_MCP_ENABLED = "mcp_enabled"
    private const val KEY_MCP_PORT = "mcp_port"

    private fun getPreferences(context: Context = MyApplication.instance) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** 确保默认配置存在。 */
    fun ensureMcp(context: Context = MyApplication.instance) {
        val preferences = getPreferences(context)
        if (!preferences.contains(KEY_MCP_PORT)) {
            preferences.edit {
                putInt(KEY_MCP_PORT, DEFAULT_PORT)
            }
        }
    }

    /** 读取 MCP 配置。 */
    fun getMcpInfo(context: Context = MyApplication.instance): McpInfo {
        val preferences = getPreferences(context)
        return McpInfo(
            enabled = preferences.getBoolean(KEY_MCP_ENABLED, false),
            port = preferences.getInt(KEY_MCP_PORT, DEFAULT_PORT),
        )
    }

    /** 检查 MCP 是否启用。 */
    fun isMcpEnabled(context: Context = MyApplication.instance): Boolean {
        return getMcpInfo(context).enabled
    }

    /** 端口必须在 1 到 65535，否则退回 24555。 */
    fun mcpPort(context: Context = MyApplication.instance): Int {
        val port = getMcpInfo(context).port
        return if (port in 1..65535) port else DEFAULT_PORT
    }

    /** 改开关和端口，端口非法直接失败。写完后返回是否成功。 */
    fun update(enabled: Boolean, port: Int, context: Context = MyApplication.instance): Boolean {
        if (port !in 1..65535) return false
        getPreferences(context).edit {
            putBoolean(KEY_MCP_ENABLED, enabled)
            putInt(KEY_MCP_PORT, port)
        }
        return true
    }

    /** Shell 和工作区都准备好之后再启动 MCP。 */
    fun onWorkspaceReady(context: Context = MyApplication.instance) {
        apply(context)
    }

    /** 按当前配置启动或停掉前台服务。 */
    fun apply(context: Context = MyApplication.instance) {
        val intent = Intent(context, McpForegroundService::class.java)
        if (!isMcpEnabled(context)) {
            context.stopService(intent)
            return
        }
        try {
            context.startForegroundService(intent)
        } catch (_: Exception) {
            context.startService(intent)
        }
    }
}