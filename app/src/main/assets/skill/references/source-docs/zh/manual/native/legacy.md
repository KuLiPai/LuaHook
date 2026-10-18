# 兼容性说明

随着 LuaHook 版本的迭代，Native 接口经历了多次重构。本页汇总了旧接口的去留情况以及升级指南。

## 已移除的旧接口

以下接口在最新版中**已被移除**或**不推荐使用**，请使用 `native.memory` 替代：

| 旧接口 | 替代方案 | 说明 |
| :--- | :--- | :--- |
| `native.read(ptr, size)` | `native.memory.read_byte_array` | 读取字节数组 |
| `native.write(ptr, data)` | `native.memory.write_byte_array` | 写入字节数组 |
| `native.readDword(ptr)` | `native.memory.read_u32` | 读 32位整数 |
| `native.readFloat(ptr)` | `native.memory.read_f32` | 读 Float |
| `native.readByte(ptr)` | `native.memory.read_u8` | 读 Byte |

*(对应 write 系列同理)*

## 保留的兼容接口

为了兼容旧脚本，以下顶级接口仍被保留，但建议新脚本使用规范的新接口：

- **`native.get_module_base`**
    - 推荐使用 `native.module_base`。
- **`native.readPoint(ptr, offsets)`**
    - 多级指针读取工具。
    - **逻辑**: 从 `ptr` 开始，依次读取偏移并解引用，直到最后一个偏移只相加不解引用。
    - ```lua
      -- 等价于 *(base + 0x10) + 0x20
      local val = native.readPoint(base, {0x10, 0x20})
      ```

## 注意事项

1. **64位精度**: Lua 的 `number` (double) 无法精确表示所有 64 位整数。处理指针地址或 64 位数值时，请务必使用 `read_u64`/`read_s64` 返回的 `LuaPointer` 对象。
2. **Hook 稳定性**: Hook 可能会导致目标应用崩溃（如修改了关键寄存器或访问非法内存）。编写 Hook 时请务必进行充分测试。
