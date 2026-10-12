package com.kulipai.luahook.ui.logcat

import com.kulipai.luahook.core.shell.ShellManager
import com.kulipai.luahook.core.shell.ShellResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 读取系统日志的工具类
 */

object LogcatDelegate {

    suspend fun getSystemLogsByTagSince(tag: String, since: String? = null): List<String> {
        return readSystemLogsByTagSince(tag, since).getOrDefault(emptyList()).toMutableList()
    }

    suspend fun readSystemLogsByTagSince(tag: String, since: String? = null): Result<List<String>> {
        val command = if (since.isNullOrEmpty()) {
            "logcat -d -v threadtime '$tag:*' '*:S'"
        } else {
            "logcat -d -v threadtime -T \"$since\" '$tag:*' '*:S'"
        }

        return withContext(Dispatchers.IO) {
            when (val result = ShellManager.shell(command)) {
                is ShellResult.Error -> {
                    Result.failure(IOException(result.stderr.ifBlank { "Failed to read logcat" }, result.throwable))
                }
                is ShellResult.Success -> {
                    Result.success(result.stdout.lineSequence()
                        .map { it.trimEnd() }
                        .filter { it.isNotBlank() && !it.startsWith("---------") }
                        .toList())
                }
            }
        }
    }

    fun getCurrentLogcatTimeFormat(): String {
        val dateFormat = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US)
        return dateFormat.format(Date())
    }
}
