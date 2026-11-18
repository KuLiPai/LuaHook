package com.kulipai.luahook.core.language

import android.content.Context
import android.content.res.Configuration
import androidx.core.content.edit
import java.util.Locale

/**
 * 语言工具类
 */

object LanguageUtils {

    private const val KEY_LANGUAGE = "key_language"
    
    // Language Codes
    const val LANGUAGE_FOLLOW_SYSTEM = "system"
    const val LANGUAGE_GERMAN = "de"
    const val LANGUAGE_ENGLISH = "en"
    const val LANGUAGE_SPANISH = "es"
    const val LANGUAGE_FRENCH = "fr"
    const val LANGUAGE_HINDI = "hi"
    const val LANGUAGE_JAPANESE = "ja"
    const val LANGUAGE_CHINESE_TRADITIONAL = "zh-TW"
    const val LANGUAGE_CHINESE_SIMPLIFIED = "zh-CN"
    const val LANGUAGE_KOREAN = "ko"
    const val LANGUAGE_PORTUGUESE = "pt"

    // Default value
    const val LANGUAGE_DEFAULT = LANGUAGE_FOLLOW_SYSTEM

    /**
     * Set language
     */
    fun changeLanguage(context: Context, language: String) {
        val locale = getLocaleByCode(language)

        Locale.setDefault(locale)
        val resources = context.resources
        val configuration = Configuration(resources.configuration)
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale) // Support RTL
        resources.updateConfiguration(configuration, resources.displayMetrics)

        val appContext = context.applicationContext
        val appResources = appContext.resources
        val appConfig = Configuration(appResources.configuration)
        appConfig.setLocale(locale)
        appConfig.setLayoutDirection(locale) // Support RTL
        appResources.updateConfiguration(appConfig, appResources.displayMetrics)

        val preferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        preferences.edit { putString(KEY_LANGUAGE, language) }
    }

    /**
     * Get Locale from code
     */
    private fun getLocaleByCode(language: String): Locale {
        if (language == LANGUAGE_FOLLOW_SYSTEM) {
            return getSystemLocale()
        }
        return when (language) {
            LANGUAGE_GERMAN -> Locale.GERMAN
            LANGUAGE_ENGLISH -> Locale.ENGLISH
            LANGUAGE_SPANISH -> Locale("es")
            LANGUAGE_FRENCH -> Locale.FRENCH
            LANGUAGE_HINDI -> Locale("hi")
            LANGUAGE_JAPANESE -> Locale.JAPANESE
            LANGUAGE_CHINESE_TRADITIONAL -> Locale.TRADITIONAL_CHINESE
            LANGUAGE_CHINESE_SIMPLIFIED -> Locale.SIMPLIFIED_CHINESE
            "zh" -> Locale.SIMPLIFIED_CHINESE // Backward compatibility
            LANGUAGE_KOREAN -> Locale.KOREAN
            LANGUAGE_PORTUGUESE -> Locale("pt", "BR")
            else -> Locale.ENGLISH
        }
    }

    /**
     * Get system locale
     */
    private fun getSystemLocale(): Locale {
        val systemConfig = android.content.res.Resources.getSystem().configuration
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            systemConfig.locales[0]
        } else {
            @Suppress("DEPRECATION")
            systemConfig.locale
        }
    }

    /**
     * Apply language settings
     */
    fun applyLanguage(context: Context) {
        val preferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val language = preferences.getString(KEY_LANGUAGE, LANGUAGE_DEFAULT) ?: LANGUAGE_DEFAULT
        changeLanguage(context, language)
    }

    /**
     * Get current language code
     */
    fun getCurrentLanguage(context: Context): String {
        val preferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        return preferences.getString(KEY_LANGUAGE, LANGUAGE_DEFAULT) ?: LANGUAGE_DEFAULT
    }
}