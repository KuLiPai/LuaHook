# 模块与符号

在 Hook 之前，通常需要先获取目标 SO (Shared Object) 的内存基址，或直接查找导出函数地址。

## 获取模块基址 (`native.module_base`)

获取已加载模块的起始地址。

```lua
native.module_base(module_name) -> LuaPointer
```

- **参数**: `module_name` (string) —— 完整模块名，如 `libil2cpp.so`。
- **返回**: 模块基址。若未找到模块，通常返回 `0` (null)。

**别名兼容**：`native.get_module_base(name)` 与此功能相同。

**示例：**

```lua
local base = native.module_base("libunity.so")
if base.not_null() then
    log("Unity Base: " .. base.to_hex())
end
```

---

## 解析导出符号 (`native.resolve_symbol`)

查找 SO 导出表（Export Table）中的符号地址。

```lua
native.resolve_symbol(module_name, symbol_name) -> number
```

- **参数**:
    - `module_name`: 模块名。
    - `symbol_name`: 符号名（C++ 函数可能是 Mangled Name）。
- **返回**: 符号的绝对地址 (number)。未找到返回 0。

**示例：**

```lua
local fopen_addr = native.resolve_symbol("libc.so", "fopen")
log("fopen address: " .. string.format("0x%X", fopen_addr))
```

---

## 高级查找 (`getModuleBase`)

此接口保留用于更复杂的 `/proc/self/maps` 匹配。

```lua
native.getModuleBase(name, filter) -> number
```

- **Feature 1**: 支持查找 BSS 段。
    - `name` 传入 `libfoo.so:bss`，可查找该 SO 的 BSS 段地址。
- **Feature 2**: `filter` 过滤。
    - 传入权限字符串（如 `r-xp`）来精确匹配内存段。

```lua
-- 获取 libfoo.so 的 r-xp 段
local code_base = native.getModuleBase("libfoo.so", "r-xp")
```
