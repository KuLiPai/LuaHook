# 内存读写

`native.memory` 提供了一套全面的内存读写接口，支持整数、浮点数和原始字节数组。

所有接口的 `addr` 参数均支持：
- `LuaPointer` 对象
- `number` (地址数值)
- `string` (十六进制字符串)

---

## 读写整数

### 基础类型表

| 位宽 | 类型 | 读取接口 | 写入接口 | 范围/备注 |
| :--- | :--- | :--- | :--- | :--- |
| **8位** | 无符号 | `read_u8` | `write_u8` | 0 ~ 255 |
| | 有符号 | `read_s8` | `write_s8` | -128 ~ 127 |
| **16位** | 无符号 | `read_u16` | `write_u16` | 0 ~ 65535 |
| | 有符号 | `read_s16` | `write_s16` | -32768 ~ 32767 |
| **32位** | 无符号 | `read_u32` | `write_u32` | Lua number (UInt32) |
| | 有符号 | `read_s32` | `write_s32` | Lua number (Int32) |
| **64位** | 无符号 | `read_u64` | `write_u64` | **返回 LuaPointer** 以保精度 |
| | 有符号 | `read_s64` | `write_s64` | **返回 LuaPointer** 以保精度 |
| **指针** | 指针宽 | `read_ptr` | `write_ptr` | 自动适配 32/64 位架构 |

### 接口定义

**读取：**
```lua
native.memory.read_u32(addr, [offset]) -> number
```
- `offset`: 可选，基于 `addr` 的偏移量，默认为 0。

**写入：**
```lua
native.memory.write_u32(addr, value, [offset]) -> boolean
```
- `value`: 要写入的数值。
- **返回**: `true` 表示写入成功。

**示例：**

```lua
local buf = native.memory.alloc(16)

-- 写入
native.memory.write_u32(buf, 0x1234, 0)
native.memory.write_s64(buf, 0x1122334455667788, 4)

-- 读取
local val = native.memory.read_u32(buf, 0) -- 0x1234
local val64 = native.memory.read_s64(buf, 4) -- 返回 LuaPointer

native.memory.free(buf)
```

---

## 读写浮点数

| 类型 | 读取接口 | 写入接口 |
| :--- | :--- | :--- |
| Float (32位) | `read_f32` | `write_f32` |
| Double (64位) | `read_f64` | `write_f64` |

**示例：**

```lua
native.memory.write_f32(addr, 3.14, 0)
local pi = native.memory.read_f32(addr, 0)
```

---

## 读写字节数组

用于批量读取或写入原始二进制数据。

### 读取数组 (`read_byte_array`)

```lua
native.memory.read_byte_array(addr, size, [offset]) -> table | nil
```

- `size`: 读取字节数。
- **返回**: Lua table，包含 `[1..size]` 个 `0-255` 的整数。

### 写入数组 (`write_byte_array`)

```lua
native.memory.write_byte_array(addr, table, [offset]) -> boolean
```

- `table`: 包含字节数据的 Lua table。

**示例：**

```lua
-- 写入 HEX: 41 42 43
native.memory.write_byte_array(addr, {0x41, 0x42, 0x43})

-- 读取
local bytes = native.memory.read_byte_array(addr, 3)
-- bytes[1] == 65
```
