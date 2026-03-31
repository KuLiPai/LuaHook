plugins {
    alias(libs.plugins.luahook.android.library)
}
android {
    namespace = "com.kulipai.luahook.core.model"
}

dependencies {
    api(libs.material.kolor)
}
