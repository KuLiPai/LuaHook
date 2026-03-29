plugins {
    alias(libs.plugins.luahook.android.library)
//    alias(libs.plugins.luahook.android.library.compose)
    id("kotlin-parcelize")
}

android {
    namespace = "com.kulipai.luahook.core.navigation"
}

dependencies {
    implementation(libs.androidx.navigation3.runtime)

}