import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// 应用Kotlin DSL插件，使构建脚本能够使用Kotlin语言编写
plugins {
    `kotlin-dsl`
}

// 定义构建逻辑模块的组名
group = "com.kulipai.luahook.buildlogic"

// 配置构建逻辑插件以目标 JDK 17
// 这与用于构建项目的 JDK 匹配，与设备上运行的内容无关
java {
    // 设置Java源代码和目标字节码的兼容性版本为Java 17
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

// 声明构建逻辑模块的依赖
dependencies {
    // 添加Android Gradle插件依赖（仅编译时需要）
    compileOnly(libs.android.gradlePlugin)
    // 添加Kotlin Gradle插件依赖（仅编译时需要）
    compileOnly(libs.kotlin.gradlePlugin)
    // 添加KSP注解处理器插件依赖（仅编译时需要）
    compileOnly(libs.ksp.gradlePlugin)
}

// 配置Gradle插件
gradlePlugin {
    plugins {
        // 注册Android应用程序插件
        register("luahookAndroidApplication") {
            id = "com.kulipai.luahook.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        // 注册Android应用程序Compose插件
        register("luahookAndroidApplicationCompose") {
            id = "com.kulipai.luahook.android.application.compose"
            implementationClass = "AndroidApplicationComposeConventionPlugin"
        }
        // 注册Android库插件
        register("luahookAndroidLibrary") {
            id = "com.kulipai.luahook.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        // 注册Android库Compose插件
        register("luahookAndroidLibraryCompose") {
            id = "com.kulipai.luahook.android.library.compose"
            implementationClass = "AndroidLibraryComposeConventionPlugin"
        }
        // 注册Android Feature模块插件
        register("luahookAndroidFeature") {
            id = "com.kulipai.luahook.android.feature"
            implementationClass = "AndroidFeatureConventionPlugin"
        }
        // 注册Koin依赖注入插件
        register("luahookKoin") {
            id = "com.kulipai.luahook.koin"
            implementationClass = "KoinConventionPlugin"
        }


    }
}

// 配置任务以处理重复资源
tasks.withType<ProcessResources> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}