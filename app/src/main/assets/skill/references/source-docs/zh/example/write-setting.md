# 编写脚本设置页面

## 1. `::set::` 注解的作用

`::set::` 是一个特殊的注解，用于标识脚本文件定义了一个可作为独立界面的 **设置页面**。在 LuaHook 运行时环境中，该注解帮助脚本引擎识别并正确加载和处理这些设置页面脚本。

-----

## 2. 设置页面代码概览

以下是一个典型的 Lua 设置页面脚本结构，它展示了一个包含文本视图和按钮的基础界面：

```lua
::set::
function setActivity()
	-- 导入必要的 Java 类，用于 UI 组件和功能
	import "com.kulipai.luahook.util.*"
	import "java.lang.*"
	import "android.widget.Toast"
	import "android.widget.*"

	-- 定义一个全局的 print 函数，方便调试输出
	function print(...)
		local text = table.concat({ ... }, " ")
		Toast.makeText(this, tostring(text), Toast.LENGTH_SHORT).show()
	end

	-- 使用 Lua 表定义界面布局
	local layout = {
		LinearLayout, -- 根布局为线性布局
		gravity = "center", -- 内容居中
		id = "filelayout", -- 布局ID
		orientation = 1, -- 垂直排列 (1=垂直，0=水平)
		layout_width = "fill", -- 宽度填满父视图
		layout_height = "fill", -- 高度填满父视图
		{
			TextView, -- 文本视图
			id = "tv", -- 控件ID
			textIsSelectable = true, -- 文本可长按复制
			gravity = "center", -- 文本居中
			layout_width = "fill", -- 宽度填满父视图
			text = "测试文本", -- 默认显示文本
		},
		{
			Button, -- 按钮视图
			id = "btn", -- 控件ID
			layout_marginTop = "16dp", -- 上边距 16dp
			layout_marginBottom = "16dp", -- 下边距 16dp
			gravity = "center", -- 内容居中
			layout_width = "fill", -- 宽度填满父视图
			text = "测试按钮", -- 按钮文本
		},
	}

	-- 加载并设置布局到当前 Activity
	activity.setContentView(loadlayout(layout))

	-- 绑定按钮点击事件
	btn.onClick = function()
		tv.setText("我爱LuaHook") -- 修改文本内容
	end
end
```

-----

## 3. 关键点说明

  * **`loadlayout` 函数**: 此函数负责将 Lua 表格定义的布局结构转换为 Android 实际的视图对象，并将其渲染到屏幕上。
  * **事件绑定**: 通过 `控件ID.onClick` 这种简洁的方式可以直接为 UI 控件绑定事件监听器，例如 `btn.onClick = function() ... end`。
  * **布局属性**:
      * `layout_width`/`layout_height`: 可以设置为 `"fill"` (填满父视图) 或 `"wrap"` (包裹内容)。
      * `gravity`: 控制视图内容的对齐方式。
      * `orientation`: 仅适用于线性布局 (`LinearLayout`)，决定子视图的排列方向（垂直或水平）。
  * **单位**: 在 Android 布局中，推荐使用 `dp` (density-independent pixels) 作为尺寸单位，以确保在不同密度的屏幕上显示效果一致。

-----

## 4. 页面跳转与参数传递

LuaHook 提供了两种主要方式来实现设置页面的跳转：**通过封装 `Intent` 启动 `ScriptSetActivity`** 或 **直接使用 `injectActivity` 方法**。无论是哪种方式，最终都是通过 `ScriptSetActivity` 作为统一的入口来加载和显示 Lua 设置页面。参数传递则通过标准的 `Intent.putExtra` 机制完成。

### 4.1 通过封装 `Intent` 实现跳转

为了简化页面跳转逻辑，建议封装一个辅助函数，集中处理 `Intent` 的构建和启动。这种方式提供了更明确的控制，尤其是当你需要传递复杂参数或进行其他 `Intent` 配置时。

```lua
local function startScriptActivity(context, scriptName, arg)
    import "android.content.ComponentName"
    import "android.content.Intent"

    local intent = Intent()
    -- 明确指定目标 Activity 组件为 LuaHook 的 ScriptSetActivity
    intent.setComponent(ComponentName("com.kulipai.luahook",
        "com.kulipai.luahook.activity.ScriptSetActivity"))

    -- 通过 Intent.putExtra 传递参数。
    -- 'arg' 是一个示例键名，你可以根据需要定义不同的键。
    intent.putExtra("arg", arg)

    -- 构建脚本文件的完整路径，这是 ScriptSetActivity 加载 Lua 脚本的关键
    local packageName = context.getPackageName()
    intent.putExtra("path", "/data/local/tmp/LuaHook/AppScript/" .. packageName .. "/" .. scriptName .. ".lua")

    -- 启动目标 Activity
    context.startActivity(intent)
end
```

### 4.2 通过 `injectActivity` 方法实现跳转

`injectActivity` 方法是 LuaHook 提供的一种更直接的跳转方式，它通常用于在运行时注入并启动新的 Activity。其底层实现原理与封装 `Intent` 启动 `ScriptSetActivity` 类似，都是将 Lua 代码或文件路径作为参数传递给 `ScriptSetActivity`。

`injectActivity` 的基本用法如下：

```lua
-- injectActivity(当前的activity实例, lua代码字符串/lua文件路径)
-- 目前主要支持 lua代码字符串
injectActivity(currentActivity, "print('Hello from injected Activity')")
```

**重要提示：**

  * 目前 `injectActivity` 主要支持直接传入 **Lua 代码字符串**。这意味着你需要将整个设置页面的 Lua 逻辑作为字符串传递。
  * 如果需要传递参数，无论使用 `startScriptActivity` 还是 `injectActivity`，**都仍然需要通过 `Intent.putExtra` 这种原始方法来传递。** 这是因为 `ScriptSetActivity` 统一通过 `Intent` 来接收所有必要的启动信息和参数。

### 4.3 宿主页面调用示例

以下示例展示了如何在宿主应用的 Lua 脚本中调用 **封装的 `startScriptActivity` 函数** 来跳转到设置页面，并传递一个字符串参数。如果你选择直接使用 `injectActivity`，逻辑会类似，但需要将整个 `::set::` 定义的 Lua 代码作为字符串传入。

```lua
hookcotr(
	"com.tencent.mm.pluginsdk.ui.chat.ChatFooter", -- 目标类
	loader,
	"android.content.Context",
	"android.util.AttributeSet",
	"int",
	function(it) end,
	function(it)
		local button = getField(it.thisObject, "w") -- 获取宿主应用中的一个按钮实例
		local context = invoke(button, "getContext") -- 获取当前上下文

		button.onLongClick = function() -- 按钮长按事件
			-- 调用封装的函数，跳转到名为 "当前脚本名称" 的设置页面，并传递参数 "你好"
			startScriptActivity(context, "当前脚本名称", "你好")

			-- 如果使用 injectActivity，大致会是这样（需要将 ::set:: 函数转换为字符串）：
			-- local luaCode = [[
			-- ::set::
			-- function setActivity()
			--    -- ... 你的设置页面代码 ...
			-- end
			-- ]]
			-- injectActivity(context, luaCode)
		end
	end
)
```

### 4.4 设置页面接收参数示例

在 `::set::` 定义的设置页面脚本中，无论你是通过 `startScriptActivity` 还是 `injectActivity` 跳转过来，都可以通过 `this.getIntent().getExtras()` 方法来获取传递过来的参数：

```lua
::set::
function setActivity()
	import "com.kulipai.luahook.util.*"
	import "java.lang.*"
	import "android.widget.Toast"
	import "android.widget.*"

	function print(...)
		local text = table.concat({ ... }, " ")
		Toast.makeText(this, tostring(text), Toast.LENGTH_SHORT).show()
	end

	-- 获取从 Intent 中传递过来的字符串参数 "arg"
	local argStr = this.getIntent().getExtras().getString("arg")

	local layout = {
		LinearLayout,
		gravity = "center",
		id = "filelayout",
		orientation = 1,
		layout_width = "fill",
		layout_height = "fill",
		{
			TextView,
			id = "tv",
			textIsSelectable = true,
			gravity = "center",
			layout_width = "fill",
			text = "测试文本",
		},
		{
			Button,
			id = "btn",
			layout_marginTop = "16dp",
			layout_marginBottom = "16dp",
			gravity = "center",
			layout_width = "fill",
			text = "测试按钮",
		},
	}

	activity.setContentView(loadlayout(layout))

	btn.onClick = function()
		-- 将接收到的参数设置到文本视图中
		tv.setText(argStr)
	end
end
```

-----

## 5. 未来展望（待实现）

目前，页面跳转和参数传递主要依赖于 `ScriptSetActivity` 作为统一入口，并通过 `Intent.putExtra` 来实现。未来计划将支持更灵活的跳转方式和参数传递机制，包括：

  * **通过 `::set::` 传递 Lua 函数**: 允许直接传递 Lua 函数引用，实现更高级的回调和交互。
  * **通过 Lua 文件路径传递**: 简化大型 Lua 脚本的组织和加载，避免长字符串。

这些改进将进一步提升 LuaHook 在开发自定义设置页面方面的便利性和强大功能。



