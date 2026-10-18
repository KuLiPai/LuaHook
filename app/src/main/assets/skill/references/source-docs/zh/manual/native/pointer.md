# 指针操作 (LuaPointer)

`LuaPointer` 是 NativeHook 中的核心对象，用于封装和操作内存地址。它比简单的 Lua number 更安全，支持链式调用，且能保持 64 位精度。

## 创建指针

### 使用 `native.ptr`

```lua
local p = native.ptr(0x12345678)
local p2 = native.ptr("0x1A2B3C")
```

### 使用 `native.memory.alloc`

```lua
local p3 = native.memory.alloc(32)
```

---

## 基础操作

### 判空

```lua
p.is_null()  -- 检查是否为 Null (0)
p.not_null() -- 检查是否非空
```

### 转换

```lua
p.to_hex()  -- 转为大写 HEX 字符串 (无 0x 前缀)
p.to_int()  -- 转为 number (可能丢失 64 位精度)
p.to_long() -- 返回自身 (兼容性接口)
```

---

## 指针运算

所有运算返回新的 `LuaPointer` 对象，原对象不变。

### 偏移计算

```lua
local p2 = p.add(0x10) -- 加法
local p3 = p.sub(0x04) -- 减法
```

### 位运算

```lua
p.and(mask) -- 按位与
p.or(mask)  -- 按位或
p.xor(mask) -- 按位异或
p.shl(bits) -- 左移
p.shr(bits) -- 右移
```

### 比较

```lua
if p.equals(other_ptr) then
    log("地址相同")
end
```

### 赋值 (Set)

```lua
-- 创建一个指向新地址的指针对象
local new_ptr = p.set(0x5566)
```

---

## 链式读写

`LuaPointer` 对象自身携带了所有 `native.memory.*` 的读写方法。
**调用时，默认地址为指针本身，`offset` 参数依然可用。**

```lua
local p = native.memory.alloc(64)

-- 等价于 native.memory.write_u32(p, 100, 0)
p.write_u32(100) 

-- 支持偏移：等价于 native.memory.write_u32(p, 200, 4)
p.write_u32(200, 4) 

-- 链式解引用
-- 读取 p 处的指针，然后偏移 0x10，再读取一个 u32
local val = p.read_ptr().add(0x10).read_u32()
```

### 常用快捷方法

| 方法 | 描述 |
| :--- | :--- |
| `p.deref([offset])` | 解引用。读取 `p` 处的指针值。等价于 `p.read_ptr(offset)` |
| `p.hexdump([size])` | 打印内存 Hex Dump，方便调试。默认 size=256 |
