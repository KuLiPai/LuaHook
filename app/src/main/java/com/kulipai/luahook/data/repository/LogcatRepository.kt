package com.kulipai.luahook.data.repository

import com.kulipai.luahook.core.shell.ShellExecutor
import com.kulipai.luahook.core.shell.ShellResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

interface LogcatRepository {
    suspend fun getSystemLogsByTagSince(tag: String, since: String? = null): List<String>
    suspend fun readSystemLogsByTagSince(tag: String, since: String? = null): Result<List<String>>
    fun getCurrentLogcatTimeFormat(): String
}

class LogcatRepositoryImpl(
    private val shellExecutor: ShellExecutor,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : LogcatRepository {

    override suspend fun getSystemLogsByTagSince(tag: String, since: String?): List<String> {
        return readSystemLogsByTagSince(tag, since).getOrDefault(emptyList()).toMutableList()
    }

    override suspend fun readSystemLogsByTagSince(tag: String, since: String?): Result<List<String>> {
        val command = if (since.isNullOrEmpty()) {
            "logcat -d -v threadtime '$tag:*' '*:S'"
        } else {
            "logcat -d -v threadtime -T \"$since\" '$tag:*' '*:S'"
        }

        return withContext(ioDispatcher) {
            when (val result = shellExecutor.exec(command)) {
                is ShellResult.Error -> {
                    Result.failure(IOException(result.stderr.ifBlank { "Failed to read logcat" }, result.throwable))
                }
                is ShellResult.Success -> {
                    Result.success(
                        result.stdout.lineSequence()
                            .map { it.trimEnd() }
                            .filter { it.isNotBlank() && !it.startsWith("---------") }
                            .toList()
                    )
                }
            }
        }
    }

    override fun getCurrentLogcatTimeFormat(): String {
        val dateFormat = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US)
        return dateFormat.format(Date())
    }
}
