# lpparam（`LoadPackageParam`）介绍

`lpparam` 是 Xposed 模块中用于代表当前 Hook 宿主应用上下文的参数对象，完整类型为 `XC_LoadPackage.LoadPackageParam`。

可以理解为：**当前被 Hook 的 App 运行时信息容器**。

它通常作为 Hook 回调函数中的参数，用于获取宿主包的信息与加载相关类。

---

### 常见字段说明

| 字段名 | 类型 | 含义说明                                                                                                                          |
| -------- | ------ | ----------------------------------------------------------------------------------------------------------------------------------- |
| `appInfo`       | `ApplicationInfo`     | 宿主 App 的安装信息对象（可获取路径、签名等）                                                                                     |
| `classLoader`       | `ClassLoader`     | 当前宿主的默认类加载器（**Hook 类核心入口**）                                                                                                        |
| `isFirstApplication`       | `boolean`     | 是否首个加载的app，在加载真实的app前，部分手机(小米)会额外加载应用包管理程序和什么程序来着，要用这个属性才能确保是加载了目标的app |
| `packageName`       | `String`     | 宿主应用的包名                                                                                                                    |
| `processName`       | `String`     | 当前进程名（可用于区分主/子进程）                                                                                                 |

---

### 常用用法总结

```lua
-- 获取类加载器，用于 findClass 或 hook 类
local loader = lpparam.classLoader

-- 获取宿主包名，用于判断目标 App
local pkg = lpparam.packageName

-- 获取安装包路径，适用于读取宿主资源或路径验证
local apkPath = lpparam.appInfo.sourceDir
```

