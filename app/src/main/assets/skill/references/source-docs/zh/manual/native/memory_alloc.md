# 内存管理

`native.memory` 提供了手动管理堆内存的能力。请务必注意内存泄漏问题，手动申请的内存必须手动释放。

## 申请内存 (`alloc`)

申请指定大小的内存块。

```lua
native.memory.alloc(size) -> LuaPointer
```

- **参数**: `size` (number) —— 字节数。
- **返回**: `LuaPointer` 对象。如果 `size <= 0` 则返回 `nil` 或空指针对象。

**示例：**

```lua
local ptr = native.memory.alloc(1024) -- 申请 1KB
if ptr.is_null() then
    log("内存申请失效")
end
```

## 申请字符串内存 (`alloc_utf8_string`)

申请一块内存，并将 Lua 字符串按照 UTF-8 编码写入，末尾自动添加 `\0`。

```lua
native.memory.alloc_utf8_string(str) -> LuaPointer
```

- **参数**: `str` (string) —— 内容。
- **返回**: 指向字符串首地址的 `LuaPointer`。

**示例：**

```lua
local str_ptr = native.memory.alloc_utf8_string("Hello World")
-- 此时内存中为：48 65 6C 6C 6F 20 57 6F 72 6C 64 00
```

## 释放内存 (`free`)

释放由 `alloc` 系列函数申请的内存。

```lua
native.memory.free(ptr)
```

- **参数**: `ptr` (LuaPointer | number) —— 要释放的内存首地址。

**警告**:
1. 只能释放自己申请的内存。
2. 禁止对同一块内存重复释放（Double Free）。
3. 禁止释放未经申请的野指针。

**示例：**

```lua
local buf = native.memory.alloc(16)
-- 使用 buf ...
native.memory.free(buf)
```
