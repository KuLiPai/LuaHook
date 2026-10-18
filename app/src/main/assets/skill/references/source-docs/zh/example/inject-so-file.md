# SO文件动态注入


## 功能概述
Hook了Android应用启动时的`Application.attach()`方法，用于动态加载指定SO文件：

- **注入目标**：应用启动时的Context初始化阶段
- **核心功能**：动态加载指定路径的SO文件
- **执行时机**：Context绑定完成后（后置Hook）
- **典型应用**：热修复、模块注入、Native代码动态加载

## 核心特性
- ✅ **无侵入式注入**：通过Hook技术实现
- ✅ **动态加载**：运行时加载SO文件
- ✅ **错误处理**：完善的异常捕获机制
- ✅ **多场景适配**：支持自定义路径和权限管理

## 代码概览

```lua
imports "java.lang.System"
-- 把so文件复制到
-- /data/data/包名/files/名称.so
-- 授权777
local soName = "libcloud.so"
hook {
    class = "android.app.Application",
    classLoader = lpparam.classLoader,
    method = "attach",
    params = {"android.content.Context"},
    after = function(after)
        local context = after.args[0]
        local packageName = invoke(context, "getPackageName")
        log("当前包名: "..packageName)
        local path = "/data/data/"..packageName.."/files/"..soName
        log("尝试加载: "..path)
        local loader = invoke(after.thisObject, "getClassLoader")
        local ok, err = pcall(function()
            System.load(path)
        end)
        if ok then
            log(soName.." 加载成功")
        else
            log("加载失败: "..tostring(err))
        end
    end
}
```

## System.load()

`System.load` 是 Java 中用于加载本地库（Native Library）的一个静态方法，它允许 Java 程序调用本地代码（通常是用 C/C++ 编写的代码）。

**方法定义**
```java
@SystemApi
public static void load(String pathName) {
    Runtime.getRuntime().load(Reflection.getCallerClass(), pathName);
}

```

**工作原理**

`System.load` 方法加载指定的本地库到 `Java` 虚拟机的地址空间中

如果库中定义了 `JNI_OnLoad` 函数，该方法会被调用

加载成功后，Java 代码可以通过 JNI (Java Native Interface) 调用本地方法

