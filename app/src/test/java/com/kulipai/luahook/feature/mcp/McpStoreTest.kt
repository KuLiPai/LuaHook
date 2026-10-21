package com.kulipai.luahook.feature.mcp

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class McpStoreTest {

    @Test
    fun `getScriptingGuide with empty or overview returns overview`() {
        val result = McpStore.getScriptingGuide(JSONObject())
        val guide = result.optString("guide")
        assertTrue(guide.isNotEmpty())
        assertTrue(guide.contains("LuaHook Scripting") || guide.contains("Core rules"))
    }

    @Test
    fun `getScriptingGuide with topic field returns field documentation`() {
        val result = McpStore.getScriptingGuide(JSONObject().put("topic", "field"))
        val guide = result.optString("guide")
        val title = result.optString("title")
        assertEquals("LuaHook Field Operations", title)
        assertTrue(guide.contains("getField"))
        assertTrue(guide.contains("setField"))
    }

    @Test
    fun `getScriptingGuide with topic native returns complete native and memory APIs`() {
        val result = McpStore.getScriptingGuide(JSONObject().put("topic", "native"))
        val guide = result.optString("guide")
        assertTrue(guide.contains("LuaPointer"))
        assertTrue(guide.contains("native.memory"))
        assertTrue(guide.contains("read_u32"))
        assertTrue(guide.contains("write_cstr"))
        assertTrue(guide.contains("native.hook"))
        assertTrue(guide.contains("native.new_function"))
    }

    @Test
    fun `getScriptingGuide with topic new_function or native invoke returns native function invocation`() {
        val result1 = McpStore.getScriptingGuide(JSONObject().put("topic", "new_function"))
        val guide1 = result1.optString("guide")
        assertEquals("LuaHook Native Function Invocation", result1.optString("title"))
        assertTrue(guide1.contains("native.new_function"))

        val result2 = McpStore.getScriptingGuide(JSONObject().put("topic", "native/invoke"))
        val guide2 = result2.optString("guide")
        assertEquals("LuaHook Native Function Invocation", result2.optString("title"))
        assertTrue(guide2.contains("native.new_function"))
    }

    @Test
    fun `getScriptingGuide with exact relative doc path returns corresponding file`() {
        val result = McpStore.getScriptingGuide(
            JSONObject().put("topic", "references/source-docs/zh/manual/field.md")
        )
        val guide = result.optString("guide")
        assertFalse(result.has("error"))
        assertTrue(guide.contains("getField"))
    }

    @Test
    fun `getScriptingGuide with unknown topic returns error and available_topics`() {
        val result = McpStore.getScriptingGuide(JSONObject().put("topic", "invalid_topic_xyz"))
        assertTrue(result.has("error"))
        assertTrue(result.optString("error").contains("invalid_topic_xyz"))
        assertTrue(result.has("available_topics"))
        val topics = result.getJSONArray("available_topics")
        val topicList = List(topics.length()) { topics.getString(it) }
        assertTrue(topicList.contains("field"))
        assertTrue(topicList.contains("native"))
        assertTrue(topicList.contains("luajpp"))
        assertTrue(topicList.contains("hook"))
    }

    @Test
    fun `getScriptingGuide with topic luajpp returns sugar and java interop`() {
        val result = McpStore.getScriptingGuide(JSONObject().put("topic", "luajpp"))
        val guide = result.optString("guide")
        assertTrue(guide.contains("switch"))
        assertTrue(guide.contains("lambda"))
    }
}
