package com.kulipai.luahook.feature.logcat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.View
import android.view.ViewParent
import com.petterp.floatingx.core.FloatingX
import com.petterp.floatingx.system.container.FxWindowContainer

/**
 * 悬浮窗剪切板辅助器：
 * 解决 Android 10+（尤其是 Android 14/15/16/HyperOS）在悬浮窗覆盖在第三方应用上方时，
 * ClipboardService 校验 mWm.isUidFocused 导致剪切板写入被拦截的问题。
 */
object FloatingClipboardHelper {

    /**
     * 递归向上寻找 FloatingX 的系统级窗口容器 FxWindowContainer
     */
    fun findWindowContainer(view: View): FxWindowContainer? {
        var current: ViewParent? = view.parent
        while (current != null) {
            if (current is FxWindowContainer) return current
            current = current.parent
        }
        val floatControl = FloatingX.controlOrNull(FloatingLogcatController.LOGCAT_FLOATING_TAG)
        return floatControl?.contentView?.parent as? FxWindowContainer
            ?: floatControl?.holder?.view?.parent as? FxWindowContainer
    }

    /**
     * 在触控 ACTION_DOWN 时触发：确保窗口保持具备聚焦能力并请求焦点
     */
    fun prepareFocus(view: View) {
        val container = findWindowContainer(view)
        container?.setWindowFocusable(true)
        view.requestFocus()
    }

    /**
     * 执行剪切板写入：
     * 1. 确保容器当前处于可聚焦模式并请求焦点；
     * 2. 调用系统 ClipboardManager 写入纯文本；
     * 3. 在下一个 Looper 消息中再次确认写入（防御极高版本系统的异步焦点生效延迟）。
     */
    fun copy(view: View, text: String, onCopied: () -> Unit) {
        val container = findWindowContainer(view)
        container?.setWindowFocusable(true)
        view.requestFocus()

        fun performSetClip() {
            try {
                val context = view.context.applicationContext
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                val clip = ClipData.newPlainText("LuaHook Log", text)
                clipboard?.setPrimaryClip(clip)
            } catch (_: Exception) {
            }
        }

        // 立即执行第一次写入
        performSetClip()
        onCopied()

        // 在下一帧 post 中再次巩固写入，以应对部分厂商系统（HyperOS等）的异步焦点同步
        view.post {
            performSetClip()
        }
    }
}
