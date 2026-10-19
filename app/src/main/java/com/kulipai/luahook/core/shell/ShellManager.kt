package com.kulipai.luahook.core.shell

import android.content.Context
import androidx.lifecycle.MutableLiveData
import com.kulipai.luahook.app.di.AppContainer

/**
 * ShellManager (绞杀者模式兼容桥梁)
 * 仅作为旧代码与 UI 层的适配层保留，新代码请使用 [ShellExecutor]。
 */
@Deprecated(
    message = "Use AppContainer.instance.shellExecutor instead",
    replaceWith = ReplaceWith("AppContainer.instance.shellExecutor")
)
object ShellManager {

    enum class Mode {
        ROOT, SHIZUKU, SHIZUKU_FALLBACK, NONE
    }

    val mode = MutableLiveData(Mode.NONE)

    fun setMode(newMode: Mode) {
        mode.value = newMode
    }

    private var customExecutor: ShellExecutor? = null

    // 在目标宿主应用（如 Xposed Hook 注入场景）无 AppContainer 时的 Standalone 兜底
    private val standaloneExecutor by lazy { StandaloneShellExecutor() }

    fun bindExecutor(executor: ShellExecutor) {
        customExecutor = executor
    }

    private fun getExecutor(): ShellExecutor {
        return AppContainer.instanceOrNull?.shellExecutor ?: customExecutor ?: standaloneExecutor
    }

    fun init(context: Context) {
        val executor = AppContainer.instanceOrNull?.shellExecutor ?: customExecutor
        executor?.init()
    }

    /**
     * 执行命令，返回 ShellResult
     */
    fun shell(cmd: String): ShellResult {
        return getExecutor().exec(cmd)
    }
}