# 导入和寻找类

1. 查找类（**`findClass`**）
2. 导入类（**`import`** / **`imports`** ）

---

### 1. `findClass` —— 查找类

```lua
local clazz = findClass("类全名", 加载器)
```

#### 参数说明：

- `类全名`：Java 类的全限定名，例如 `"android.app.Activity"`
- `加载器`（可选）：指定使用的类加载器，默认为 `lpparam.classLoader`

---

### 2. `imports` —— 导入类（宿主或模块）

```lua
imports "android.os.Build"

local device = Build.DEVICE
```

#### 功能说明：

- 将 Java 类导入为 **全局变量**，可直接使用类名访问其静态字段或方法
- 搜索顺序：

  1. 优先从 **宿主应用** 中加载类
  2. 若未找到，再从 **模块本身** 中加载类

---

### 3. `import` —— 导入模块类（支持通配符）

```lua
import "java.lang.String"
import "java.util.*"
```

#### 功能说明：

- 仅用于导入模块 APK 自带的类
- 支持通配符 `*`，一次导入整个包中的所有类
