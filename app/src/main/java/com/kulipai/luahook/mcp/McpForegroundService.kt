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
import com.kulipai.luahook.ui.home.MainActivity

/**
 * MCP 启用后拉起的前台服务。每 15 秒检查一次，配置改了端口就重新绑定。
 */
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
        startInForeground(McpManager.mcpPort(this))
        ensureServer()
        handler.postDelayed(watchdog, 15_000)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startInForeground(McpManager.mcpPort(this))
        ensureServer()
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(watchdog)
        server?.stop()
        server = null
        boundPort = -1
        setListening(0)
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        startService(Intent(this, McpForegroundService::class.java))
        super.onTaskRemoved(rootIntent)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /** MCP 关掉就停服务。端口没变且还活着就不动。绑定失败把监听端口记成 0。 */
    private fun ensureServer() {
        if (!McpManager.isMcpEnabled(this)) {
            server?.stop()
            server = null
            boundPort = -1
            setListening(0)
            stopSelf()
            return
        }
        val port = McpManager.mcpPort(this)
        if (server?.isAlive == true && boundPort == port) return
        server?.stop()
        val next = McpHttpServer(port)
        try {
            next.start(5_000, false)
            server = next
            boundPort = port
            setListening(port)
            startInForeground(port)
        } catch (e: Exception) {
            next.stop()
            setListening(0)
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
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    companion object {
        private const val CHANNEL = "luahook_mcp"
        private const val NOTIFICATION_ID = 24555

        @Volatile
        var listeningPort: Int = 0
            private set

        /** 插件页用这个判断「运行中」还是「未启动」。 */
        fun isListening(): Boolean = listeningPort > 0

        private fun setListening(port: Int) {
            listeningPort = port
        }
    }
}
