---
name: luahook-scripting
description: Use when writing, debugging, reviewing, translating, or explaining LuaHook scripts for Android. This skill covers LuaHook as a Lua-based Xposed module framework, including hook/hookctor/hookAll/replace usage, lpparam, imports/import/findClass/invoke, field access, Task/http/json helpers, Java interop patterns, and LuaJ++ syntax sugar such as omitted keywords, switch/when, lambda, property-style getter/setter access, and Java class construction.
---

# LuaHook Scripting

## Overview

Use this skill when the user wants to create or fix a LuaHook script, explain a LuaHook API, translate Java or Xposed logic into LuaHook, or understand LuaJ++ syntax used by LuaHook examples.

LuaHook is an Android framework for writing Xposed-style modules in Lua. Prefer this skill when the task is primarily about LuaHook script authoring rather than Android app reverse engineering or native exploitation.

## Use This Skill For

- Writing a new LuaHook script for a target app, class, or method.
- Migrating a Java/Xposed idea into LuaHook.
- Explaining `hook`, `hookctor`, `hookAll`, `replace`, `lpparam`, `imports`, `import`, `findClass`, `invoke`, or field helpers.
- Debugging LuaJ++ syntax issues or Java interop quirks.
- Producing reusable LuaHook script templates and examples.
- Handling advanced native LuaHook usage through the `native.*` API.

## Do Not Use This Skill For

- General Lua questions that do not involve LuaHook or LuaJ++.
- Pure Android Java/Kotlin development with no LuaHook scripting.
- Native exploitation where the main problem is memory corruption, not LuaHook script authoring.

## Workflow

1. Identify whether the user needs app-level Java hooks, native hooks, or syntax explanation.
2. If working via MCP (`get_scripting_guide`), pass the desired topic or relative path directly (e.g. `topic = "core"`, `"patterns"`, `"luajpp"`, `"native"`, `"field"`, `"hook"`, `"import"`, `"invoke"`, `"lpparam"`, `"native/pointer"`, `"native/memory"`, `"all"`).
3. If working locally as an agent skill, load only the relevant reference file:
   - Core Java/Xposed-style scripting: `references/core-api.md`
   - LuaJ++ syntax and Java sugar: `references/luajpp-cheatsheet.md`
   - Practical starter templates and patterns: `references/patterns.md`
   - Native hook and pointer work: `references/native-api.md`
   - Project framing and terminology: `references/project-context.md`
4. Prefer bundled local source docs under `references/source-docs/` (or via MCP topic keywords like `"field"`, `"hook"`, `"memory"`) over external web links.
4. If the docs do not confirm an API shape, say that explicitly and choose the safest documented form.
5. Return code that is ready to paste into a LuaHook script, with a short explanation of why each LuaHook-specific choice is used.

## Authoring Rules

- Prefer the newer table-style hook syntax when the runtime supports it.
- When using table-style `hook`, `hookctor`, or `replace` with a named class target, always emit `params = {...}` explicitly.
- For zero-argument methods or constructors, write `params = {}` rather than omitting `params`.
- Only omit `params` when directly hooking a reflected `Method` object or when the API truly does not use `params`, such as ordinary `hookAll` usage.
- Use exact Java class names and method names.
- Default to `lpparam.classLoader` for host-side class lookup unless the user provides a different loader.
- Prefer `imports` for host-or-module class loading and `import` for module-bundled classes only.
- Use `invoke`, `getField`, `setField`, `getStaticField`, and `setStaticField` when direct Java access is insufficient.
- Explain LuaJ++ sugar only when it improves readability; otherwise prefer conventional, explicit code.
- When using nonstandard LuaJ++ syntax, mention that it is LuaJ++-specific, not plain Lua.
- For uncertain or version-sensitive behavior, avoid inventing undocumented convenience helpers.

## Output Checklist

- State the target hook point: class, method, constructor, or native address.
- State whether the logic belongs in `before`, `after`, or `replace`.
- Use the correct loader and import style.
- Include `params = {}` explicitly for zero-argument table-style hooks and replaces.
- Use LuaJ++ syntax intentionally rather than accidentally.
- Mention any known ambiguity from the docs if it affects correctness.

## Known Ambiguities To Handle Carefully

- The official docs show both `classloader` and `classLoader` in hook config examples. If the user does not know which version they run, prefer omitting the field when the default loader is sufficient, or note that both spellings appear in docs.
- The `hookAll` documentation describes hooking all overloads of a named method, but the published config example omits a `method` field. Treat this as version-sensitive and call it out when generating `hookAll` code.
- Upstream docs often describe `params` as optional, but this project has observed that generated code is more reliable when `params` is always explicit for table-style `hook`, `hookctor`, and `replace`.
- Native APIs have both new `native.memory.*` forms and legacy compatibility aliases. Prefer the newer names in fresh code.

## Local Docs

- Bundled local source docs live under `references/source-docs/`.
- Start with `references/source-docs/README.md` to see what was copied into the skill.
- Prefer these local files over the original web URLs when citing or checking API details.

## Example Requests

- "Write a LuaHook script that hooks an Activity `onCreate`."
- "Convert this Xposed Java snippet into LuaHook."
- "Explain the difference between LuaHook `imports` and `import`."
- "Why does this LuaJ++ syntax fail, and can you rewrite it more safely?"
- "Use LuaHook native APIs to hook a function in `libxxx.so`."

## References

- `references/project-context.md`
- `references/core-api.md`
- `references/luajpp-cheatsheet.md`
- `references/patterns.md`
- `references/native-api.md`
- `references/source-docs/README.md`
