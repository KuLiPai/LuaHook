# AGENTS.md — LuaHook Development & Architecture Guide

> **适用对象**：所有参与 LuaHook 项目代码编写、Bug 修复、特性开发或架构维护的 AI Agent 与人类贡献者。  
> **核心原则**：动手前明确进程上下文、严格遵守单向分层依赖、遵循防御性 I/O 规范、提交前必过 Konsist 架构守卫。

---

## 1. 核心铁律：双进程物理隔离 (Dual-Process Isolation) ⚠️

LuaHook 代码运行在两个**完全隔离、运行环境截然不同**的 Android 进程空间中。在编写或修改任何代码前，必须首先确认目标代码所属的进程：

| 维度 | 主 App 进程 (`com.kulipai.luahook`) | Xposed 宿主进程 (被 Hook 的目标应用，如微信等) |
| :--- | :--- | :--- |
| **代码位置** | `app/`, `core/`, `data/`, `feature/` | `hook/entry/`, `hook/api/`, `hook/activity/` |
| **执行上下文** | 拥有完整的 `MyApplication` 与 `AppContainer` DI 容器 | **无 `MyApplication`！无 `AppContainer`！** 仅运行目标宿主的 Application |
| **可用依赖** | AndroidX、Material 3、SoraEditor、FloatingX、ViewModel、Flow | 仅允许纯 Java/Android 基础 SDK、Xposed API、LuaJ 及带 `Standalone` 兜底的工具 |
| **绝对红线** | 1. 严禁在 UI 线程直接执行 Shell 或 Lua 解析。<br>2. 严禁在 `feature/**` 中直接 import 底层 Shell/Shizuku。 | 1. **严禁引用** `AppContainer`、`ViewModel` 或任何 UI 库 (`SoraEditor`, `FloatingX`, `DialogX`)，否则导致宿主 ClassLoader 找不到类或 `UninitializedPropertyAccessException` 闪退。<br>2. **严禁修改 Hook 入口类名与路径**：<br>　- 传统 Xposed (API 53)：`assets/xposed_init` 硬编码为 `com.kulipai.luahook.hook.entry.MainHook`<br>　- 现代 LSPosed (API 102)：`resources/META-INF/xposed/java_init.list` 硬编码为 `com.kulipai.luahook.hook.entry.NewHook` |

---

## 2. 项目包结构拓扑 (Package Topology)

本项目采用单模块 (`:app`) 下的 **Feature-first / Clean** 架构，严格遵循单向依赖流：`feature -> data -> core`（由 Konsist 架构测试强制守护）：

```text
com.kulipai.luahook
├── app/                  # 应用入口与轻量级手动 DI
│   ├── MyApplication.kt  # 主 Application 入口
│   └── di/AppContainer.kt# 全局依赖注入容器 (ShellExecutor, WorkspaceStorage, Repositories)
├── core/                 # 纯基础设施层（严禁反向 import data.* 或 feature.*）
│   ├── shell/            # ShellExecutor (Root / Shizuku 抽象与 StateFlow<ShellMode>)
│   ├── file/             # WorkspaceStorage (工作区原子文件读写、转义与 WorkspaceEventManager)
│   ├── shizuku/          # Shizuku API 绑定与 Binder/Permission 状态流
│   ├── base/             # BaseActivity 等通用基础架构
│   ├── theme/            # Material 3 动态取色与色彩适配
│   └── utils/            # 编码、安全反射、设备环境工具
├── data/                 # 领域模型与数据仓储层
│   ├── model/            # AppInfo, Project, LogEntry 等不可变数据模型
│   └── repository/       # InstalledAppsRepository, ProjectRepository, ScriptRepository, LogcatRepository
├── feature/              # 业务特性层 (Activity/Fragment + ViewModel + UiState/Effect)
│   ├── home/             # 首页底栏、应用列表 (AppsFragment)、项目列表 (ProjectFragment)
│   ├── project/          # 项目工程管理、新建、配置 (ProjectConfigActivity)、项目脚本编辑
│   ├── script/           # 脚本管理与代码编辑
│   │   ├── manager/      # 应用多脚本管理 (MultiScriptActivity, MultScriptAdapter)
│   │   ├── editor/       # 脚本编辑器通用 ViewModel 与委托 (EditActivity, AppsEdit, SoraEditorDelegate)
│   │   ├── selector/     # 生效范围与应用选择器 (SelectApps, ScopeSelectorActivity)
│   │   └── setting/      # 脚本独立运行配置 (ScriptSetActivity)
│   ├── logcat/           # 日志查看器 (LogCatActivity) 与悬浮日志控制器 (FloatingLogcatController)
│   ├── mcp/              # 内置 MCP HTTP/SSE Server 与前台服务 (支持 Cursor/外部 AI 远程接入)
│   ├── manual/           # 内置 Lua API 使用手册与文档
│   ├── setting/          # 全局应用设置 (SettingsActivity)
│   └── about/            # 关于与更新检查
└── hook/                 # Xposed 宿主注入层（独立进程上下文，严禁依赖 feature/UI）
    ├── entry/            # Xposed 入口 (MainHook, NewHook)
    ├── api/              # 暴露给 Lua 运行时的原生桥接 API 与 LuaProject
    └── activity/         # 宿主中弹出的轻量免依赖 Activity (EmptyActivity)
```

---

## 3. 本地存储拓扑与数据协议 (Storage & Protocol)

LuaHook 工作区集中存储于 `/data/local/tmp/LuaHook/`，受 DAC (666) 与 SELinux 保护，所有读写必须通过 `WorkspaceStorage` / `ScriptRepository`：

```text
/data/local/tmp/LuaHook/
├── apps.txt              # 启用了 Hook 的目标应用包名列表（每行一个包名）
├── global.lua            # 全局生效的 Lua 脚本（所有勾选应用启动时均优先执行）
├── AppScript/            # 各应用专属脚本目录
│   └── <packageName>/
│       └── <scriptName>.lua
├── AppConf/              # 各应用多脚本元数据配置
│   └── <packageName>.txt # 格式：{"<scriptName>": [enabled: Boolean, description: String, version: String]}
├── Project/              # 独立工程项目目录
│   ├── info.json         # 项目启用状态字典：{"<projectName>": true/false}
│   └── <projectName>/    # 具体项目文件夹
│       ├── init.lua      # 项目入口文件（定义 icon, author, description, scope, launcher 等全局表）
│       └── ...           # 项目子模块与资产
└── Plugin/               # 运行时扩展插件库
```

---

## 4. 常见开发任务开发指南 (How-To Recipes)

### 场景 A：新增或修改 UI 业务功能
1. **定义数据仓储**：在 `data/repository/` 的接口及实现类中收拢文件/Shell 操作，重度 IO 与解析**必须**切换到 `Dispatchers.IO`。
2. **注册容器依赖**：若新增了 Repository，在 `app/di/AppContainer.kt` 中声明 `by lazy` 实例并提供安全访问。
3. **响应式状态驱动**：在对应 `feature/**` 的 `ViewModel` 中暴露：
   - `StateFlow<UiState>`：用于渲染界面持久状态。
   - `SharedFlow<Effect>`：用于处理一次性事件（Toast 提示、弹窗、页面跳转）。
4. **生命周期安全收集**：Activity / Fragment 收集流时必须包裹在 `lifecycleScope.launch { repeatOnLifecycle(Lifecycle.State.STARTED) { ... } }` 中。

### 场景 B：在 Hook 端新增 Lua 桥接 API 或修改注入逻辑
1. 目标代码位于 `hook/api/` 或 `hook/entry/`。
2. **反射防御**：编写 Java/Kotlin 与 LuaJ (`org.luaj.vm2.*`) 的绑定方法时，**必须捕获所有反射与运行时异常**并输出至日志，严禁向宿主抛出未捕获异常导致宿主 App 崩溃。
3. **ProGuard 规则同步**：若新增了供 Lua 反射调用的类、方法或字段，必须在 `app/proguard-rules.pro` 中显式添加 `-keep` 规则，防止 Release 构建被 R8 剥离或混淆重命名。

### 场景 C：扩展工作区文件操作或执行 Shell 命令
1. 所有文件操作统一调用 `WorkspaceStorage`，所有 Shell 执行统一调用 `ShellExecutor`。
2. **强制转义防注入**：拼接任何文件路径或参数到 Shell 命令前，**必须调用 `.escapeShellPath()` 进行单引号安全转义**（例如 `cat ${path.escapeShellPath()}`），严禁直接裸拼字符串！
3. **SELinux 兼容读取**：读取 `/data/local/tmp` 下文件时，优先尝试直接 File IO，若被拒绝则自动降级到 Root `cat`，确保在各类系统与宿主环境下均可读出内容。

---

## 5. 代码质量与防坑守则 (Guardrails & Pitfalls)

### 1. 防御性文件写入 (Defensive File I/O) ⚠️
- **防空文件覆写（血泪教训）**：编辑器在页面生命周期切换（`onPause` / `onStop`）或自动保存时，**必须执行防空覆盖校验**：
  ```kotlin
  // 严禁在待写入内容为空而磁盘原文件非空时静默覆盖！
  if (currentText.isEmpty() && state.lastSavedContent.isNotEmpty()) return
  ```
- **异步保存不可取消**：页面退出触发的异步保存协程必须包裹在 `withContext(NonCancellable)` 中，防止 Activity 销毁导致协程被直接 Cancel 产生 0 字节截断。

### 2. 路由与 Intent Extra 向后兼容
修改核心组件的 Intent 路由时，必须严格保留既有的 Extra Key，严禁擅自改名：
- `AppsEdit`：必须兼容 `"packageName"`, `"scripName"`（注意历史拼写少个 t）, `"scriptName"`, `"appName"`, `"scriptDescription"`。
- `MultiScriptActivity`：必须兼容 `"packageName"`, `"appName"`。
- `ScriptSetActivity`：必须兼容 `"path"`。
- `ProjectConfigActivity`：必须兼容 `"projectName"`。

### 3. 多 Agent 并行协作纪律 (Subagent Concurrency Rules)
当使用多 Agent 并发开发不同模块时，必须严格遵守以下并发纪律：
1. **严禁子 Agent 各自并发运行 `./gradlew`**：子 Agent 并发触发构建会引发 Gradle 守护进程的文件锁冲突（Lock Contention）。必须采用“**各子 Agent 并行修改代码，由主 Agent 串行统一执行构建与测试**”的策略。
2. **公共单点文件隔离**：子 Agent 严禁直接修改公共中枢文件（如 `AppContainer.kt`、`AndroidManifest.xml`、`build.gradle.kts`），应在汇报中说明所需的依赖，由主协调 Agent 统一装配。

### 4. 内置 MCP 服务协同
LuaHook 内置了基于 NanoHTTPD 的 MCP HTTP Server（运行于 `feature/mcp/`），支持外部 IDE/AI 远程读写工作区脚本。修改 `WorkspaceStorage` 时已内置 `WorkspaceEventManager.fileEvents` 变更通知，编辑器与 MCP 互相修改文件时需通过时间戳与内容比对防止保存回环冲突。

### 5. C++ Native (JNI / xdl) 约束
项目包含 Native C++ 组件（位于 `app/src/main/cpp/xdl`），构建 ABI 限定为 `armeabi-v7a` 与 `arm64-v8a`。修改 C++ 或 JNI 代码时，需确保 32 位与 64 位平台符号均能正常链接。

---

## 6. 提交前的强制验收流水线 (Verification Pipeline)

任何代码修改在发起 Commit 或提交 PR 前，必须在终端依次执行以下验证并确保 **100% 绿灯**：

```bash
# 1. 运行单元测试与 Konsist 架构防腐蚀断言（耗时 < 3 秒，核心门禁！）
# 自动检验：Hook 层零 UI 依赖、Core 层零反向依赖、UI 层零直接 Shell 调用
./gradlew testDebugUnitTest

# 2. 验证 Debug 完整打包
./gradlew assembleDebug

# 3. 验证 Release 打包与 R8/ProGuard 混淆压缩（排查 Hook 入口与反射规则是否断裂）
./gradlew assembleRelease
```
