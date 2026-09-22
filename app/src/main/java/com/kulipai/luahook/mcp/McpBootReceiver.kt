package com.kulipai.luahook.mcp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kulipai.luahook.core.shell.ShellManager

class McpBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        ShellManager.init(context.applicationContext)
    }
}
