package com.kulipai.luahook.data.repository

import com.kulipai.luahook.core.file.WorkspaceFileManager
import com.kulipai.luahook.core.file.WorkspaceStorage
import com.kulipai.luahook.core.shell.ShellExecutor
import com.kulipai.luahook.core.shell.ShellMode
import com.kulipai.luahook.core.shell.ShellResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class FakeShellExecutor : ShellExecutor {
    val executedCommands = mutableListOf<String>()
    var nextResult: ShellResult = ShellResult.Success("")
    private val _mode = MutableStateFlow(ShellMode.ROOT)
    override val mode: StateFlow<ShellMode> = _mode

    override fun init() {}

    override fun exec(cmd: String): ShellResult {
        executedCommands.add(cmd)
        return nextResult
    }
}

class FakeWorkspaceStorage(private val shellExecutor: ShellExecutor) : WorkspaceStorage {
    val files = mutableMapOf<String, String>()
    val maps = mutableMapOf<String, MutableMap<String, Any?>>()
    val directories = mutableSetOf<String>()

    override fun read(relativePath: String): String {
        val norm = if (relativePath.startsWith("/")) relativePath else "/$relativePath"
        return files[norm] ?: ""
    }

    override fun write(file: String, content: String): Boolean {
        val norm = if (file.startsWith("/")) file else "/$file"
        files[norm] = content
        return true
    }

    override fun writeMap(file: String, data: MutableMap<String, Any?>): Boolean {
        val norm = if (file.startsWith("/")) file else "/$file"
        maps[norm] = HashMap(data)
        return true
    }

    override fun readMap(file: String): MutableMap<String, Any?> {
        val norm = if (file.startsWith("/")) file else "/$file"
        return maps[norm]?.let { HashMap(it) } ?: mutableMapOf()
    }

    override fun writeStringList(path: String, list: List<String>) {
        write(path, list.joinToString(","))
    }

    override fun readStringList(path: String): MutableList<String> {
        val s = read(path)
        return if (s.isEmpty()) mutableListOf() else s.split(",").toMutableList()
    }

    override fun rm(file: String): Boolean {
        val norm = if (file.startsWith("/")) file else "/$file"
        files.remove(norm)
        maps.remove(norm)
        directories.remove(norm)
        return true
    }

    override fun init(context: android.content.Context) {}

    override fun directoryExists(path: String): Boolean = directories.contains(path)

    override fun ensureDirectoryExists(path: String): Boolean {
        directories.add(path)
        return true
    }

    override fun ensureReadable(path: String) {}

    override fun copy(srcPath: String, destPath: String): Boolean = true

    override fun move(srcPath: String, destPath: String): Boolean = true

    override fun unzip(zipPath: String, destDir: String): Boolean = true

    override fun listFiles(dirPath: String): List<String> = emptyList()

    override fun delete(path: String): Boolean = rm(path)

    override fun chmod(path: String, mode: String, recursive: Boolean): Boolean = true

    override fun parseParameters(fileContent: String): WorkspaceFileManager.FileParameters? {
        val lines = fileContent.split("\n")
        val parsed = mutableMapOf<String, String>()
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("--")) {
                val content = trimmed.substring(2).trim()
                val idx = content.indexOf(":")
                if (idx != -1) {
                    val k = content.substring(0, idx).trim()
                    val v = content.substring(idx + 1).trim()
                    parsed[k] = v
                } else {
                    break
                }
            } else if (trimmed.isEmpty()) {
                continue
            } else {
                break
            }
        }
        return WorkspaceFileManager.FileParameters(
            name = parsed["name"],
            descript = parsed["descript"],
            packageName = parsed["package"],
            author = parsed["author"]
        )
    }
}

class ScriptRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var shellExecutor: FakeShellExecutor
    private lateinit var workspaceStorage: FakeWorkspaceStorage
    private lateinit var repository: ScriptRepositoryImpl

    @Before
    fun setUp() {
        shellExecutor = FakeShellExecutor()
        workspaceStorage = FakeWorkspaceStorage(shellExecutor)
        repository = ScriptRepositoryImpl(
            workspaceStorage = workspaceStorage,
            shellExecutor = shellExecutor,
            ioDispatcher = Dispatchers.Unconfined
        )
    }

    @Test
    fun `readGlobalScript and saveGlobalScript work properly`() = runBlocking {
        val initial = repository.readGlobalScript()
        assertEquals("", initial)

        val success = repository.saveGlobalScript("print('hello global')")
        assertTrue(success)
        assertEquals("print('hello global')", repository.readGlobalScript())
    }

    @Test
    fun `readConf transforms legacy boolean entries to JSONArray`() = runBlocking {
        val pkg = "com.test.app"
        val confPath = "${WorkspaceFileManager.AppConf}/$pkg.txt"
        val map = mutableMapOf<String, Any?>(
            "scriptA" to true,
            "scriptB" to JSONArray(arrayOf<Any>(false, "desc B", "v1.0"))
        )
        workspaceStorage.writeMap(confPath, map)

        val confList = repository.readConf(pkg)
        assertEquals(2, confList.size)

        val entryA = confList.first { it.key == "scriptA" }
        assertTrue(entryA.value is JSONArray)
        val arrA = entryA.value as JSONArray
        assertEquals(true, arrA.getBoolean(0))
        assertEquals("", arrA.getString(1))

        val entryB = confList.first { it.key == "scriptB" }
        val arrB = entryB.value as JSONArray
        assertEquals(false, arrB.getBoolean(0))
        assertEquals("desc B", arrB.getString(1))
    }

    @Test
    fun `writeScriptConfig adds entry and creates directory`() = runBlocking {
        val pkg = "com.test.app"
        repository.writeScriptConfig(pkg, "hook1", "My Hook 1")

        val confList = repository.readConf(pkg)
        assertEquals(1, confList.size)
        assertEquals("hook1", confList[0].key)
        assertTrue(confList[0].value is JSONArray)
        val arr = confList[0].value as JSONArray
        assertEquals(true, arr.getBoolean(0))
        assertEquals("My Hook 1", arr.getString(1))

        assertTrue(workspaceStorage.directories.contains("${WorkspaceFileManager.DIR}${WorkspaceFileManager.AppScript}/$pkg"))
    }

    @Test
    fun `createAppScript creates config and script file`() = runBlocking {
        val pkg = "com.test.app"
        val created = repository.createAppScript(pkg, "hook1", "Description", "-- Custom hook")
        assertTrue(created)

        val content = repository.readAppScript(pkg, "hook1")
        assertEquals("-- Custom hook", content)

        val scripts = repository.getAppScripts(pkg)
        assertEquals(1, scripts.size)
        assertEquals("hook1", scripts[0].name)
        assertTrue(scripts[0].enabled)
        assertEquals("Description", scripts[0].description)
    }

    @Test
    fun `setAppScriptEnabled updates enabled state in config`() = runBlocking {
        val pkg = "com.test.app"
        repository.createAppScript(pkg, "hook1", "Description", "-- code")
        repository.setAppScriptEnabled(pkg, "hook1", false)

        val scripts = repository.getAppScripts(pkg)
        assertFalse(scripts[0].enabled)
        assertEquals("Description", scripts[0].description)

        repository.setAppScriptEnabled(pkg, "hook1", true)
        val scriptsAfter = repository.getAppScripts(pkg)
        assertTrue(scriptsAfter[0].enabled)
    }

    @Test
    fun `deleteAppScript removes file and config entry`() = runBlocking {
        val pkg = "com.test.app"
        repository.createAppScript(pkg, "hook1", "Desc", "-- code")
        assertEquals(1, repository.getAppScripts(pkg).size)

        val deleted = repository.deleteAppScript(pkg, "hook1")
        assertTrue(deleted)
        assertEquals(0, repository.getAppScripts(pkg).size)
        assertEquals("", repository.readAppScript(pkg, "hook1"))
    }

    @Test
    fun `forceStopApp executes am force-stop command`() = runBlocking {
        val pkg = "com.test.app"
        val success = repository.forceStopApp(pkg)
        assertTrue(success)
        assertEquals("am force-stop com.test.app", shellExecutor.executedCommands.last())
    }

    @Test
    fun `prepareShareScript creates destination file with metadata header`() = runBlocking {
        val pkg = "com.test.app"
        repository.saveAppScript(pkg, "myScript", "print('share test')")

        val cacheDir = tempFolder.newFolder("cache")
        val file = repository.prepareShareScript(
            cacheDir = cacheDir,
            packageName = pkg,
            scriptName = "myScript",
            description = "Test Desc",
            author = "KuLiPai"
        )

        assertNotNull(file)
        assertTrue(file!!.exists())
        val text = file.readText()
        assertTrue(text.contains("-- name: myScript"))
        assertTrue(text.contains("-- descript: Test Desc"))
        assertTrue(text.contains("-- package: com.test.app"))
        assertTrue(text.contains("-- author: KuLiPai"))
        assertTrue(text.contains("print('share test')"))
    }

    @Test
    fun `parseParameters parses valid metadata header`() = runBlocking {
        val lua = """
            -- name: my_hook
            -- descript: my description
            -- package: com.foo.bar
            -- author: Alice

            print("hello")
        """.trimIndent()

        val params = repository.parseParameters(lua)
        assertNotNull(params)
        assertEquals("my_hook", params?.name)
        assertEquals("my description", params?.descript)
        assertEquals("com.foo.bar", params?.packageName)
        assertEquals("Alice", params?.author)
    }
}
