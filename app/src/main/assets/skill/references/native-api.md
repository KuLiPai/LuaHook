# LuaHook Native API Notes

Use this reference only when the user is working with native libraries or memory addresses. For ordinary app hooks, stay with `hook` and related Java-layer APIs.

## Core Native Concepts

- `native.module_base("libname.so")` returns a `LuaPointer` base address.
- `native.resolve_symbol("libc.so", "fopen")` resolves an exported symbol address.
- `native.hook(target_addr, config)` installs an inline native hook.
- `native.ptr(value)` creates a `LuaPointer`.
- `native.memory.*` is the preferred modern memory API family.

## `LuaPointer`

The docs describe `LuaPointer` as a safer pointer wrapper that preserves 64-bit address behavior better than raw Lua numbers.

Useful methods documented:

- `is_null()`
- `not_null()`
- `to_hex()`
- `to_int()`
- `add(offset)`
- `sub(offset)`
- `equals(other)`
- `deref([offset])`
- `hexdump([size])`

Docs also describe pointer read and write chaining such as `read_ptr()` and `write_u32()`.

## Native Hook Shape

```lua
local base = native.module_base("libgame.so")
if base.is_null() then
  return
end

native.hook(base.add(0x1234), {
  ret = "int",
  onEnter = function(ctx)
  end,
  onLeave = function(ret)
    return ret
  end,
})
```

## Native Hook Callback Notes

- `onEnter(ctx)` runs before the target function.
- `ctx[0]`, `ctx[1]`, and similar access general-purpose registers, not abstract argument numbers across all ABIs.
- Docs explicitly frame these examples around ARM64 register usage.
- `onLeave(ret)` receives the original return value and may return a replacement.

## Compatibility Guidance

- Prefer `native.module_base` over `native.get_module_base`.
- Prefer `native.memory.*` over old top-level `native.read*` or `native.write*` helpers.
- Keep pointer values as `LuaPointer` objects rather than plain Lua numbers whenever 64-bit precision matters.

## Safety Guidance

- Native hooks are version-sensitive and crash-prone.
- If the user does not provide ABI, module name, and offset or symbol name, ask for them or clearly label assumptions.

## Local Source Docs

- `references/source-docs/zh/manual/native/hook.md`
- `references/source-docs/zh/manual/native/module.md`
- `references/source-docs/zh/manual/native/pointer.md`
- `references/source-docs/zh/manual/native/legacy.md`
