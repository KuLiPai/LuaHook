# LuaHook

<p align="center">
  <img src="docs/logo.png" alt="LuaHook Logo" width="550"/>
</p>

<p align="center">
  <b>A Lua-powered Xposed scripting framework</b>
</p>

[简体中文](README_ZH.md) | English

---

## 📖 Introduction

LuaHook is an actively developed framework that enables developers to write powerful Xposed modules using the lightweight and flexible Lua language.

It simplifies Android hooking by removing the need for full Java/Kotlin build cycles, allowing faster development and iteration.

---

## ✨ Features

- **Lua Script Support**  
  Write hook logic using concise and expressive Lua syntax.

- **Seamless Xposed Integration**  
  Deep integration with the Xposed framework for robust hooking capabilities.

- **Dynamic Script Loading**  
  Load and apply scripts without recompiling or reinstalling the APK.

- **Rapid Prototyping**  
  Iterate faster compared to traditional Xposed module development.

- **More Features Coming Soon...**

---

## ⚠️ Project Status

This project is currently in an early development stage. Bugs, incomplete features, and breaking changes may occur.

---

## 📬 Contact

- Telegram: https://t.me/LuaXposed  
- QQ: https://qm.qq.com/q/Qt3yKDzCeG  

You are welcome to submit issues, suggestions, or bug reports via GitHub Issues.

---

## 🙏 Acknowledgements

- DexKit  
- XpHelper  
- NeLuaJ  
- sora-editor  
- sora-editor-with-androlua  

---

## 💡 Example

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

## 💰 Support
### WeChat
Scan the QR code below after enabling LuaHook in WeChat:

<p align="center"> <img src="app/src/main/res/drawable/wechat_qr.png" width="220"/> </p>

### Other Methods

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

## ⭐ Support the Project
If you find this project useful, consider giving it a ⭐ on GitHub.