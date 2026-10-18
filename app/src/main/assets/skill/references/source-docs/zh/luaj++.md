# Luaj++

## 简介

nirenr的luaj改版，增加对java更好的支持，以下内容来自NeLuaj提供的Luaj++参考手册，希望对您了解luaj++有帮助

## 入口文件

`Activity` main.lua

`Service` service.lua

`AccessibilityService` accessibility.lua

`NotificationListenerService` notification.lua

`WallpaperService` wallpaper.lua

服务可以使用`setLuaDir(dir)`设置运行目录，`setEnabled(context)`打开启动服务设置界面，`getInstance()`获取服务实例。

## 可省略非必要关键字

- 省略then

```lua
if a then

end

-->

if a

end
```

- 省略do

```lua
while a do

end

-->

while a

end
```

- 省略in

```lua
for k,v in pairs(t) do

end

-->

for k,v pairs(t)

end
```

- 省略function

```lua
local function a()
    
end

-->

local a()

end
```

- 支持switch

```lua
switch a
  case 1,3,5,7,9
    print(1)
  case 2,4,6,8
    print(2)
  case 0
    print(0)
  default
    print(nil)
end
```

- 支持when

```lua
a = when a
   case 1,3,5,7,9
     return 1
   case 2,4,6,8
     return 2
   case 0
     return 0
   default
     return nil
 end
```

- 支持continue

```lua
for n = 1,10
  if n%2 == 0
    continue
  end
  print(n)
end
```

## 支持foreach

```lua
for k,v : t
end

for k,v in t
end

```

## 支持defer

defer后语句将在函数结束时运行
多个defer将按照后入先出原则运行。

## 支持?操作符

```lua
?a print(1)`print(2)
a = ?a print(1)`print(2)
```

## 支持三目 if

```lua
b = if a 1 else 2
print(b)
```

## 支持try-catch-finally

```lua
try
  error("err")
catch(e)
  print("catch", e)
finally
  print("finally")
end
```

** 支持lambda，可以使用反斜杠代替lambda关键字 **

```lua
lambda a,b->a+b

lambda a,b=>print(a+b)

lambda a,b:print(a+b)

lambda () -> print("lambda")
```

## 支持import

```lua
import "package"
--将导入包并设置为局部变量

import "java.lang.String"
--返回值为 javaClass

import "java.lang.*"
--返回值为 javaPackage

import str "java.lang.String"
--设置别名

import "java.lang. *", "java.io.* "
--一次性导入多个包或类
```

**支持module**

module自带环境，默认设置环境表的metatable为自己

module "name"

**支持自赋值local**

local:print

将全局print设置为局部print

**运算符优化**

```lua
!= 可代替 ~=
！ 可代替 not
&& 可代替 and
|| 可代替 or
```

## 支持位运算

- 按位与
  a=1&2
- 按位或
  a=1|2
- 按位异或
  a=1~2
- 右移
  a=1>>8
- 左移
  a=8<<2
- 按位非
  a=~2

## 支持64位整数

```lua
i=0xffffffffff
```

**支持`+= -= *= /= %= ^= //= &= |= ~= <<= >>= ..=`运算**

```lua
a+=1
a-=1
a*=1
a/=1
```

## 调用java优化

- javaClass 拓展函数/属性

```lua
Object.array{} -- 创建数组
print(Object.new) -- 类的构造器
print(Object.class) -- 获取类本身
Object.override{} -- 覆盖方法
```

- 直接()构建实例或实现接口,抽象类

```lua
b = ArrayList()
m = HashMap()
i = interface { 
    methodName=function(arg)
    end
}
c = abstract {
    methodName=function(super, arg)
    end
}
```

- 支持覆盖方法

```lua
list = ArrayList.override {
  function add(superCall, arg)
    superCall(arg)
  end
}()
list = ArrayList {
  add = function(s, a)
  end
}
```

- 支持元方法

```lua
function Button:print()
  print(self)
end
Button(this):print()
```

- 支持批量设置属性

```lua
Button(this) {
  text="test",
  enabled=false
}
```

- 直接创建数组

```lua
i=int[10]
i=int{1,2,3}
i=Integer[10]
```

- java 方法使用.调用

```lua
b.add(!)
```

- is 方法简写

```lua
view.isActivated()
-->
view.Activated
```

- java getter/setter优化

```lua
b.setText("")
-->
b.text=""
m.abc=1

t=b.getText()
-->
t=b.text
t=m.abc
```

- 语法糖示例

```lua
  mBtn.setOnClickListener(View.OnClickListener {
    onClick = function(v)
      print(v)
    end
  })
  --> 忽略接口类型
  mBtn.setOnClickListener({
    onClick = function(v)
      print(v)
    end
  })
  --> 简写函数式接口
  mBtn.setOnClickListener(function(v)
    print(v)
  end)
  --> 简写此类方法
  mBtn.onClick = function(v)
    print(v)
  end
```

- 数组操作

```lua
使用#获取Java常见数据类型的长度
使用 ["索引"] 或 .索引 直接访问数组
```

- 索引优化

```lua
t={}
t."end"=123
t.end=123
```

- 非必要的接口类型可省略

```lua
mViewPager.addOnPageChangeListener(
  ViewPager.OnPageChangeListener{
    onPageSelected = function(_)
    end
  }
)
-->
mViewPager.addOnPageChangeListener{
  onPageSelected = function(_)
  end
}
```

- 函数式接口可简写

```lua
obj.run(Runnable {
  run = function()
    -- do something
  end
})
-->
obj.run(function()
  -- do something
end)
```

**支持增强型字符串格式化**

`a/A` 有符号十六进制浮点数，

`b` 布尔值，

`B` 无符号byte类型，

`c char`类型，数字转文字，

`i/d` 有符号整数类型，字符串转十进制，

`I` 无符号`int`整数，

`e/E/f/g/G` 有符号浮点数，

`o` 八进制有符号整数，

`L` 无符号长整数，

`u/U` 字符串转u码，

`x/X` 十六进制有符号整数，字符串转hex，

`r` 解析字符串转义，

`q` 格式化为合法字符串形式，

`s` 转字符串，

`l` url编码，
