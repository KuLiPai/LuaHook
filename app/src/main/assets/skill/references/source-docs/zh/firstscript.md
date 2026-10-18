
# ✨ 编写你的第一个 LuaHook 脚本

## 第一步：选择目标应用

首先，选择一个你想 Hook 的目标应用。

## 第二步：在 Lsposed 中启用 LuaHook 并勾选目标应用

1. 打开 Lsposed，启用 LuaHook 模块。
2. 在模块设置中勾选你想 Hook 的目标应用：
    <img src="/assets/79f55709b78c719a417bfdd0f85641db-20250601181049-nc00r3v.jpg" alt="启用模块" width="60%">

---

## 第三步：在 LuaHook 中添加目标应用

1. 回到 LuaHook，进入「应用」页面。
2. 点击右下角「+」按钮，展开菜单：
     <img src="/assets/8bd128d71a1e9b8c687b441b1a7411ac-20250601181142-lfqwoq4.jpg" alt="展开添加应用" width="60%">
3. 选择「添加应用」：
    <img src="/assets/04056dd7ef43af075889294de17194d3-20250601181207-bpird32.jpg" alt="点击添加应用" width="60%">
4. 找到目标应用，勾选后点击右下角「✔」保存：
    <img src="/assets/88f1fb05e0503e87fa0e54220fbc0f34-20250601181405-cyurx06.jpg" alt="保存勾选" width="60%">
5. 回到应用页面，可以看到应用已成功添加，点击它进入脚本管理界面：
    <img src="/assets/8d2d26e0f4695f0e6a4afa9152f0c609-20250601181509-byffa8t.jpg" alt="点击应用" width="60%">

---

## 第四步：创建脚本

1. 进入「多脚本管理」页面，点击右下角「+」创建新脚本：
    <img src="/assets/722a7423e3a23b884ebd93cadce6e2cd-20250601181532-493wqrb.jpg" alt="创建脚本" width="60%">
2. 给脚本起一个名字：
    <img src="/assets/8989e0d3254ccd3cf0365244db64d49b-20250601181543-tl9icag.jpg" alt="命名脚本" width="60%">
3. 命名完成后，点击确认，进入脚本编辑页面。

---

## 第五步：编写脚本代码

在编辑器中输入以下示例代码，用于在目标应用启动时弹出一个 Toast：

```lua
imports "android.widget.Toast"

hook {
    class = "android.app.Activity",
    method = "onCreate",
    params = {"android.os.Bundle"},
    before = function(it) 

    end,
    after = function(it)
        Toast.makeText(it.thisObject, "Hello LuaHook!", Toast.LENGTH_SHORT).show()
    end
}
```

---

## 第六步：运行并测试

1. 点击顶部的「三角形」按钮，启动目标应用：
    <img src="/assets/409b420733cfd9ea7711a5444797144e-20250601181742-05b60bc.jpg" alt="启动应用" width="60%">
2. 如果你看到目标应用下方弹出 Toast 信息 “LuaHook”，<button id="firework">🎉 恭喜你，脚本运行成功！</button>
    <img src="/assets/317864add1197a02411f14626e01bf15-20250601181834-y4pqx0i.jpg" alt="成功提示" width="60%">

---

## ✅ 小结

你已完成 LuaHook 的首次 Hook 操作，并成功运行了一个简单的脚本。后续你可以尝试：

- Hook 其他方法、类
- 使用更多 Android API
- 上传并分享你的脚本到 [LuaHook 在线脚本仓库](https://github.com/KuLiPai/LuaHook-Scripts)
