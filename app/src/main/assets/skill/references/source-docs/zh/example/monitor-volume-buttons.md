# 监听音量按键
Hook了Android系统中所有Activity的`onKeyDown`方法，主要用于监听并处理音量键的按下事件：
- 监听目标：所有Activity的按键事件
- 特别关注：音量+键（KeyCode 24）和音量-键（KeyCode 25）
- 执行时机：在系统处理按键事件后触发（后置Hook）
- 典型应用：修改音量键功能、添加快捷操作等

## 代码概览

```lua
hook{
  class="android.app.Activity",
  method="onKeyDown",
  params={"int","android.view.KeyEvent"},
  before=function(it)
    local context = it.thisObject
    local keyCode = it.args[0]
    
    if keyCode == 24 then
      -- 音量+键处理
    end

    if keyCode == 25 then
      -- 音量-键处理
    end
  end
}
```

通过上下文`context`可以进行UI相关的操作
| 操作               | 代码示例                                                                 | 说明                   |
|--------------------|--------------------------------------------------------------------------|------------------------|
| 显示Toast         | `Toast.makeText(context, "text", Toast.LENGTH_SHORT).show()`             | 弹出提示               |
| 弹对话框          | `AlertDialog.Builder(context).setTitle(...).show()`                   | 显示系统对话框         |
| 获取屏幕尺寸      | `context.getResources().getDisplayMetrics().widthPixels`                 | 屏幕宽度/高度          |
| 获取状态栏高度    | `context.getResources().getIdentifier("status_bar_height", "dimen", "android")` | 读取系统状态栏高度     |

## 关键参数

- `it.args[0]`对应方法的第一个参数（KeyCode）
- `it.thisObject`获取当前Activity上下文

## 使用场景

- 修改默认音量键行为
- 实现自定义快捷键功能
- 禁用特定按键功能
- 记录用户按键操作

## Android 按键 KeyCode 对照表

| 按键名称                  | KeyCode | 常量名                          | 备注                     |
|---------------------------|---------|---------------------------------|--------------------------|
| **音量控制**              |         |                                 |                          |
| 音量+                     | 24      | `KEYCODE_VOLUME_UP`             |                          |
| 音量-                     | 25      | `KEYCODE_VOLUME_DOWN`           |                          |
| 静音                      | 164     | `KEYCODE_VOLUME_MUTE`           |                          |
| **导航键**                |         |                                 |                          |
| 返回键                    | 4       | `KEYCODE_BACK`                  |                          |
| 主页键                    | 3       | `KEYCODE_HOME`                  | 需要系统级权限           |
| 最近任务                  | 187     | `KEYCODE_APP_SWITCH`            |                          |
| 菜单键                    | 82      | `KEYCODE_MENU`                  |                          |
| **媒体控制**              |         |                                 |                          |
| 播放/暂停                 | 85      | `KEYCODE_MEDIA_PLAY_PAUSE`      |                          |
| 下一曲                    | 87      | `KEYCODE_MEDIA_NEXT`            |                          |
| 上一曲                    | 88      | `KEYCODE_MEDIA_PREVIOUS`        |                          |
| 停止播放                  | 86      | `KEYCODE_MEDIA_STOP`            |                          |
| **数字键**                |         |                                 |                          |
| 0-9                       | 7-16    | `KEYCODE_0`-`KEYCODE_9`         |                          |
| **字母键**                |         |                                 |                          |
| A-Z                       | 29-54   | `KEYCODE_A`-`KEYCODE_Z`         |                          |
| **功能键**                |         |                                 |                          |
| 电源键                    | 26      | `KEYCODE_POWER`                 | 需要系统级权限           |
| 相机键                    | 27      | `KEYCODE_CAMERA`                |                          |
| 搜索键                    | 84      | `KEYCODE_SEARCH`                |                          |
| **方向键**                |         |                                 |                          |
| 上                        | 19      | `KEYCODE_DPAD_UP`               |                          |
| 下                        | 20      | `KEYCODE_DPAD_DOWN`             |                          |
| 左                        | 21      | `KEYCODE_DPAD_LEFT`             |                          |
| 右                        | 22      | `KEYCODE_DPAD_RIGHT`            |                          |
| 确定                      | 23      | `KEYCODE_DPAD_CENTER`           |                          |
| **游戏控制**              |         |                                 |                          |
| L1/R1 (肩键)              | 102/103 | `KEYCODE_BUTTON_L1`/`R1`        |                          |
| A/B/X/Y                   | 96-99   | `KEYCODE_BUTTON_A`-`Y`          |                          |
| **特殊按键**              |         |                                 |                          |
| 截图                      | 120     | `KEYCODE_SYSRQ`                 | Android 4.0+             |
| 语音助手                  | 231     | `KEYCODE_VOICE_ASSIST`          |                          |
| 外接键盘Enter             | 66      | `KEYCODE_ENTER`                 |                          |

## 触摸事件对照表

| 事件类型       | 说明                          |
|----------------|-------------------------------|
| `ACTION_DOWN`  | 手指按下 (代码值: 0)          |
| `ACTION_UP`    | 手指抬起 (代码值: 1)          |
| `ACTION_MOVE`  | 手指移动 (代码值: 2)          |
| `ACTION_CANCEL`| 事件取消 (代码值: 3)          |

> 注：完整常量定义见 [KeyEvent](https://developer.android.com/reference/android/view/KeyEvent) 官方文档