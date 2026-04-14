plugins {
//    alias(libs.plugins.luahook.android.library)
    alias(libs.plugins.luahook.android.library.compose)
}

android {
    namespace = "com.kulipai.luahook.core.ui"
}

dependencies {
    implementation(project(":core:theme"))
    implementation(project(":core:model"))

}
