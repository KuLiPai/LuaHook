package com.kulipai.luahook.mcp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import android.util.Log
import com.kulipai.luahook.R
import com.kulipai.luahook.core.plugin.PluginManager
import com.kulipai.luahook.ui.home.MainActivity

class McpForegroundService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private var server: McpHttpServer? = null
    private var boundPort = -1
    private val watchdog = object : Runnable {
        override fun run() {
            ensureServer()
            handler.postDelayed(this, 15_000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        startInForeground(PluginManager.mcpPort())
        ensureServer()
        handler.postDelayed(watchdog, 15_000)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startInForeground(PluginManager.mcpPort())
        ensureServer()
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(watchdog)
        server?.stop()
        server = null
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        startService(Intent(this, McpForegroundService::class.java))
        super.onTaskRemoved(rootIntent)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun ensureServer() {
        if (!PluginManager.isMcpEnabled()) {
            server?.stop()
            server = null
            boundPort = -1
            stopSelf()
            return
        }
        val port = PluginManager.mcpPort()
        if (server?.isAlive == true && boundPort == port) return
        server?.stop()
        val next = McpHttpServer(port)
        try {
            next.start(5_000, false)
            server = next
            boundPort = port
            startInForeground(port)
        } catch (e: Exception) {
            next.stop()
            Log.e("LuaXposed", "mcp bind $port", e)
        }
    }

    private fun startInForeground(port: Int) {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL, "LuaHook MCP", NotificationManager.IMPORTANCE_LOW)
            )
        }
        val pending = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("LuaHook MCP")
            .setContentText("0.0.0.0:$port")
            .setContentIntent(pending)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    companion object {
        private const val CHANNEL = "luahook_mcp"
        private const val NOTIFICATION_ID = 24555
    }
}
