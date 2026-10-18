# 字符串操作

`native.memory` 提供了一组工具来处理 Native 内存中的字符串，涵盖 C 风格字符串和长度前缀字符串。

## C 风格字符串 (`\0` 结尾)

### 读取 (`read_cstr`)

```lua
native.memory.read_cstr(addr, [max_len]) -> string | nil
native.memory.read_cstr_utf8(addr, [max_len]) -> string | nil
```

- **区别**：`read_cstr` 按原始字节读取，`read_cstr_utf8` 尝试做 UTF-8 解码。
- `max_len`: 可选，最大读取长度（防溢出），默认 512。
- **返回**: Lua字符串，遇到 `\0` 截断。

### 写入 (`write_cstr`)

```lua
native.memory.write_cstr(addr, str) -> boolean
native.memory.write_cstr_utf8(addr, str) -> boolean
```

- 会自动在字符串末尾补 `\0`。

**示例：**

```lua
local buf = native.memory.alloc(64)
native.memory.write_cstr_utf8(buf, "你好, LuaHook")
local txt = native.memory.read_cstr_utf8(buf)
log(txt) -- "你好, LuaHook"
native.memory.free(buf)
```

---

## 长度前缀字符串 (Len-Prefixed)

处理首字节为长度的自定义字符串格式。

### 读取 (`read_lp_utf8`)

```lua
native.memory.read_lp_utf8(addr, [max_len]) -> string | nil
```

- **格式**: `[Length (1 byte)] [Body ...]`
- 读取第一个字节 `len`，然后读取后续 `len` 个字节并转为 UTF-8。

### 写入 (`write_lp_utf8`)

```lua
native.memory.write_lp_utf8(addr, str, [max_len]) -> boolean
```

- 写入长度字节，随后写入内容。

---

## 自动识别 (`read_auto_utf8`)

```lua
native.memory.read_auto_utf8(addr, [max_len]) -> string | nil
```

**逻辑**：
1. 检查首字节是否在 `[1..0x7F]` 范围内。
2. 尝试该长度的内存内容是否符合 UTF-8 打印字符特征。
3. 若符合则按长度前缀解析；否则回退到 `read_cstr` 模式。
