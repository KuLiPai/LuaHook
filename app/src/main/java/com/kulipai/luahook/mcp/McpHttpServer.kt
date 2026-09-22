package com.kulipai.luahook.mcp

import org.json.JSONArray
import org.json.JSONObject
import org.nanohttpd.protocols.http.IHTTPSession
import org.nanohttpd.protocols.http.NanoHTTPD
import org.nanohttpd.protocols.http.request.Method
import org.nanohttpd.protocols.http.response.Response
import org.nanohttpd.protocols.http.response.Status

/**
 * 绑定 0.0.0.0 的 JSON-RPC。Cursor 用 POST /mcp。
 * Accept 含 text/event-stream 时，响应是 `event: message` 的 SSE；否则是普通 JSON。
 */
class McpHttpServer(private val listenPort: Int) : NanoHTTPD("0.0.0.0", listenPort) {
    init {
        setHTTPHandler { session -> onRequest(session) }
    }

    /**
     * GET /mcp 回一段 SSE 保活；GET /health 和 /mcp/health 回端口。
     * 没有 id 的通知回 202 空体。initialize 会回显客户端的 protocolVersion。
     */
    private fun onRequest(session: IHTTPSession): Response {
        if (session.method == Method.OPTIONS) {
            return respond(session, Status.OK, "", "application/json")
        }
        if (session.method == Method.GET && session.uri == "/mcp") {
            return respond(session, Status.OK, ": ok\n\n", "text/event-stream")
        }
        if (session.method == Method.GET && (session.uri == "/health" || session.uri == "/mcp/health")) {
            return respond(session, Status.OK, JSONObject().put("ok", true).put("port", listenPort).toString(), "application/json")
        }
        if (session.method != Method.POST || (session.uri != "/" && session.uri != "/mcp")) {
            return respond(session, Status.METHOD_NOT_ALLOWED, rpcError(null, -32600, "POST JSON-RPC to /mcp").toString(), "application/json")
        }
        val request = try {
            JSONObject(readBody(session))
        } catch (_: Exception) {
            return respond(session, Status.BAD_REQUEST, rpcError(null, -32700, "parse error").toString(), "application/json")
        }
        if (!request.has("id")) {
            return respond(session, Status.ACCEPTED, "", "application/json")
        }
        val id = request.get("id")
        val params = request.optJSONObject("params") ?: JSONObject()
        val rpc = try {
            val result = dispatch(request.optString("method"), params)
            JSONObject().put("jsonrpc", "2.0").put("id", id).put("result", result)
        } catch (e: Exception) {
            rpcError(id, -32000, e.message ?: "error")
        }
        val accept = session.headers?.get("accept").orEmpty()
        return if (accept.contains("text/event-stream")) {
            respond(session, Status.OK, "event: message\ndata: $rpc\n\n", "text/event-stream")
        } else {
            respond(session, Status.OK, rpc.toString(), "application/json")
        }
    }

    /** JSON-RPC 方法：initialize、ping、tools/list、tools/call。 */
    private fun dispatch(method: String, params: JSONObject): Any {
        return when (method) {
            "initialize" -> {
                val requested = params.optString("protocolVersion")
                val version = if (requested.isNotBlank()) requested else "2025-03-26"
                JSONObject()
                    .put("protocolVersion", version)
                    .put("capabilities", JSONObject().put("tools", JSONObject()))
                    .put("serverInfo", JSONObject().put("name", "luahook").put("version", "4.1.1"))
            }
            "ping" -> JSONObject()
            "tools/list" -> JSONObject().put("tools", toolDefs())
            "tools/call" -> callTool(params)
            else -> error("unknown method $method")
        }
    }

    /** 把工具名转到 McpStore。结果包成 MCP 的 content 文本。 */
    private fun callTool(params: JSONObject): JSONObject {
        val name = params.optString("name")
        val args = params.optJSONObject("arguments") ?: JSONObject()
        val payload = when (name) {
            "list_projects" -> JSONObject().put("projects", McpStore.listProjects())
            "create_project" -> McpStore.createProject(args)
            "set_project_enabled" -> McpStore.setProjectEnabled(args)
            "delete_project" -> McpStore.deleteProject(args)
            "read_project_file" -> McpStore.readProjectFile(args)
            "write_project_file" -> McpStore.writeProjectFile(args)
            "read_global_script" -> McpStore.readGlobalScript()
            "write_global_script" -> McpStore.writeGlobalScript(args)
            "read_selected_apps" -> McpStore.readSelectedApps()
            "write_selected_apps" -> McpStore.writeSelectedApps(args)
            "list_app_scripts" -> McpStore.listAppScripts(args)
            "create_app_script" -> McpStore.createAppScript(args)
            "read_app_script" -> McpStore.readAppScript(args)
            "write_app_script" -> McpStore.writeAppScript(args)
            "set_app_script_enabled" -> McpStore.setAppScriptEnabled(args)
            "delete_app_script" -> McpStore.deleteAppScript(args)
            "read_logs" -> McpStore.readLogs(args)
            "list_installed_apps" -> JSONObject().put("apps", McpStore.listInstalledApps())
            else -> error("unknown tool $name")
        }
        return JSONObject()
            .put("content", JSONArray().put(JSONObject().put("type", "text").put("text", payload.toString())))
            .put("isError", payload.optBoolean("ok", true).not() && payload.has("error"))
    }

    /** tools/list 里的 18 个工具，和插件页说明一一对应。 */
    private fun toolDefs(): JSONArray {
        fun tool(name: String, description: String, vararg fields: String): JSONObject {
            val props = JSONObject()
            fields.forEach { props.put(it, JSONObject().put("type", "string")) }
            return JSONObject()
                .put("name", name)
                .put("description", description)
                .put("inputSchema", JSONObject().put("type", "object").put("properties", props))
        }
        return JSONArray()
            .put(tool("list_projects", "List projects"))
            .put(tool("create_project", "Create a project", "name", "description", "author", "scope", "launcher", "icon"))
            .put(tool("set_project_enabled", "Enable or disable a project", "name", "enabled"))
            .put(tool("delete_project", "Delete a project", "name"))
            .put(tool("read_project_file", "Read a file in a project", "name", "file"))
            .put(tool("write_project_file", "Write a file in a project", "name", "file", "content"))
            .put(tool("read_global_script", "Read /global.lua"))
            .put(tool("write_global_script", "Write /global.lua", "content"))
            .put(tool("read_selected_apps", "Read /apps.txt"))
            .put(tool("write_selected_apps", "Write /apps.txt", "packages"))
            .put(tool("list_app_scripts", "List scripts for a package", "package"))
            .put(tool("create_app_script", "Create an app script", "package", "name", "description", "content"))
            .put(tool("read_app_script", "Read an app script", "package", "name"))
            .put(tool("write_app_script", "Replace an app script", "package", "name", "content"))
            .put(tool("set_app_script_enabled", "Enable or disable an app script", "package", "name", "enabled"))
            .put(tool("delete_app_script", "Delete an app script", "package", "name"))
            .put(tool("read_logs", "Read LuaXposed logcat", "lines"))
            .put(tool("list_installed_apps", "List installed apps for scope selection"))
    }

    /** NanoHTTPD 把 POST 正文放在 parseBody 的 postData 里。 */
    private fun readBody(session: IHTTPSession): String {
        val files = HashMap<String, String>()
        session.parseBody(files)
        return files["postData"].orEmpty()
    }

    /** JSON-RPC 错误体。解析失败用 -32700，未知方法用 -32000。 */
    private fun rpcError(id: Any?, code: Int, message: String): JSONObject {
        return JSONObject()
            .put("jsonrpc", "2.0")
            .put("id", id ?: JSONObject.NULL)
            .put("error", JSONObject().put("code", code).put("message", message))
    }

    /** 补上 CORS，并在 / 和 /mcp 上带固定会话头 Mcp-Session-Id: luahook。 */
    private fun respond(session: IHTTPSession, status: Status, body: String, contentType: String): Response {
        val response = Response.newFixedLengthResponse(status, contentType, body)
        response.addHeader("Access-Control-Allow-Origin", "*")
        response.addHeader("Access-Control-Allow-Headers", "Content-Type, Accept, Mcp-Session-Id, MCP-Protocol-Version")
        response.addHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        if (session.uri == "/mcp" || session.uri == "/") {
            response.addHeader("Mcp-Session-Id", "luahook")
        }
        return response
    }

    private fun json(status: Status, body: JSONObject): Response {
        return Response.newFixedLengthResponse(status, "application/json", body.toString())
    }
}
