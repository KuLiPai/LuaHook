# 简介

## 1. 什么是 LuaHook

[LuaHook](https://github.com/kulipai/luahook) 是一个正在积极开发中的开源模块，旨在通过轻量灵活的 Lua 语言，让开发者能够高效地编写功能强大的 Xposed 脚本。

相比于传统的 Frida 方案，LuaHook 同时具备 **持久化运行** 和 **动态脚本加载** 的优势，避免了 Frida 在持久化能力和 JavaScript 学习门槛方面的短板。

相较于基于 Java 的 Xposed 模块，LuaHook 同样保留了对系统深度集成的持久特性，并额外提供了无需重新编译 APK 即可实时修改脚本的能力。

结合 Lua 简洁易学的语法，LuaHook 为开发者提供了一个**高效、灵活、易上手**的解决方案，大幅降低了 Xposed 开发的门槛，既满足了实时调试的需求，也支持长久部署的场景。

---

## 2. 语言要求

使用 LuaHook 你只需要掌握基础的 Lua 语法 —— 如果你了解 Java 或 Kotlin 会更容易上手，但并非必需。

即使你没有编程基础，也不用担心：Lua 是一种非常简单的脚本语言，语法极其轻量，几分钟就能学会基础用法。

下面是几个简单示例：

```lua
-- 这是注释
print("Hello, LuaHook!")
-- 输出: Hello, LuaHook!

a = 2 + 3
print(a)
-- 输出: 5

if a > 4 then
  print("a is greater than 4")
end
```

你可以通过以下教程快速入门：

- [菜鸟教程 - Lua 入门](https://www.runoob.com/lua/lua-tutorial.html)
- [Luaj++ 快速指北（推荐）](./luaj++) 

---

## 3. 为什么选择 LuaHook

- ✅ **无需编译打包**：修改脚本即生效，适合调试与快速迭代。
- ✅ **持久化运行支持**：相比 Frida，更适合长期部署。
- ✅ **语法简单上手快**：无需 Java 背景也能写出 Hook 脚本。
- ✅ **支持 Root 与非 Root 环境**：适用于更多用户场景。
- ✅ **可动态加载脚本**：结合 Lua 的热加载特性，体验丝滑。

---

## 贡献

如果您是学生或热心开发人员并希望做出贡献，我们有一个[想法清单](./ideas)，这将是一件很酷的事情！:-)

