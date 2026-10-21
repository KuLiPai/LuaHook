# LuaJ++ Cheat Sheet For LuaHook

LuaHook examples rely on LuaJ++, a Lua variant with Java-friendly syntax sugar. When generating code, distinguish clearly between plain Lua and LuaJ++-specific syntax.

## Safer Default

If the user did not ask for concise sugar, prefer explicit, readable forms over dense LuaJ++ shorthand.

## Documented Syntax Extensions

### Optional Keywords

Docs show these keywords may be omitted in LuaJ++:

- `then`
- `do`
- `in`
- `function`

Examples:

```lua
if ok
end

while running
end

local work()
end
```

### `switch` and `when`

```lua
switch value
  case 1,3,5
    print(1)
  default
    print(0)
end
```

```lua
result = when value
  case 1,3,5
    return 1
  default
    return 0
end
```

### `continue`

```lua
for n = 1,10
  if n % 2 == 0
    continue
  end
  print(n)
end
```

### `defer`

Docs say deferred statements run when the function exits in last-in-first-out order.

### Ternary-Style `if`

```lua
local value = if ok 1 else 0
```

### `try` / `catch` / `finally`

```lua
try
  error("err")
catch(e)
  print(e)
finally
  print("done")
end
```

### Lambda Forms

```lua
lambda a,b->a+b
lambda a,b=>print(a+b)
lambda () -> print("lambda")
```

## Java Interop Sugar

### Import Forms

```lua
import "java.lang.String"
import name "java.lang.String"
import "java.lang.*", "java.io.*"
```

### Class Construction and Interface Shortcuts

```lua
local list = ArrayList()
local map = HashMap()

local runnable = Runnable {
  run = function()
  end
}
```

Docs also show shorter callback assignment patterns such as setting `onClick` directly.

### Property-Style Getter and Setter Access

LuaJ++ docs show Java getter and setter sugar:

```lua
button.text = "hello"
local text = button.text
```

This corresponds to Java-style `setText` and `getText` where supported.

### `isXxx()` Shortcut

Docs show:

```lua
view.Activated
```

as shorthand for `view.isActivated()`.

### Arrays

```lua
local ints = int[10]
local values = int{1,2,3}
```

### Class Helpers & Advanced Interop

```lua
-- Create Java Array
local strArray = String.array{"apple", "banana", "orange"}
local intArray = int{1, 2, 3, 4, 5}
local emptyInts = int[10]

-- Access Class Constructor & Class Reflection
local newInstance = MyClass.new("arg1", 123)
local classObj = MyClass.class

-- Interface Implementation (Anonymous interface)
local clickListener = interface {
  onClick = function(view)
    print("Clicked: " .. tostring(view))
  end
}

-- Abstract Class Implementation
local customHandler = abstract {
  handleMessage = function(superCall, msg)
    print("Handling msg: " .. tostring(msg))
    superCall(msg)
  end
}

-- Method Overriding on Existing Java Class
local customList = ArrayList.override {
  add = function(superCall, element)
    print("Adding element: " .. tostring(element))
    return superCall(element)
  end
}()
```

## Operator Extensions Mentioned In Docs

Documented alternatives include:

- `!=` for `~=`
- full-width exclamation mark for `not`
- `&&` for `and`
- `||` for `or`
- compound assignments like `+=`, `-=`, `*=`, `/=` and more
- bitwise operations such as `&`, `|`, `~`, `>>`, `<<`

## Style Guidance

- Prefer standard Lua syntax if portability matters.
- Prefer LuaJ++ sugar when the user explicitly wants idiomatic LuaHook code or shorter Java interop.
- Avoid mixing too many sugar forms in one answer unless the user already writes that way.

## Local Source Doc

- `references/source-docs/zh/luaj++.md`
