# 📂 LuaFile

文件操作库，用于读写、删除、复制、移动文件等。

### file.isFile(path)

`返回值: boolean`
是否为文件。

### file.isDir(path)

`返回值: boolean`
是否为文件夹。

### file.isExists(path)

`返回值: boolean`
文件或文件夹是否存在。

### file.read(path)

`返回值: string | nil`
读取文件文本内容。

### file.readBytes(path)

`返回值: string | nil`
读取文件二进制内容（按字节）。

### file.write(path, content)

`返回值: boolean`
写入文本，覆盖原文件。

### file.writeBytes(path, content)

`返回值: boolean`
写入二进制，覆盖原文件。

### file.append(path, content)

`返回值: boolean`
追加写入文本。

### file.appendBytes(path, content)

`返回值: boolean`
追加写入二进制。

### file.copy(from, to)

`返回值: boolean`
复制文件，若存在则覆盖。

### file.move(from, to)

`返回值: boolean`
移动文件，若存在则覆盖。

### file.rename(path, newName)

`返回值: boolean`
重命名文件。

### file.delete(path)

`返回值: boolean`
删除文件。

### file.getName(path)

`返回值: string`
获取文件名。

### file.getSize(path)

`返回值: number`
获取文件大小（字节数）。
