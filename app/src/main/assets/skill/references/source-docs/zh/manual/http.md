# 🌐 LuaHttp

HTTP 请求库，用于 GET / POST / 下载 / 上传。

### http.get(url [, headers, cookie, timeout], callback)

`返回值: nil`
发送 GET 请求。

- `url` : 请求地址
- `headers`  *(可选)*  : 表头（LuaTable）
- `cookie`  *(可选)*  : Cookie 字符串
- `timeout`  *(可选)*  : 超时时间（毫秒，默认 10000）
- `callback` : 回调函数，格式 `function(success, body, code)`

---

### http.post(url, body [, headers, cookie, timeout], callback)

`返回值: nil`
发送 POST 请求。

- `body` : 请求体（字符串或 LuaTable）
- 其余参数与 `http.get` 相同。

---

### http.download(url, path [, headers, cookie, timeout], callback)

`返回值: nil`
下载文件并保存到指定路径。

- `path` : 保存路径
- `callback` : `function(success, pathOrError, code)`

---

### http.upload(url, path [, headers, cookie, timeout], callback)

`返回值: nil`
上传文件。

- `path` : 文件路径
- `callback` : `function(success, body, code)`

---

### 示例

```lua
-- GET 请求
http.get("https://httpbin.org/get", function(success, body, code)
    print(success, code)
    print(body)
end)

-- POST 请求
http.post("https://httpbin.org/post", { name = "kulipai" }, function(success, body, code)
    print("POST:", success, code)
end)

-- 下载文件
http.download("https://example.com/file.zip", "/sdcard/file.zip", function(success, path, code)
    if success then
        print("下载完成:", path)
    else
        print("失败:", path, code)
    end
end)

-- 上传文件
http.upload("https://example.com/upload", "/sdcard/file.zip", function(success, body, code)
    print("上传结果:", success, code)
end)
```
