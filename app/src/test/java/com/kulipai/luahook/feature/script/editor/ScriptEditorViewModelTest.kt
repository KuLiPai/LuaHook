package com.kulipai.luahook.feature.script.editor

import com.kulipai.luahook.core.file.WorkspaceEventManager
import com.kulipai.luahook.core.file.WorkspaceFileManager
import com.kulipai.luahook.data.repository.AppScriptConfig
import com.kulipai.luahook.data.repository.ScriptRepository
import com.kulipai.luahook.feature.script.editor.ScriptEditorEffect
import com.kulipai.luahook.feature.script.editor.ScriptEditorMode
import com.kulipai.luahook.feature.script.editor.ScriptEditorViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class FakeScriptRepository : ScriptRepository {
    val scripts = mutableMapOf<String, String>()
    val stoppedApps = mutableListOf<String>()

    override suspend fun readGlobalScript(): String = scripts["/global.lua"].orEmpty()

    override suspend fun saveGlobalScript(content: String): Boolean {
        scripts["/global.lua"] = content
        return true
    }

    override suspend fun readConf(packageName: String): MutableList<MutableMap.MutableEntry<String, Any?>> {
        return mutableListOf()
    }

    override suspend fun writeScriptConfig(packageName: String, name: String, description: String) {}

    override suspend fun getAppScripts(packageName: String): List<AppScriptConfig> = emptyList()

    override suspend fun readAppScript(packageName: String, name: String): String {
        return scripts["/AppScript/$packageName/$name.lua"].orEmpty()
    }

    override suspend fun saveAppScript(packageName: String, name: String, content: String): Boolean {
        scripts["/AppScript/$packageName/$name.lua"] = content
        return true
    }

    override suspend fun createAppScript(
        packageName: String,
        name: String,
        description: String,
        content: String?
    ): Boolean {
        scripts["/AppScript/$packageName/$name.lua"] = content ?: ""
        return true
    }

    override suspend fun deleteAppScript(packageName: String, name: String): Boolean {
        scripts.remove("/AppScript/$packageName/$name.lua")
        return true
    }

    override suspend fun setAppScriptEnabled(packageName: String, name: String, enabled: Boolean): Boolean = true

    override suspend fun readScript(relativePath: String): String {
        val norm = if (relativePath.startsWith("/")) relativePath else "/$relativePath"
        return scripts[norm].orEmpty()
    }

    override suspend fun saveScript(relativePath: String, content: String): Boolean {
        val norm = if (relativePath.startsWith("/")) relativePath else "/$relativePath"
        scripts[norm] = content
        return true
    }

    override suspend fun parseParameters(content: String): WorkspaceFileManager.FileParameters? = null

    override suspend fun forceStopApp(packageName: String): Boolean {
        stoppedApps.add(packageName)
        return true
    }

    override suspend fun restartApp(packageName: String): Boolean = forceStopApp(packageName)

    override suspend fun prepareShareScript(
        cacheDir: File,
        packageName: String,
        scriptName: String,
        description: String,
        author: String
    ): File? {
        val file = File(cacheDir, "$scriptName.lua")
        file.writeText("-- share test")
        return file
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ScriptEditorViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeScriptRepository
    private lateinit var viewModel: ScriptEditorViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeScriptRepository()
        viewModel = ScriptEditorViewModel(
            scriptRepository = repository,
            ioDispatcher = testDispatcher,
            syntaxValidator = { code ->
                if (code.contains("broken")) "syntax error near 'broken'" else null
            }
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init in Global mode sets state and emits InitContent`() = runTest(testDispatcher) {
        repository.scripts["/global.lua"] = "print('init global')"

        val effects = mutableListOf<ScriptEditorEffect>()
        val job = launch {
            viewModel.effect.collect { effects.add(it) }
        }

        viewModel.init(ScriptEditorMode.Global)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Global", state.title)
        assertEquals(ScriptEditorMode.Global, state.mode)
        assertFalse(state.canRun)
        assertFalse(state.isLoading)
        assertEquals("print('init global')", state.lastSavedContent)

        assertTrue(effects.any { it is ScriptEditorEffect.InitContent && it.content == "print('init global')" })
        job.cancel()
    }

    @Test
    fun `init in AppSpecific mode sets title and canRun is true`() = runTest(testDispatcher) {
        val mode = ScriptEditorMode.AppSpecific(
            packageName = "com.foo.app",
            scriptName = "myHook",
            appName = "FooApp",
            scriptDescription = "My Hook Desc"
        )
        repository.scripts["/AppScript/com.foo.app/myHook.lua"] = "-- hook code"

        viewModel.init(mode)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("myHook", state.title)
        assertTrue(state.canRun)
        assertEquals("-- hook code", state.lastSavedContent)
    }

    @Test
    fun `save updates lastSavedContent and emits SaveSuccess`() = runTest(testDispatcher) {
        viewModel.init(ScriptEditorMode.Global)
        advanceUntilIdle()

        val effects = mutableListOf<ScriptEditorEffect>()
        val job = launch {
            viewModel.effect.collect { effects.add(it) }
        }

        viewModel.save("new script content", showToast = true)
        advanceUntilIdle()

        assertEquals("new script content", repository.scripts["/global.lua"])
        assertEquals("new script content", viewModel.uiState.value.lastSavedContent)
        assertFalse(viewModel.uiState.value.isModified)
        assertTrue(effects.contains(ScriptEditorEffect.SaveSuccess))

        job.cancel()
    }

    @Test
    fun `checkExternalChange reloads content when editor is not modified`() = runTest(testDispatcher) {
        repository.scripts["/global.lua"] = "v1"
        viewModel.init(ScriptEditorMode.Global)
        advanceUntilIdle()

        val effects = mutableListOf<ScriptEditorEffect>()
        val job = launch {
            viewModel.effect.collect { effects.add(it) }
        }

        // Simulate external edit (e.g. MCP)
        repository.scripts["/global.lua"] = "v2"
        viewModel.checkExternalChange(currentEditorContent = "v1", quiet = false)
        advanceUntilIdle()

        assertEquals("v2", viewModel.uiState.value.lastSavedContent)
        assertTrue(effects.any { it is ScriptEditorEffect.ReloadContent && it.content == "v2" })

        job.cancel()
    }

    @Test
    fun `checkExternalChange emits FileConflict when editor is modified`() = runTest(testDispatcher) {
        repository.scripts["/global.lua"] = "v1"
        viewModel.init(ScriptEditorMode.Global)
        advanceUntilIdle()

        val effects = mutableListOf<ScriptEditorEffect>()
        val job = launch {
            viewModel.effect.collect { effects.add(it) }
        }

        // External update to disk, and user modified editor
        repository.scripts["/global.lua"] = "v2_disk"
        viewModel.checkExternalChange(currentEditorContent = "v1_user_unsaved", quiet = false)
        advanceUntilIdle()

        assertTrue(effects.any { it is ScriptEditorEffect.FileConflict && it.diskContent == "v2_disk" })

        job.cancel()
    }

    @Test
    fun `runScript in AppSpecific mode force-stops app and emits LaunchApp`() = runTest(testDispatcher) {
        val mode = ScriptEditorMode.AppSpecific("com.target.pkg", "scriptX")
        viewModel.init(mode)
        advanceUntilIdle()

        val effects = mutableListOf<ScriptEditorEffect>()
        val job = launch {
            viewModel.effect.collect { effects.add(it) }
        }

        viewModel.runScript("updated content")
        advanceUntilIdle()

        assertTrue(repository.stoppedApps.contains("com.target.pkg"))
        assertTrue(effects.any { it is ScriptEditorEffect.LaunchApp && it.packageName == "com.target.pkg" })

        job.cancel()
    }

    @Test
    fun `validateSyntax sets syntaxError for invalid Lua and clears for valid Lua`() = runTest(testDispatcher) {
        viewModel.validateSyntax("function(broken")
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.syntaxError)

        viewModel.validateSyntax("local x = 123; print(x)")
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.syntaxError)
    }

    @Test
    fun `isContentModified returns true when text differs from lastSavedContent`() = runTest(testDispatcher) {
        repository.scripts["/global.lua"] = "original"
        viewModel.init(ScriptEditorMode.Global)
        advanceUntilIdle()

        assertFalse(viewModel.isContentModified("original"))
        assertTrue(viewModel.isContentModified("modified"))
    }

    @Test
    fun `onContentChanged updates isModified and performs debounced syntax validation`() = runTest(testDispatcher) {
        repository.scripts["/global.lua"] = "initial"
        viewModel.init(ScriptEditorMode.Global)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isModified)
        assertNull(viewModel.uiState.value.syntaxError)

        viewModel.onContentChanged("code with broken token")
        assertTrue(viewModel.uiState.value.isModified)

        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.syntaxError)

        viewModel.onContentChanged("initial")
        assertFalse(viewModel.uiState.value.isModified)
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.syntaxError)
    }

    @Test
    fun `file event triggers FileConflict instead of silent overwrite when user modified editor`() = runTest(testDispatcher) {
        repository.scripts["/global.lua"] = "v1"
        viewModel.init(ScriptEditorMode.Global)
        advanceUntilIdle()

        val effects = mutableListOf<ScriptEditorEffect>()
        val job = launch {
            viewModel.effect.collect { effects.add(it) }
        }

        // User typed into editor
        viewModel.onContentChanged("v1_unsaved_user_work")
        advanceUntilIdle()

        // Disk modified by external source (e.g. MCP)
        repository.scripts["/global.lua"] = "v2_disk"
        WorkspaceEventManager.notifyFileChanged("/global.lua")
        advanceUntilIdle()

        // Must emit FileConflict, NEVER ReloadContent
        assertTrue(effects.any { it is ScriptEditorEffect.FileConflict && it.diskContent == "v2_disk" })
        assertFalse(effects.any { it is ScriptEditorEffect.ReloadContent })

        job.cancel()
    }
}
