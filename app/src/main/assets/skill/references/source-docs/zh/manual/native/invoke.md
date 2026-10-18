# Native函数调用

## 1. 创建Native函数对象

使用 `native.new_function` 为Native函数地址创建一个包装器。

```lua

local func = native.new_function(address, returnType, paramTypes)

```

- **address**: Native函数的内存地址（Long 类型）。

- **returnType**: 返回类型字符串。支持的类型有：`void`、`int`、`float`、`double` 和 `pointer`。

- **paramTypes**: 包含参数类型字符串的表。支持的类型有：`int`、`float`、`double` 和 `pointer`。

**示例：**

```lua

local base = native.resolve_symbol("libexample.so", "some_exported_func")

local func = native.new_function(base, "void", {"int", "int"})

```

---

## 2. 调用函数

函数对象创建完成后，即可直接调用。

```lua

func(arg1, arg2...)

```
- 直接传递参数。

- 返回值与指定的 `returnType` 匹配。

**示例：**

```lua

func(10, 20)

```