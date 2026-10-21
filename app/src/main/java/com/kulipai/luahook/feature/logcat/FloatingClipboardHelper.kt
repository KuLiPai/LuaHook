package com.kulipai.luahook.feature.logcat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.View

/**
 * 悬浮窗剪切板辅助器：
 * 纯粹的剪切板复制辅助，绝不抢占系统焦点，避免影响宿主应用及系统返回手势。
 */
object FloatingClipboardHelper {

    /**
     * 执行剪切板写入：使用 applicationContext 执行标准 setPrimaryClip，
     * 绝不篡改 WindowManager 的 Focusable 属性，保障系统返回按键与手势完全畅通。
     */
    fun copy(view: View, text: String, onCopied: () -> Unit) {
        try {
            val context = view.context.applicationContext
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = ClipData.newPlainText("LuaHook Log", text)
            clipboard?.setPrimaryClip(clip)
            onCopied()
        } catch (_: Exception) {
            // 系统安全限制时静默处理，绝不强行夺取焦点
        }
    }
}
