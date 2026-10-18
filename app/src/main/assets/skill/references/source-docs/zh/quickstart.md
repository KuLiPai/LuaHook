# 🚀 快速开始

欢迎使用 **LuaHook** —— 一个使用 Lua 编写 Xposed 模块的框架。本教程将帮助你在几分钟内完成安装和首次使用。

---

## 🧰 0. 准备工具

根据你的设备权限选择对应方式：

- **Root 用户**：

  - 安装 [LSPosed](https://github.com/LSPosed/LSPosed)
  - 配置 Root 管理器（如 Magisk）
- **非 Root 用户**：

  - 安装 [LSPatch](https://github.com/LSPosed/LSPatch)
  - 启动并授权 [Shizuku](https://github.com/RikkaApps/Shizuku)

---

## 📥 1. 下载 LuaHook

前往 GitHub [最新发布页](https://github.com/KuLiPai/LuaHook/releases/latest) 下载 LuaHook 安装包并安装到设备上。

---

## ✅ 2. 激活模块

- **Root 用户**：

  1. 在 Root 管理器中授予 LuaHook Root 权限。
  2. 打开 LSPosed，勾选要 Hook 的目标应用。
- **非 Root 用户**：

  1. 启动 Shizuku 并确保其正常运行。
  2. 启动 LuaHook 并授予其 Shizuku 权限。
  3. 使用 LSPatch 修补目标应用，将 LuaHook 加入模块作用域。

---

## ✍️ 3. 编写并启动 Hook 脚本

1. 打开 LuaHook，进入 **App 页面**。
2. 点击右下角 “+” 按钮，展开添加界面，选择你想 Hook 的应用。
3. 点击该应用卡片 → 点击右下角 “+” 按钮新建脚本，并给脚本取一个名字，进入编辑界面输入以下示例脚本：

```lua
imports "android.widget.Toast"

hook("android.app.Activity",
  lpparam.classLoader,
  "onCreate",
  "android.os.Bundle",
  function(it) end,
  function(it)
    Toast.makeText(it.thisObject, "LuaHook Hooked!", 1000).show()
  end
)
```

4. 保存脚本后，点击页面顶部的 **三角**（Start）图标启动目标应用。
5. 如果成功看到 Toast 提示 “LuaHook Hooked!” 则说明 <button id="firework">Hook 已生效 🎉！</button>

---

📺 **视频教程**：
想通过视频了解详细过程？你可以观看这段演示：[Bilibili 视频教程](https://www.bilibili.com/video/BV16njCz1Eyw/?share_source=copy_web&vd_source=002efab470a9b210cf443ba711f9d39a)
