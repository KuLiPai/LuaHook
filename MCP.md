# LuaHook MCP

MCP 服务挂在插件 `mcp` 上，不随模块无条件常驻。工作区就绪后，如果 `/data/local/tmp/LuaHook/Plugin/mcp/init.lua` 里 `enabled = true` 且 `entry = "mcp"`，应用才启动前台服务。

默认监听 `0.0.0.0:24555`。端口写在同一份 `init.lua` 的 `port` 字段，范围 1–65535。插件页的卡片和项目列表同一套样式：开关控制启用，齿轮或卡片打开端口输入框。改完后服务按新端口重新绑定。

```lua
name = "mcp"
description = "LuaHook MCP"
enabled = true
port = 24555
entry = "mcp"
```

协议是 HTTP JSON-RPC 2.0，`POST /`。`GET /health` 返回 `{"ok":true,"port":<当前端口>}`。工具调用：

```json
{"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"read_global_script","arguments":{}}}
```

工具只覆盖现有工作区，路径都在 `/data/local/tmp/LuaHook` 下。

| 工具 | 作用 |
| --- | --- |
| `read_global_script` / `write_global_script` | 读写 `/global.lua` |
| `read_selected_apps` / `write_selected_apps` | 读写 `/apps.txt` |
| `list_app_scripts` / `create_app_script` / `read_app_script` / `write_app_script` / `set_app_script_enabled` / `delete_app_script` | 某个包的 `AppScript` 与 `AppConf` |
| `list_projects` / `create_project` / `read_project_file` / `write_project_file` / `set_project_enabled` / `delete_project` | 项目目录与 `Project/info.json` |
| `read_logs` | 只读 `LuaXposed` logcat，不能清空或改写 |
| `list_installed_apps` | 只读已安装且有启动入口的应用，供作用域选择 |

不提供 shell、不改日志、不提交 Xposed 作用域、不做项目 zip 导入导出。

本机验证脚本：`scripts/mcp-probe.py`（把里面的地址改成手机局域网 IP）。
