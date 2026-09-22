# Adaptation notes for JingMatrix LSPosed 1.11.0 (API 100) on Xiaomi M2102J2SC

## Goal verified

After one reboot (clears ephemeral `magiskpolicy --live`) and launching Settings
(`com.android.settings/.MiuiSettings`), logcat shows:

```text
09-22 10:37:17.528  9074  9074 D LuaXposed: LUAHHOOK_LOAD_OK com.android.settings
```

Legacy entry `assets/xposed_init` → `com.kulipai.luahook.hook.entry.MainHook` remains the manager entry so the "designed for Xposed 101" warning stays gone (`java_init.list` removed). `module.prop` keeps `minApiVersion=53` and `targetApiVersion=53`, matching `xposedminversion`. A target above the framework API is what shows the warning on API 93 and API 100. `NewHook` is the API 102 no-arg class and is not registered.

## Root causes

1. **PackageReadyParam ClassNotFound** — `LuaHookEngine.init` references `XposedModuleInterface.PackageReadyParam` (API 101+). JingMatrix 1.11.0 is API 100 and does not ship that class. Fix: package `io.github.libxposed:api:102.0.0` as `implementation` (not `compileOnly`).

2. **Empty global.lua under SELinux** — Host domains (`system_app`, `system_server`, …) get `avc: denied { search }` on `/data/local/tmp` (`shell_data_file`). `WorkspaceFileManager.read` then returned `""`, so LuaJ still initialized (OsLib lock-verification warning) but `print("LUAHHOOK_LOAD_OK …")` never ran and no `LuaXposed` tag appeared.

## Fixes in this adaptation

- Keep legacy `MainHook` entry; do not reintroduce `java_init.list`.
- Package libxposed API 102 classes into the APK.
- `WorkspaceFileManager.read` falls back to root `cat` (libsu / `su`) when the File API cannot open the workspace.
- Optional APatch/Magisk module under `tools/luahook-selinux/` (`sepolicy.rule`) permanently allows host apps to read `shell_data_file` so direct File reads also work after reboot.
- Load-test script: `scripts/load-test.lua` → `/data/local/tmp/LuaHook/global.lua`.

## MCP

插件 `mcp` 在工作区就绪后按 `Plugin/mcp/init.lua` 的 `port` 监听，默认 `0.0.0.0:24555`。工具和验证见 `MCP.md`。
