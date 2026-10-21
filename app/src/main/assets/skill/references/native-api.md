# LuaHook Native API Reference

Use this reference when working with native C/C++ libraries (`.so`), inline native hooks, memory read/write, or pointer manipulation.

---

## 1. Core Native Concepts

- `native.module_base("libname.so")` -> returns `LuaPointer` base address of a loaded ELF shared library.
- `native.resolve_symbol("libname.so", "symbol_name")` -> resolves an exported symbol address into a `LuaPointer`.
- `native.ptr(addr)` -> wraps a number, hex string, or address into a `LuaPointer`.
- `native.hook(target_addr, config)` -> installs an inline native hook.
  - `native.new_function(address, returnType, paramTypes)` -> creates a callable wrapper object for a native function address.
- `native.memory.*` -> comprehensive modern memory read, write, allocation, and string operations.

---

## 2. `LuaPointer` Object

`LuaPointer` encapsulates 64-bit native memory addresses safely, preventing precision loss from standard Lua numbers, and supports direct chained memory operations.

### Creation
```lua
local p1 = native.ptr(0x7f12345678)
local p2 = native.ptr("0x7f12345678")
local p3 = native.module_base("libil2cpp.so")
local p4 = native.memory.alloc(64)
```

### Null Check & Inspection
```lua
p.is_null()   -- boolean: true if address == 0
p.not_null()  -- boolean: true if address != 0
p.to_hex()    -- uppercase hex string (e.g. "7F12345678", without 0x)
p.to_int()    -- number (note: may lose precision on 64-bit addresses)
p.to_long()   -- returns self (compatibility)
p.hexdump([size]) -- logs or dumps hex memory representation (default size 64)
```

### Pointer Arithmetic & Bitwise (all return a new `LuaPointer`)
```lua
local p_offset = p.add(0x10)  -- address + 0x10
local p_sub    = p.sub(0x04)  -- address - 0x04
local p_and    = p.and(mask)  -- bitwise AND
local p_or     = p.or(mask)   -- bitwise OR
local p_xor    = p.xor(mask)  -- bitwise XOR
local p_shl    = p.shl(bits)  -- left shift
local p_shr    = p.shr(bits)  -- right shift
local p_set    = p.set(0x123) -- create new pointer with new address
p.equals(other)               -- boolean comparison
```

### Chained Pointer Dereferencing & Read/Write
`LuaPointer` directly implements all `native.memory.*` methods with self as the base address:
```lua
-- Read pointer at p, add 0x10 offset, then read a uint32
local val = p.read_ptr().add(0x10).read_u32()

-- Write directly to pointer
p.write_u32(100)       -- at p + 0
p.write_u32(200, 0x04) -- at p + 0x04
p.write_cstr("hello")
```

---

## 3. `native.memory.*` API

All `addr` parameters support `LuaPointer`, integer `number`, or hex `string`.

### Integer Read & Write
| Width | Type | Read Function | Write Function | Notes |
| :--- | :--- | :--- | :--- | :--- |
| **8-bit** | Unsigned | `native.memory.read_u8(addr, [offset])` | `native.memory.write_u8(addr, value, [offset])` | 0 ~ 255 |
| | Signed | `native.memory.read_s8(addr, [offset])` | `native.memory.write_s8(addr, value, [offset])` | -128 ~ 127 |
| **16-bit**| Unsigned | `native.memory.read_u16(addr, [offset])` | `native.memory.write_u16(addr, value, [offset])` | 0 ~ 65535 |
| | Signed | `native.memory.read_s16(addr, [offset])` | `native.memory.write_s16(addr, value, [offset])` | -32768 ~ 32767 |
| **32-bit**| Unsigned | `native.memory.read_u32(addr, [offset])` | `native.memory.write_u32(addr, value, [offset])` | Lua number (UInt32) |
| | Signed | `native.memory.read_s32(addr, [offset])` | `native.memory.write_s32(addr, value, [offset])` | Lua number (Int32) |
| **64-bit**| Unsigned | `native.memory.read_u64(addr, [offset])` | `native.memory.write_u64(addr, value, [offset])` | **Returns `LuaPointer`** |
| | Signed | `native.memory.read_s64(addr, [offset])` | `native.memory.write_s64(addr, value, [offset])` | **Returns `LuaPointer`** |
| **Pointer**| Architecture width | `native.memory.read_ptr(addr, [offset])` | `native.memory.write_ptr(addr, ptrVal, [offset])`| Auto 32/64 bit pointer |

### Floating Point Read & Write
| Type | Read Function | Write Function |
| :--- | :--- | :--- |
| Float (32-bit) | `native.memory.read_f32(addr, [offset])` | `native.memory.write_f32(addr, value, [offset])` |
| Double (64-bit)| `native.memory.read_f64(addr, [offset])` | `native.memory.write_f64(addr, value, [offset])` |

### Raw Byte Arrays
```lua
-- Read size bytes into a Lua table of integers [1..size] (0-255)
local bytes = native.memory.read_byte_array(addr, size, [offset])

-- Write Lua table of byte values into memory
native.memory.write_byte_array(addr, { 0x90, 0x90, 0x00 }, [offset])
```

### Strings
```lua
-- C-style null-terminated strings
local str = native.memory.read_cstr(addr, [max_len=512])
local strUtf8 = native.memory.read_cstr_utf8(addr, [max_len=512])
native.memory.write_cstr(addr, "my_string")
native.memory.write_cstr_utf8(addr, "中文内容")

-- Length-prefixed strings (1-byte length + body)
local lpStr = native.memory.read_lp_utf8(addr, [max_len])
native.memory.write_lp_utf8(addr, "string", [max_len])

-- Auto-detect (length-prefixed if printable, otherwise cstr)
local autoStr = native.memory.read_auto_utf8(addr, [max_len])
```

### Allocation & Free
```lua
local buf = native.memory.alloc(128) -- returns LuaPointer
-- ... use buf ...
native.memory.free(buf) -- returns boolean
```

---

## 4. Native Inline Hooking (`native.hook`)

```lua
local base = native.module_base("libil2cpp.so")
if base.is_null() then
  log("libil2cpp.so not loaded yet")
  return
end

local targetAddr = base.add(0x123456) -- Function offset

native.hook(targetAddr, {
  ret = "int", -- Return type: "void", "int", "pointer", "bool", "float", "double"
  onEnter = function(ctx)
    -- On ARM64: ctx[0], ctx[1], ctx[2] ... correspond to registers X0, X1, X2 ...
    -- ctx[0] is typically 'this' for C++ member methods or first argument
    local arg0 = ctx[0]
    local arg1 = ctx[1]
    log("onEnter called with arg0=" .. tostring(arg0) .. ", arg1=" .. tostring(arg1))

    -- Modify argument register
    ctx[1] = 9999
  end,
  onLeave = function(ret)
    log("onLeave original return value: " .. tostring(ret))
    -- Return replacement value or original
    return 1
  end,
})
```

---

## 5. Native Function Calls (`native.new_function`)

### 1. Creating a Native Function Object
Use `native.new_function` to create a wrapper for a native function address:

```lua
local func = native.new_function(address, returnType, paramTypes)
```
- **address**: The memory address of the native function (Long or `LuaPointer`).
- **returnType**: The return type string. Supported: `"void"`, `"int"`, `"float"`, `"double"`, `"pointer"`.
- **paramTypes**: A table containing parameter type strings. Supported: `"int"`, `"float"`, `"double"`, `"pointer"`.

**Example:**
```lua
local base = native.resolve_symbol("libexample.so", "some_exported_func")
local func = native.new_function(base, "void", {"int", "int"})
```

### 2. Invoking the Function
Once the function object is created, you can call it directly:

```lua
func(arg1, arg2...)
```
- Pass arguments directly.
- The return value matches the specified `returnType`.

**Example:**
```lua
func(10, 20)
```
