package com.kulipai.luahook.core.file

import android.content.Context
import android.util.Base64
import com.kulipai.luahook.core.log.d
import com.kulipai.luahook.core.shell.ShellExecutor
import com.kulipai.luahook.core.shell.ShellResult
import com.kulipai.luahook.core.utils.dd
import com.topjohnwu.superuser.Shell
import org.json.JSONObject
import java.io.File

interface WorkspaceStorage {
    fun read(relativePath: String): String
    fun write(file: String, content: String): Boolean
    fun writeMap(file: String, data: MutableMap<String, Any?>): Boolean
    fun readMap(file: String): MutableMap<String, Any?>
    fun writeStringList(path: String, list: List<String>)
    fun readStringList(path: String): MutableList<String>
    fun rm(file: String): Boolean
    fun init(context: Context)
    fun directoryExists(path: String): Boolean
    fun ensureDirectoryExists(path: String): Boolean
    fun ensureReadable(path: String)
    fun parseParameters(fileContent: String): WorkspaceFileManager.FileParameters?
}

class WorkspaceStorageImpl(
    private val shellExecutor: ShellExecutor
) : WorkspaceStorage {

    override fun read(relativePath: String): String {
        try {
            val fullPath = WorkspaceFileManager.DIR + relativePath
            val file = File(fullPath)
            // Host app domains (system_app / untrusted_app / system_server) are
            // often denied search/open on /data/local/tmp (shell_data_file) even
            // when DAC mode is 666. File.exists/canRead then fail and the script
            // would silently become "". Prefer root cat before giving up.
            if (file.exists()) {
                try {
                    if (file.canRead()) {
                        val text = file.readText()
                        if (text.isNotEmpty() || file.length() == 0L) {
                            return text
                        }
                    }
                } catch (_: Exception) {
                }
            }

            when (val result = shellExecutor.exec("cat \"$fullPath\"")) {
                is ShellResult.Success -> return result.stdout
                is ShellResult.Error -> Unit
            }
            return readViaRoot(fullPath) ?: ""
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }

    /**
     * Read a workspace file with root. Used from hooked host processes where
     * SELinux blocks direct access to [DIR] under /data/local/tmp.
     */
    private fun readViaRoot(absolutePath: String): String? {
        try {
            val result = Shell.cmd("cat \"$absolutePath\"").exec()
            if (result.isSuccess && result.out.isNotEmpty()) {
                return result.out.joinToString("\n")
            }
        } catch (_: Exception) {
            // Fall through to Runtime.exec("su").
        }
        return try {
            val process = ProcessBuilder("su", "-c", "cat \"$absolutePath\"")
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            if (process.waitFor() == 0 && output.isNotEmpty()) output else null
        } catch (_: Exception) {
            null
        }
    }

    override fun write(file: String, content: String): Boolean {
        val path = "${WorkspaceFileManager.DIR}$file"

        val base64Content = Base64.encodeToString(
            content.toByteArray(Charsets.UTF_8),
            Base64.NO_WRAP
        )

        val script = """
        rm -f "$path"
        touch "$path"
        echo "$base64Content" | base64 -d > "$path"
        chmod 666 "$path"
    """.trimIndent()

        val result = shellExecutor.exec(script)
        return when (result) {
            is ShellResult.Error -> false
            is ShellResult.Success -> {
                WorkspaceEventManager.notifyFileChanged(file)
                true
            }
        }
    }

    override fun writeMap(file: String, data: MutableMap<String, Any?>): Boolean {
        val jsonObject = JSONObject(data)
        val jsonString = jsonObject.toString()
        return write(file, jsonString)
    }

    override fun readMap(file: String): MutableMap<String, Any?> {
        return try {
            val jsonString = read(file)
            if (jsonString.isEmpty()) {
                return mutableMapOf()
            }
            val jsonObject = JSONObject(jsonString)
            val map = mutableMapOf<String, Any?>()
            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                map[key] = jsonObject.get(key)
            }
            map
        } catch (e: Exception) {
            ("Error decoding arbitrary object from $file: ${e.message}").d()
            mutableMapOf()
        }
    }

    override fun writeStringList(path: String, list: List<String>) {
        write(path, list.joinToString(","))
    }

    override fun readStringList(path: String): MutableList<String> {
        val serialized = read(path)
        return if (serialized.isNotEmpty()) {
            serialized.split(",").toMutableList()
        } else {
            mutableListOf()
        }
    }

    override fun rm(file: String): Boolean {
        val path = "${WorkspaceFileManager.DIR}$file"
        val script = "rm -rf \"$path\""
        val result = shellExecutor.exec(script)
        result.dd()
        return when (result) {
            is ShellResult.Error -> false
            is ShellResult.Success -> {
                WorkspaceEventManager.notifyFileChanged(file)
                true
            }
        }
    }

    override fun init(context: Context) {
        ensureDirectoryExists(WorkspaceFileManager.DIR)
        ensureDirectoryExists(WorkspaceFileManager.DIR + WorkspaceFileManager.Project)
        ensureDirectoryExists(WorkspaceFileManager.DIR + WorkspaceFileManager.AppConf)
        ensureDirectoryExists(WorkspaceFileManager.DIR + WorkspaceFileManager.AppScript)
        ensureDirectoryExists(WorkspaceFileManager.DIR + WorkspaceFileManager.Plugin)
    }

    override fun directoryExists(path: String): Boolean {
        val result = shellExecutor.exec("ls \"$path\"")
        return when (result) {
            is ShellResult.Error -> false
            is ShellResult.Success -> true
        }
    }

    override fun ensureDirectoryExists(path: String): Boolean {
        if (directoryExists(path)) {
            return true
        }

        val result = shellExecutor.exec("mkdir -p \"$path\"\nchmod 777 \"$path\"")
        return when (result) {
            is ShellResult.Error -> false
            is ShellResult.Success -> true
        }
    }

    override fun ensureReadable(path: String) {
        shellExecutor.exec("chmod 666 \"$path\"")
    }

    override fun parseParameters(fileContent: String): WorkspaceFileManager.FileParameters? {
        try {
            val lines = fileContent.split("\n")
            val parsedParams = mutableMapOf<String, String>()

            for (line in lines) {
                val trimmedLine = line.trim()

                if (trimmedLine.startsWith("--")) {
                    val content = trimmedLine.substring(2).trim()
                    val colonIndex = content.indexOf(":")

                    if (colonIndex != -1) {
                        val key = content.substring(0, colonIndex).trim()
                        val value = content.substring(colonIndex + 1).trim()
                        parsedParams[key] = value
                    } else {
                        break
                    }
                } else if (trimmedLine.isEmpty()) {
                    continue
                } else {
                    break
                }
            }

            return WorkspaceFileManager.FileParameters(
                name = parsedParams["name"],
                descript = parsedParams["descript"],
                packageName = parsedParams["package"],
                author = parsedParams["author"],
                otherParams = parsedParams.filterKeys {
                    it != "name" && it != "descript" && it != "package" && it != "author"
                }
            )
        } catch (_: Exception) {
        }
        return null
    }
}
