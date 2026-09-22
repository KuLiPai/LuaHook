package com.kulipai.luahook.mcp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kulipai.luahook.core.shell.ShellManager

/** 开机后先把 ShellManager 拉起来，工作区就绪时再由 PluginManager 决定要不要开 MCP。 */
class McpBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        ShellManager.init(context.applicationContext)
    }
}
