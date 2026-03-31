package com.kulipai.luahook.core.model

enum class UiMode(val value: String) {
    Miuix("miuix"),
    Material("material");

    companion object {
        const val DEFAULT_VALUE = "miuix"

        fun fromValue(value: String): UiMode = when (value) {
            Material.value -> Material
            else -> Miuix
        }
    }
}
