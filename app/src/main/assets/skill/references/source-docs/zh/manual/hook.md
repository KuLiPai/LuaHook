# Hook 使用说明文档

本模块提供了四种 Hook 接口形式：

  - `hook`：用于 Hook 指定的方法
  - `hookctor`：用于 Hook 构造函数（构造方法）
  - `hookAll`：用于 Hook 某个类中所有同名方法（重载方法）
  - `replace`：用于完全替换指定方法的实现

所有 Hook 形式均支持**新式配置表语法**，这种语法提高了可读性并使其参数更明确。旧的函数参数列表语法依然保留，以保持向后兼容性。

`before` 和 `after` 钩子函数（以及 `replace` 的替换函数）均接收一个参数 `it`（即 `MethodHookParam`），其中包含当前 Hook 的上下文信息。

-----

## 1. `hook` —— Hook 单个方法

`hook` 用于对单个 Java 方法进行拦截。它支持前置 (`before`) 和后置 (`after`) 钩子。

### 推荐新语法

新语法通过一个 Lua `table` 来组织 Hook 的所有参数，提高了可读性和灵活性。`before` 和 `after` 钩子都是可选的，你只写需要的即可。

**用法一：通过类名、方法名和参数类型 Hook 方法**

```lua
hook {
  class = "com.xx",           -- 必填：目标类的全限定名字符串 或 Class 类型的类对象
  --classloader = lpparam.classLoader, -- 可选：用于加载类的 ClassLoader，不填默认 lpparam.classLoader
  method = "f1",              -- 必填：要 Hook 的方法名
  params = {"int", "String", findClass("com.my.class", lpparam.classLoader)}, -- 可选：一个 table，包含参数类型字符串或 Class 对象
  before = function(it)
    -- before hook：在原方法执行前执行
    print("进入 f1 方法前")
  end,
  after = function(it)
    -- after hook：在原方法执行后执行
    print("离开 f1 方法后，返回值:", it.result)
  end,
}
```

**用法二：直接 Hook `Method` 对象**

如果你已经通过 `DexKit` 或其他方式获取了 `Method` 实例，可以直接传入。

```lua
local myMethod = someMethodRetrievalFunction("com.xx", "f1", "String") -- 假设这是获取 Method 对象的方法

hook {
  method = myMethod,          -- 必填：Method 类型的量
  before = function(it)
    print("Method 对象 Hook 前")
  end,
  -- after 可选，此处省略
}
```

-----

### 旧语法

旧语法作为兼容性选项保留。

**用法一：通过类名、类加载器、方法名、参数类型列表 Hook 方法**

```lua
hook("com.xx",
    lpparam.classLoader,
    "f1",
    "int", "String", "com.my.class", -- 参数类型可变参数
    function(it)
        -- before
    end,
    function(it)
        -- after
    end
)
```

**用法二：直接 Hook 方法对象**

```lua
hook(method, -- 直接传入 Method 对象
    function(it)
        -- before
    end,
    function(it)
        -- after
    end
)
```

-----

## 2. `hookctor` —— Hook 构造方法（Constructor）

`hookctor` 用于拦截类的构造方法。它没有 `method` 属性，因为目标总是构造函数。

### 推荐新语法

```lua
hookctor {
  class = "com.xx",           -- 必填：目标类的全限定名字符串 或 Class 类型的类对象
  classloader = lpparam.classLoader, -- 可选：用于加载类的 ClassLoader，不填默认 lpparam.classLoader
  params = {"int", "String"}, -- 可选：一个 table，包含构造函数的参数类型
  before = function(it)
    print("构造函数调用前")
  end,
  after = function(it)
    print("构造函数调用后")
  end,
}
```

### 旧语法

```lua
hookctor("com.xx",
    lpparam.classLoader,
    "int", "String", -- 构造函数参数类型
    function(it)
        -- before
    end,
    function(it)
        -- after
    end
)
```

-----

## 3. `hookAll` —— Hook 所有同名方法

`hookAll` 用于 Hook 某个类中**所有同名（重载）方法**。它不支持 `method` 属性或直接传入 `Method` 对象，因为其目标是所有重载。

### 推荐新语法

```lua
hookAll {
  class = "com.xx",           -- 必填：目标类的全限定名字符串 或 Class 类型的类对象
  classloader = lpparam.classLoader, -- 可选：用于加载类的 ClassLoader，不填默认 lpparam.classLoader
  before = function(it)
    print("Hook 所有重载方法前，当前方法:", it.method.getName())
  end,
  after = function(it)
    print("Hook 所有重载方法后，当前方法:", it.method.getName(), "返回值:", it.result)
  end,
}
```

### 旧语法（函数参数列表形式）

```lua
hookAll("com.xx",
    lpparam.classLoader,
    function(it)
        -- before
    end,
    function(it)
        -- after
    end
)
```

-----

## 4. `replace` —— 完全替换方法实现

`replace` 方法用于**完全取代**目标方法的原始实现。一旦方法被 `replace`，其原有的功能将不再执行，而是由你传入的替换函数来接管。`replace` 没有 `before` 和 `after` 钩子，因为它本身就是终极替换。

### 推荐新语法（配置表形式）

```lua
replace {
  class = "com.xx",           -- 必填：目标类的全限定名字符串 或 Class 类型的类对象
  classloader = lpparam.classLoader, -- 可选：用于加载类的 ClassLoader，不填默认 lpparam.classLoader
  method = "f1",              -- 必填：要替换的方法名
  params = {"int", "String"}, -- 可选：一个 table，包含参数类型字符串或 Class 对象
  replace = function(it)
    -- 这是替换后的方法体，完全取代原方法
    print("f1 方法被完全替换了！接收参数:", table.concat(it.args, ", "))
    -- 替换函数的返回值将成为被替换方法的最终返回值
    return "新的返回值"
  end,
}
```

**用法二：直接替换 `Method` 对象**

```lua
local targetMethod = someMethodRetrievalFunction("com.yy", "calculate")

replace {
  method = targetMethod,      -- 必填：Method 类型的量
  replace = function(it)
    print("calculate 方法被替换，原始 this 对象:", it.thisObject)
    return it.args[0] + it.args[1] -- 假设原方法是计算两个参数的和
  end,
}
```

### 旧语法（函数参数列表形式）

```lua
replace("com.xx",
    lpparam.classLoader,
    "f1",
    "int", "String",
    function(it)
        -- 这是替换后的方法体
        print("f1 方法被替换了！参数:", table.concat(it.args, ", "))
        return 123 -- 返回值会成为被替换方法的最终返回值
    end
)
```

-----

## `it` 参数说明（即 `MethodHookParam`）

> 可以简单地理解成函数的相关信息。

无论是 `hook` 的 `before`/`after` 钩子，还是 `replace` 方法的替换函数，都会接收到一个 **`it` 参数**（即 `MethodHookParam` 实例），它提供了当前 Hook 或替换操作的上下文信息：

  - `it.method`: 当前 Hook/替换的 **`java.lang.reflect.Method` 或 `Constructor` 对象**。你可以通过 `it.method.getName()` 获取方法名，`it.method.getDeclaringClass()` 获取所属类等。
  - `it.args`：一个 Lua `table`，包含了目标方法的**参数数组**。
      - `it.args[0]`：获取第一个参数。
      - `#it.args`：获取参数个数。
      - `it.args[0] = 1`：**修改参数值**。此操作仅在 `before` 钩子中对后续的原方法执行有效。在 `replace` 函数中修改 `it.args` **不会**影响任何原始方法调用（因为原方法不会被执行）。
  - `it.result`：
      - `it.result`：**获取返回值**。主要在 `after` 钩子中用于获取原方法的执行结果。
      - `it.result = true`：**修改返回值**。
          - 在 `before` 钩子中赋值 `it.result` 将会**直接跳过原方法的执行**，并使用 `it.result` 的值作为该方法的最终返回值。
          - 在 `after` 钩子中赋值 `it.result` 将会**覆盖原方法已经产生的结果**，作为该方法的最终返回值。
          - **在 `replace` 方法中，直接 `return` 替换函数的值**即可作为最终返回值，`it.result` 在此场景下不用于设定返回值。
  - `it.thisObject`：当前方法所属的**对象实例**（即 Java 中的 `this`）。
      - 对于非静态方法，你可以通过 `it.thisObject` 访问该对象的字段、调用其其他方法等。
      - 对于静态方法或构造函数（在 `after` 构造函数中 `it.thisObject` 代表新创建的对象实例），`it.thisObject` 可能为 `nil` 或代表类本身。

-----

