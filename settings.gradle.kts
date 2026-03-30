// Gradle插件管理配置
pluginManagement {
    // 包含build-logic目录作为构建逻辑模块
    includeBuild("build-logic")

    // 配置插件仓库
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "LuaHook"

// 包含主应用模块
include(":app")

// 核心模块
include(":core:theme")
include(":core:navigation")
include(":core:data")
include(":core:model")


// feature 功能模块
include(":feature:main")

include(":feature:about")
