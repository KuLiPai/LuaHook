# 字段操作（Field 操作）

在 Hook 过程中，除了方法的调用，我们也经常需要对类的字段进行**读取**或**修改**。本模块提供以下四个函数，用于访问对象或类的字段信息：

---

### 1. `getField` —— 获取实例字段

```lua
getField(实例, "字段名")
```

#### 功能说明：

- 获取对象实例中的成员字段（包括 `public`、`private`、`protected` 等所有修饰符）

#### 示例：

```lua
local activityTitle = getField(activityInstance, "mTitleTextView")
```

---

### 2. `getStaticField` —— 获取静态字段

```lua
getStaticField(类, "字段名")
```

#### 功能说明：

- 适用于访问类级别的 `static` 字段

#### 示例：

```lua
local sdkInt = getStaticField(Build, "SDK_INT")
```

---

### 3. `setField` —— 修改实例字段

```lua
setField(实例, "字段名", 新值)
```

#### 功能说明：

- 动态设置对象的字段值
- 支持私有字段修改

#### 示例：

```lua
setField(view, "mPaddingTop", 20)
```

---

### 4. `setStaticField` —— 修改静态字段

```lua
setStaticField(类, "字段名", 新值)
```

#### 功能说明：

- 用于修改静态字段的值

#### 示例：

```lua
setStaticField(Builds, "SDK_INT", 99)
```
