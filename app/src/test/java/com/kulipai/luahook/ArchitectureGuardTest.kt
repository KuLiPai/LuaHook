package com.kulipai.luahook

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withNameEndingWith
import com.lemonappdev.konsist.api.ext.list.withPackage
import com.lemonappdev.konsist.api.verify.assertFalse
import org.junit.Ignore
import org.junit.Test

class ArchitectureGuardTest {

    private val scope = Konsist.scopeFromProject()

    @Test
    fun `Xposed Hook 入口必须保持纯净，严禁引入 UI 或重型库`() {
        // 防止 Hook 代码不小心 import 了 UI 组件或重型库，被注入目标宿主 App 引发 ClassLoader 崩溃
        scope.files
            .withPackage("com.kulipai.luahook.hook.entry..", "com.kulipai.luahook.hook.api..")
            .assertFalse { file ->
                file.hasImport { import ->
                    import.name.startsWith("com.kulipai.luahook.ui.") ||
                    import.name.startsWith("com.kulipai.luahook.feature.") ||
                    import.name.startsWith("com.kongzue.dialogx.") ||
                    import.name.startsWith("com.petterp.floatingx.") ||
                    import.name.startsWith("io.github.rosemoe.sora.")
                }
            }
    }

    @Test
    fun `Core 层严禁反向依赖 UI 层或 MCP 业务层`() {
        scope.files
            .withPackage("com.kulipai.luahook.core..")
            .assertFalse { file ->
                file.hasImport { import ->
                    import.name.startsWith("com.kulipai.luahook.ui.") ||
                    import.name.startsWith("com.kulipai.luahook.feature.") ||
                    import.name.startsWith("com.kulipai.luahook.mcp.")
                }
            }
    }

    @Test
    @Ignore("待 Phase 2-4 UI 界面与底层解耦完成后开启")
    fun `UI 界面严禁直接调用底层 Shell 或 Shizuku`() {
        scope.files
            .withNameEndingWith("Activity", "Fragment", "Adapter")
            .assertFalse { file ->
                file.hasImport { import ->
                    import.name == "com.topjohnwu.superuser.Shell" ||
                    import.name == "rikka.shizuku.Shizuku" ||
                    import.name == "com.kulipai.luahook.core.shizuku.ShizukuShellFallback"
                }
            }
    }
}
