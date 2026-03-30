plugins {
    alias(libs.plugins.luahook.android.library)
    id("kotlin-parcelize")
}

android {
    namespace = "com.kulipai.luahook.core.navigation"
}

dependencies {
    implementation(libs.androidx.navigation3.runtime)
}