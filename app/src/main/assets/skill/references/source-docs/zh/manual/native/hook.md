# Hook 接口

`native.hook` 是 NativeLayor 的核心功能，支持对任意内存地址的函数进行 Inline Hook。

## 接口定义

```lua
native.hook(target_addr, config) -> boolean
```

- **target_addr**: 目标函数绝对地址。可以通过基址+偏移算得，或通过符号解析得到。
- **config**: Hook 配置表。

### Config 结构

```lua
{
    ret = "void",       -- [可选] 返回值类型
    argc = 0,           -- [可选] 参数个数（影响栈参数读取）
    onEnter = function(ctx) ... end, -- [可选] 进入回调
    onLeave = function(retval) ... end -- [可选] 离开回调
}
```

- **ret 类型支持**: `"int"`, `"ptr"`, `"pointer"`, `"float"`, `"double"`, `"void"`。

---

## onEnter 回调

函数执行前调用。

```lua
onEnter = function(ctx)
    -- ctx 是上下文对象
end
```

### 上下文对象 (`ctx`)

| 属性 | 描述 |
| :--- | :--- |
| `ctx[i]` | 获取第 `i` 个通用寄存器 (GPR) 参数。**注意：这是寄存器索引，不是参数索引**。ARM64 下 `ctx[0]`~`ctx[7]` 对应参数 1~8。 |
| `ctx.raw` | 访问原始寄存器数组。 |
| `ctx.fpr` | 浮点寄存器数组。 |
| `ctx.stack` | 栈参数数组。 |

**修改参数示例：**

```lua
-- 修改第一个整型参数 (ARM64 x0)
ctx[0] = 100 

-- 修改第二个参数指针偏移
ctx[1] = ctx[1].add(0x20)
```

---

## onLeave 回调

函数执行后调用。

```lua
onLeave = function(retval)
    -- retval: 原始返回值
    -- return: (可选) 新的返回值
end
```

**修改返回值示例：**

```lua
onLeave = function(orig_ret)
    log("Original return: " .. orig_ret)
    return 1 -- 强制返回 1
end
```

---

## 完整示例

假设我们要 Hook `libgame.so` 偏移 `0x1234` 处的函数 `int add(int a, int b)`。

```lua
local base = native.module_base("libgame.so")
if base.is_null() then return end

native.hook(base.add(0x1234), {
    ret = "int",
    onEnter = function(ctx)
        local a = ctx[0].to_int() -- ARM64 第1个参数在 x0
        local b = ctx[1].to_int() -- ARM64 第2个参数在 x1
        log("add called with: " .. a .. ", " .. b)
        
        -- 强行把 b 改成 999
        ctx[1] = 999
    end,
    onLeave = function(ret)
        log("result: " .. ret)
        return ret * 2 -- 结果翻倍
    end
})
```
