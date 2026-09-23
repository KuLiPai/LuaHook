package com.kulipai.luahook.core.shell

import android.content.Context
import androidx.lifecycle.MutableLiveData
import com.kulipai.luahook.core.file.WorkspaceFileManager
import com.kulipai.luahook.mcp.McpManager
import com.kulipai.luahook.core.shizuku.ShizukuApi
import com.kulipai.luahook.core.shizuku.ShizukuShellFallback
import com.topjohnwu.superuser.Shell


/**
 * ShellManager
 */

object ShellManager {

    enum class Mode {
        ROOT, SHIZUKU, SHIZUKU_FALLBACK, NONE
    }


    var mode = MutableLiveData(Mode.NONE)
//    var mode: LiveData<Mode> = mode

    fun setMode(newMode: Mode) {
        mode.value = newMode
    }


    private var rootShell: Shell? = null

    fun init(context: Context) {

        // MOUNT_MASTER 标志
        try {
            Shell.setDefaultBuilder(
                Shell.Builder.create()
                    .setFlags(Shell.FLAG_MOUNT_MASTER)
            )
        }catch (_: Exception) {

        }


        Shell.getShell {
            if (it.isRoot) {
                rootShell = it
                setMode(Mode.ROOT)
                WorkspaceFileManager.init(context)
                McpManager.onWorkspaceReady(context)

            } else {
                // try shizuku
                observeShizuku(context)
            }
        }

    }

    private fun observeShizuku(context: Context) {
        ShizukuApi.isBinderAvailable.observeForever {
            if (it == true) {
                if (ShizukuApi.isPermissionGranted.value == true) {
                    bindShizukuWithFallback(context)
                } else {
                    ShizukuApi.requestShizuku()
                }
            }
        }

        ShizukuApi.isPermissionGranted.observeForever {
            if (it == true) {
                bindShizukuWithFallback(context)
            }
        }
    }

    private fun bindShizukuWithFallback(context: Context) {
        if (ShizukuApi.isServiceConnected.value != true) {
            setMode(Mode.SHIZUKU_FALLBACK)
            WorkspaceFileManager.init(context)
            McpManager.onWorkspaceReady(context)
        }
        ShizukuApi.bindShizuku(context)
    }




    /**
     * 执行命令，返回 (输出, 是否成功)
     */
    fun shell(cmd: String): ShellResult {
        return when (mode.value) {
            Mode.ROOT -> {
                try {
                    val result = Shell.cmd(cmd).exec()
                    if (result.isSuccess) {
                        ShellResult.Success(result.out.joinToString("\n"))
                    } else {
                        ShellResult.Error(result.err.joinToString("\n"))
                    }
                } catch (e: Exception) {
                    ShellResult.Error("Exception occurred", e)
                }
            }

            Mode.SHIZUKU -> {
                ShizukuApi.execShell(cmd)
            }

            Mode.SHIZUKU_FALLBACK -> {
                val result = ShizukuShellFallback.exec(cmd)
                if (result.second) {
                    ShellResult.Success(result.first)
                } else {
                    ShellResult.Error(result.first)
                }
            }

            else -> {
                ShellResult.Error("No shell method available")
            }
        }
    }

}