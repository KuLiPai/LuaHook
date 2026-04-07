package com.kulipai.luahook.core.data.model


data class AppSettings(
    val language: String = DEFAULT_LANGUAGE,
    ) {
    companion object {
       const val DEFAULT_LANGUAGE = ""
    }
}