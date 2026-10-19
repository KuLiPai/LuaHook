package com.kulipai.luahook.feature.mcp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kulipai.luahook.app.di.AppContainer

/** 开机后先把 ShellExecutor 拉起来，工作区就绪时再由 McpManager 决定要不要开 MCP。 */
class McpBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        AppContainer.instance.shellExecutor.init()
    }
}
