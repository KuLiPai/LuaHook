# LuaHook Core API Reference

## Hook Family

LuaHook docs describe four core Java-layer hook forms:

- `hook`: intercept a single method.
- `hookctor`: intercept a constructor.
- `hookAll`: intercept all overloads of a method name.
- `replace`: fully replace a method implementation.

## Project-Safe Rule For `params`

For this skill, prefer a stricter style than the upstream docs:

- When using table-style `hook`, `hookctor`, or `replace` with `class` plus a named method or constructor target, always emit `params = {...}`.
- For zero-argument methods or constructors, write `params = {}`.
- Only omit `params` when directly targeting a reflected `Method` object or when the API does not use `params`, such as the documented `hookAll` example shape.

This rule is intentional. Upstream docs often describe `params` as optional, but the project feedback says explicit `params` is more reliable in practice.

## Preferred Hook Style

Prefer the config-table style when possible.

```lua
hook {
  class = "android.app.Activity",
  method = "onCreate",
  params = {"android.os.Bundle"},
  after = function(it)
  end,
}
```

If loader selection matters, use the host loader explicitly.

```lua
hook {
  class = "android.app.Activity",
  classLoader = lpparam.classLoader,
  method = "onCreate",
  params = {"android.os.Bundle"},
  after = function(it)
  end,
}
```

For zero-argument methods, still write `params = {}`.

```lua
hook {
  class = "com.example.TargetClass",
  method = "isVip",
  params = {},
  before = function(it)
    it.result = true
  end,
}
```

Docs also show `classloader` with a lowercase `l`. Treat the exact field spelling as version-sensitive.

## Old Positional Hook Style

The docs retain positional syntax for compatibility.

```lua
hook("android.app.Activity",
  lpparam.classLoader,
  "onCreate",
  "android.os.Bundle",
  function(it)
  end,
  function(it)
  end
)
```

Use this fallback form if config-table field names are uncertain on the user's version.

## `hookctor`

Use explicit `params`, including an empty table for a zero-argument constructor.

```lua
hookctor {
  class = "com.example.TargetClass",
  params = {},
  after = function(it)
  end,
}
```

## `replace`

Use `replace` when the original method must not run at all.

```lua
replace {
  class = "com.example.TargetClass",
  method = "getMessage",
  params = {},
  replace = function(it)
    return "patched"
  end,
}
```

## `before`, `after`, and `replace`

- Use `before` to inspect or modify arguments before the original method runs.
- Use `after` to read or replace the result after the original method runs.
- Use `replace` when the original method must not run at all.

## `it` Hook Context

Hook callbacks receive `it`, a method hook context similar to `MethodHookParam`.

- `it.method`: current `Method` or `Constructor` object.
- `it.args`: argument table.
- `it.args[0]`: first argument.
- `it.result`: return value.
- `it.thisObject`: Java `this` instance for instance methods.

Documented behavior:

- Assigning `it.args[i]` in `before` changes the argument passed to the original method.
- Assigning `it.result` in `before` skips the original method and returns that value.
- Assigning `it.result` in `after` overwrites the original result.
- In `replace`, return the new value directly.

## `lpparam`

`lpparam` represents the current host package context.

Common fields documented by LuaHook:

- `lpparam.classLoader`
- `lpparam.packageName`
- `lpparam.processName`
- `lpparam.appInfo`
- `lpparam.isFirstApplication`

Typical usage:

```lua
local loader = lpparam.classLoader
local pkg = lpparam.packageName
local apkPath = lpparam.appInfo.sourceDir
```

## Class Lookup and Import

### `findClass`

Use `findClass("full.name", loader)` to resolve a Java class.

```lua
local Activity = findClass("android.app.Activity", lpparam.classLoader)
```

### `imports`

Use `imports` for classes that may exist in the host app or in the module.

```lua
imports "android.os.Build"
local device = Build.DEVICE
```

Docs say `imports` loads globally and searches the host first, then the module.

### `import`

Use `import` for module-bundled classes. Docs say it supports wildcards.

```lua
import "java.lang.String"
import "java.util.*"
```

## Construction and Invocation

Direct Java-style construction is supported.

```lua
local obj = SomeClass()
local obj2 = SomeClass(1, "abc")
```

Call static methods with `Class.method()` and instance methods with `obj.method()`.

Use `invoke` for private or otherwise inaccessible methods.

```lua
local packageName = invoke(context, "getPackageName")
```

## Field Helpers

LuaHook documents four reflection-style field helpers:

- `getField(instance, "field")`
- `setField(instance, "field", value)`
- `getStaticField(clazz, "field")`
- `setStaticField(clazz, "field", value)`

## Utility Helpers

### `Task`

Runs a Lua function asynchronously after an optional delay.

```lua
Task(function()
  print("Hello")
end, 2000)
```

### `json`

- `json.decode(string)` returns a Lua table.
- `json.encode(table)` returns a JSON string.

### `http`

Documented async helpers:

- `http.get(url, ..., callback)`
- `http.post(url, body, ..., callback)`
- `http.download(url, path, ..., callback)`
- `http.upload(url, path, ..., callback)`

Callbacks receive success state plus response data and status code.

## Local Source Docs

- `references/source-docs/zh/manual/hook.md`
- `references/source-docs/zh/manual/import.md`
- `references/source-docs/zh/manual/invoke.md`
- `references/source-docs/zh/manual/lpparam.md`
- `references/source-docs/zh/manual/field.md`
- `references/source-docs/zh/manual/task.md`
- `references/source-docs/zh/manual/http.md`
- `references/source-docs/zh/manual/json.md`
