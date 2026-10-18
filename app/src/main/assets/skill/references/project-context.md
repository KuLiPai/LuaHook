# LuaHook Project Context

## What LuaHook Is

LuaHook is an Android framework for writing Xposed-style modules in Lua. It aims to combine persistent module behavior with dynamic script loading, so scripts can often be edited without rebuilding an APK.

## What Makes LuaHook Different

- Uses Lua as the authoring language for Xposed-style hooks.
- Exposes Xposed concepts such as `lpparam` and method interception in Lua.
- Adds LuaJ++ syntax sugar for easier Java interop.
- Supports both Java-layer hooks and advanced native-layer APIs.

## Environment Framing

- Root workflows commonly mention `LSPosed`.
- Non-root workflows commonly mention `LSPatch` and `Shizuku`.
- The script author usually targets a specific Android app package and its classes.

## Script Writing Assumptions

- The user usually wants paste-ready LuaHook code.
- The most important inputs are package, target class, target method, parameter types, and desired timing.
- If the user only knows behavior, infer likely hook points but label them as guesses.

## Entry Points Mentioned In Docs

The LuaJ++ notes mention these entry filenames for service-style runtimes:

- `main.lua`
- `service.lua`
- `accessibility.lua`
- `notification.lua`
- `wallpaper.lua`

## Local Source Docs

Use the bundled copies rather than external links when possible:

- `references/source-docs/zh/introduction.md`
- `references/source-docs/zh/quickstart.md`
- `references/source-docs/zh/firstscript.md`
- `references/source-docs/zh/luaj++.md`
- `references/source-docs/README.md`

## Original Upstream Repo

The bundled docs were copied from the LuaHook docs repository so the skill can still work offline or in restricted environments.
