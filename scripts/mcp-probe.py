import json
import urllib.request

BASE = "http://192.168.5.14:24555/"
PKG = "com.android.settings"
SCRIPT = "mcp_probe"
PROJECT = "mcp_goal"


def rpc(method, params=None, id=1):
    body = {"jsonrpc": "2.0", "id": id, "method": method}
    if params is not None:
        body["params"] = params
    req = urllib.request.Request(
        BASE,
        data=json.dumps(body).encode(),
        headers={"Content-Type": "application/json"},
    )
    with urllib.request.urlopen(req, timeout=60) as resp:
        return json.load(resp)


def call(name, arguments=None, id=1):
    result = rpc("tools/call", {"name": name, "arguments": arguments or {}}, id)
    if "error" in result:
        raise SystemExit(f"{name} RPC {result['error']}")
    text = result["result"]["content"][0]["text"]
    return json.loads(text)


def main():
    print("INIT", rpc("initialize", {"protocolVersion": "2024-11-05", "capabilities": {}, "clientInfo": {"name": "test", "version": "0"}}, 1)["result"]["serverInfo"])
    tools = [t["name"] for t in rpc("tools/list", {}, 2)["result"]["tools"]]
    expected = [
        "list_projects", "create_project", "set_project_enabled", "delete_project",
        "read_project_file", "write_project_file",
        "read_global_script", "write_global_script",
        "read_selected_apps", "write_selected_apps",
        "list_app_scripts", "create_app_script", "read_app_script", "write_app_script",
        "set_app_script_enabled", "delete_app_script",
        "read_logs", "list_installed_apps",
    ]
    missing = [name for name in expected if name not in tools]
    print("TOOLS", len(tools), "MISSING", missing)
    if missing:
        raise SystemExit(1)

    original_global = call("read_global_script", {}, 3)["content"]
    original_apps = call("read_selected_apps", {}, 4)["packages"]
    print("GLOBAL_LEN", len(original_global), "APPS", original_apps)
    try:
        marker = 'print("LUAHHOOK_MCP_GLOBAL")\n'
        written = call("write_global_script", {"content": marker}, 5)
        read_back = call("read_global_script", {}, 6)["content"]
        print("GLOBAL_ROUNDTRIP", written.get("ok"), read_back.strip() == marker.strip())

        apps_written = call("write_selected_apps", {"packages": [PKG]}, 7)
        apps_read = call("read_selected_apps", {}, 8)["packages"]
        print("APPS_ROUNDTRIP", apps_written.get("ok"), PKG in apps_read, apps_read)

        created = call("create_app_script", {
            "package": PKG,
            "name": SCRIPT,
            "description": "mcp probe",
            "content": 'print("LUAHHOOK_MCP_APP")\n',
        }, 9)
        app_read = call("read_app_script", {"package": PKG, "name": SCRIPT}, 10)
        app_write = call("write_app_script", {
            "package": PKG,
            "name": SCRIPT,
            "content": 'print("LUAHHOOK_MCP_APP2")\n',
        }, 11)
        app_read2 = call("read_app_script", {"package": PKG, "name": SCRIPT}, 12)
        listed = call("list_app_scripts", {"package": PKG}, 13)["scripts"]
        names = [item["name"] for item in listed]
        disabled = call("set_app_script_enabled", {"package": PKG, "name": SCRIPT, "enabled": False}, 14)
        listed_off = call("list_app_scripts", {"package": PKG}, 15)["scripts"]
        off_meta = next((item.get("meta") for item in listed_off if item["name"] == SCRIPT), "")
        enabled = call("set_app_script_enabled", {"package": PKG, "name": SCRIPT, "enabled": True}, 16)
        deleted = call("delete_app_script", {"package": PKG, "name": SCRIPT}, 17)
        listed_after = [item["name"] for item in call("list_app_scripts", {"package": PKG}, 18)["scripts"]]
        print("APP", created.get("ok"), app_read.get("content"), app_write.get("ok"), app_read2.get("content"), SCRIPT in names)
        print("APP_TOGGLE", disabled.get("ok"), "false" in str(off_meta).lower(), enabled.get("ok"))
        print("APP_DELETED", deleted.get("ok"), SCRIPT not in listed_after, listed_after)

        project = call("create_project", {
            "name": PROJECT,
            "description": "mcp probe",
            "author": "mcp",
            "scope": [PKG],
            "launcher": PKG,
        }, 19)
        file_write = call("write_project_file", {
            "name": PROJECT,
            "file": "main.lua",
            "content": 'print("LUAHHOOK_MCP_OK")\n',
        }, 20)
        file_read = call("read_project_file", {"name": PROJECT, "file": "main.lua"}, 21)
        projects = call("list_projects", {}, 22)["projects"]
        names = [item["name"] for item in projects]
        off = call("set_project_enabled", {"name": PROJECT, "enabled": False}, 23)
        listed_off = call("list_projects", {}, 24)["projects"]
        off_enabled = next((item.get("enabled") for item in listed_off if item["name"] == PROJECT), None)
        on = call("set_project_enabled", {"name": PROJECT, "enabled": True}, 25)
        gone = call("delete_project", {"name": PROJECT}, 26)
        projects_after = [item["name"] for item in call("list_projects", {}, 27)["projects"]]
        print("PROJECT", project.get("ok"), file_write.get("ok"), file_read.get("content"), PROJECT in names)
        print("PROJECT_TOGGLE", off.get("ok"), off_enabled, on.get("ok"))
        print("PROJECT_DELETED", gone.get("ok"), PROJECT not in projects_after)

        apps = call("list_installed_apps", {}, 28)["apps"]
        print("INSTALLED", len(apps), any(item.get("package") == PKG for item in apps))

        logs = call("read_logs", {"lines": 400}, 29)
        content = logs.get("content", "")
        print("LOG_LEN", len(content), "HAS_OK", "LUAHHOOK_LOAD_OK" in content)
        print("LOG_TAIL", content[-500:])
    finally:
        call("write_global_script", {"content": original_global}, 90)
        call("write_selected_apps", {"packages": original_apps}, 91)
        restored = call("read_global_script", {}, 92)["content"]
        print("RESTORED_GLOBAL", restored == original_global)


if __name__ == "__main__":
    main()
