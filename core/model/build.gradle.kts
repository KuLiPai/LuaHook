plugins {
    alias(libs.plugins.luahook.android.library)
    id("kotlin-parcelize")
}
android {
    namespace = "com.kulipai.luahook.core.model"
}

dependencies {
    api(libs.material.kolor)
}
