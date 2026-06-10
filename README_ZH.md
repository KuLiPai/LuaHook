# LuaHook

<p align="center">
  <img src="docs/logo.png" alt="LuaHook Logo" width="550"/>
</p>

<p align="center">
  <b>一个基于 Lua 的 Xposed 脚本框架</b>
</p>


简体中文 | [English](README.md)

---

## 📖 项目简介

LuaHook 是一个正在积极开发中的框架，允许开发者使用轻量且灵活的 Lua 语言来编写强大的 Xposed 模块。

它简化了 Android Hook 开发流程，无需完整的 Java/Kotlin 编译构建，即可快速开发与迭代。

---

## ✨ 特性

- **Lua 脚本支持**  
  使用简洁直观的 Lua 语法编写 Hook 逻辑。

- **无缝集成 Xposed**  
  深度对接 Xposed 框架，实现稳定强大的 Hook 能力。

- **动态脚本加载**  
  无需重新编译或安装 APK，即可加载并应用脚本。

- **快速开发迭代**  
  相比传统 Xposed 开发方式，大幅提升开发效率。

- **更多特性持续开发中...**

---

## ⚠️ 开发状态

本项目目前仍处于早期开发阶段，可能存在 Bug、不完整功能或破坏性更新。

---

## 📬 联系方式

- Telegram：https://t.me/LuaXposed  
- QQ：https://qm.qq.com/q/Qt3yKDzCeG  

欢迎通过 GitHub Issue 提交问题、建议或反馈。

---

## 🙏 致谢

- DexKit  
- XpHelper  
- NeLuaJ  
- sora-editor  
- sora-editor-with-androlua  

---

## 💡 示例

```lua
imports "top.sacz.xphelper.dexkit.FieldFinder"
imports "java.lang.reflect.Modifier"
imports "top.sacz.xphelper.dexkit.bean.MethodInfo"

hook {
  class="android.app.Application",
  method="attach",
  params={"android.content.Context"},
  after=function(it)
    XpHelper.initContext(it.thisObject)
    XpHelper.injectResourcesToContext(it.thisObject)
    local loader = invoke(it.thisObject, "getClassLoader")
    local dexFinder = DexFinder.INSTANCE
    dexFinder.create(lpparam.appInfo.sourceDir)

    local method = MethodInfo() {
      UsedString = { "MicroMsg.QRCodeHandler", "qbar_string_scan_source" },
      ParamCount = 2,
    }.generate().firstOrNull()

    hook {
      method=method,
      before=function(it)
        it.args[1].putString("result_code_name", "WX_CODE")
      end
    }
  end
}
```

## 💰 支持项目

### 微信赞赏
使用 LuaHook 勾选微信并加载脚本后，扫描下方二维码：

<p align="center"> <img src="app/src/main/res/drawable/wechat_qr.png" width="220"/> </p>

### 其他方式
TON

```text
UQCT4SxRvop52iLADb8_TcuoGFlr8UqC4QNTlIraRcljm-Us
```
USDT (TRC20)

```text
TLhumaxCuCJYddWwfAyS9ZyVWeFbwUfydm
```
TRX (TRC20)

```text
TGGvqp4zx9VNT6HaijAQxQT8uFibs1etxt
```

## ⭐ 支持项目
如果这个项目对你有帮助，欢迎点一个 ⭐ 支持一下。