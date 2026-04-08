plugins {
    alias(libs.plugins.luahook.android.feature)
}
android {
    namespace = "com.kulipai.luahook.feature.main"


}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-XXLanguage:+ContextParameters")
    }
}
dependencies {
}