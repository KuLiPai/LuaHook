# LuaHook Script Patterns

## Minimal Activity Hook

```lua
imports "android.widget.Toast"

hook {
  class = "android.app.Activity",
  method = "onCreate",
  params = {"android.os.Bundle"},
  after = function(it)
    Toast.makeText(it.thisObject, "Hello LuaHook!", Toast.LENGTH_SHORT).show()
  end,
}
```

Use this when the user needs a first working proof of life.

## Hook Template With Clear Intent

```lua
hook {
  class = "com.example.TargetClass",
  method = "targetMethod",
  params = {"java.lang.String", "int"},
  before = function(it)
    local arg0 = it.args[0]
    local arg1 = it.args[1]
  end,
  after = function(it)
    local result = it.result
  end,
}
```

Fill this when the user knows class, method, and parameter types.

## Skip Original Logic

For zero-argument methods, keep `params = {}` explicit.

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

Use this when the goal is to bypass the original method entirely.

## Full Replacement

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

Use this when the user wants complete control over the return value.

## Constructor Hook

```lua
hookctor {
  class = "com.example.TargetClass",
  params = {},
  after = function(it)
    print(it.thisObject)
  end,
}
```

## Private Method Access

```lua
hook {
  class = "android.app.Application",
  method = "attach",
  params = {"android.content.Context"},
  after = function(it)
    local context = it.args[0]
    local packageName = invoke(context, "getPackageName")
    print(packageName)
  end,
}
```

## Reflection-Style Field Edit

```lua
hook {
  class = "com.example.TargetClass",
  method = "targetMethod",
  params = {},
  after = function(it)
    setField(it.thisObject, "someFlag", true)
  end,
}
```

## Delayed Follow-Up Work

```lua
Task(function()
  print("Delayed work")
end, 1000)
```

Use this for deferred UI or async follow-up behavior.

## HTTP Callback Pattern

```lua
http.get("https://httpbin.org/get", function(success, body, code)
  if success then
    print(body)
  else
    print(code)
  end
end)
```

## Native Injection Pattern

```lua
imports "java.lang.System"

local soName = "libcloud.so"

hook {
  class = "android.app.Application",
  method = "attach",
  params = {"android.content.Context"},
  after = function(it)
    local context = it.args[0]
    local packageName = invoke(context, "getPackageName")
    local path = "/data/data/" .. packageName .. "/files/" .. soName
    local ok, err = pcall(function()
      System.load(path)
    end)
    if not ok then
      print(err)
    end
  end,
}
```

## Writing Guidance

- Start with the simplest working hook.
- Add `before` only when you need to alter inputs or short-circuit execution.
- Add `after` when you need host objects, result inspection, or UI work.
- Prefer readable explicit code for first drafts.
- Keep `params = {}` explicit for zero-argument table-style hooks and replaces.
- Use LuaJ++ shorthand only after correctness is established.

## Local Source Docs

- `references/source-docs/zh/firstscript.md`
- `references/source-docs/zh/example/monitor-volume-buttons.md`
- `references/source-docs/zh/example/inject-so-file.md`
