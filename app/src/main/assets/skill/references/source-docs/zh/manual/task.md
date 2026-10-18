# Task

用于在指定时间后异步执行一段 Lua 函数。

### Task(function [, delay])

**参数:**

- `function` : 需要执行的 Lua 函数
- `delay`  *(可选)*  : 延迟时间（毫秒），默认为 `0`

**返回值:**

- `nil`

**说明:** 
在后台启动一个协程，等待 `delay` 毫秒后调用 `function`。

---

### 示例

```lua
-- 立即执行一次
Task(function()
    print("Hello from Task!")
end)

-- 2秒后执行
Task(function()
    print("This runs after 2 seconds")
end, 2000)

-- 连续调度多个
for i = 1, 3 do
    Task(function()
        print("Task #" .. i)
    end, i * 1000) -- 1秒后、2秒后、3秒后依次执行
end
```
