package com.kulipai.luahook.core.shell

import android.content.Context
import com.kulipai.luahook.core.shizuku.ShizukuApi
import com.kulipai.luahook.core.shizuku.ShizukuShellFallback
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

enum class ShellMode {
    ROOT, SHIZUKU, SHIZUKU_FALLBACK, NONE
}

interface ShellExecutor {
    val mode: StateFlow<ShellMode>
    fun init()
    fun exec(command: String): ShellResult
}

class ShellExecutorImpl(
    private val context: Context,
    private val scope: CoroutineScope
) : ShellExecutor {

    private val _mode = MutableStateFlow(ShellMode.NONE)
    override val mode: StateFlow<ShellMode> = _mode.asStateFlow()

    private var rootShell: Shell? = null

    override fun init() {
        try {
            Shell.setDefaultBuilder(
                Shell.Builder.create()
                    .setFlags(Shell.FLAG_MOUNT_MASTER)
            )
        } catch (_: Exception) {
        }

        Shell.getShell { shell ->
            if (shell.isRoot) {
                rootShell = shell
                _mode.value = ShellMode.ROOT
            } else {
                observeShizuku()
            }
        }
    }

    private fun observeShizuku() {
        // 观察 binder 与 permission 状态流，自动请求权限或发起绑定
        scope.launch {
            combine(
                ShizukuApi.isBinderAvailableFlow,
                ShizukuApi.isPermissionGrantedFlow
            ) { binderAvailable, permissionGranted ->
                binderAvailable to permissionGranted
            }.collectLatest { (binderAvailable, permissionGranted) ->
                if (!binderAvailable) {
                    if (_mode.value != ShellMode.ROOT) {
                        _mode.value = ShellMode.NONE
                    }
                    return@collectLatest
                }
                if (permissionGranted) {
                    bindShizukuWithFallback()
                } else {
                    ShizukuApi.requestShizuku()
                }
            }
        }

        // 观察 Shizuku 服务连接状态流
        scope.launch {
            ShizukuApi.isServiceConnectedFlow.collectLatest { isConnected ->
                if (isConnected) {
                    _mode.value = ShellMode.SHIZUKU
                } else if (_mode.value == ShellMode.SHIZUKU) {
                    // 若服务断开连接，退回 FALLBACK 或 NONE
                    _mode.value = if (ShizukuApi.isPermissionGrantedFlow.value) {
                        ShellMode.SHIZUKU_FALLBACK
                    } else {
                        ShellMode.NONE
                    }
                }
            }
        }
    }

    private fun bindShizukuWithFallback() {
        if (!ShizukuApi.isServiceConnectedFlow.value) {
            _mode.value = ShellMode.SHIZUKU_FALLBACK
        }
        ShizukuApi.bindShizuku(context)
    }

    override fun exec(command: String): ShellResult {
        return when (mode.value) {
            ShellMode.ROOT -> {
                try {
                    val result = Shell.cmd(command).exec()
                    if (result.isSuccess) {
                        ShellResult.Success(result.out.joinToString("\n"))
                    } else {
                        ShellResult.Error(result.err.joinToString("\n"))
                    }
                } catch (e: Exception) {
                    ShellResult.Error("Exception occurred", e)
                }
            }

            ShellMode.SHIZUKU -> {
                ShizukuApi.execShell(command)
            }

            ShellMode.SHIZUKU_FALLBACK -> {
                val result = ShizukuShellFallback.exec(command)
                if (result.second) {
                    ShellResult.Success(result.first)
                } else {
                    ShellResult.Error(result.first)
                }
            }

            ShellMode.NONE -> {
                ShellResult.Error("No shell method available")
            }
        }
    }
}

/**
 * 在目标宿主应用（如 Xposed Hook 注入场景）中兜底执行的 Standalone 执行器
 */
class StandaloneShellExecutor : ShellExecutor {
    private val _mode = MutableStateFlow(ShellMode.NONE)
    override val mode: StateFlow<ShellMode> = _mode.asStateFlow()

    override fun init() {
        // 宿主进程中无需主动初始化
    }

    override fun exec(command: String): ShellResult {
        return try {
            val result = Shell.cmd(command).exec()
            if (result.isSuccess) {
                ShellResult.Success(result.out.joinToString("\n"))
            } else {
                ShellResult.Error(result.err.joinToString("\n"))
            }
        } catch (e: Exception) {
            try {
                val process = ProcessBuilder("su", "-c", command)
                    .redirectErrorStream(true)
                    .start()
                val output = process.inputStream.bufferedReader().use { it.readText() }
                if (process.waitFor() == 0) {
                    ShellResult.Success(output)
                } else {
                    ShellResult.Error(output)
                }
            } catch (t: Throwable) {
                ShellResult.Error("Standalone execution failed", t)
            }
        }
    }
}
