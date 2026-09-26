package com.kulipai.luahook.mcp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/** Android 17 起，本机以外的局域网连接需要用户单独授权。 */
object McpNetworkAccess {
    const val PERMISSION: String = Manifest.permission.ACCESS_LOCAL_NETWORK

    /** 旧系统没有局域网运行时权限，视为已经允许。 */
    fun isGranted(context: Context): Boolean {
        return Build.VERSION.SDK_INT < 37 ||
            ContextCompat.checkSelfPermission(context, PERMISSION) == PackageManager.PERMISSION_GRANTED
    }

    fun needsPermission(context: Context): Boolean = !isGranted(context)
}
