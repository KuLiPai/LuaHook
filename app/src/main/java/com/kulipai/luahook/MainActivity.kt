package com.kulipai.luahook

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kulipai.luahook.navigation.AppNavHost
import com.kulipai.luahook.core.theme.ThemePreferenceManager
import org.koin.compose.koinInject

/**
 * @author kulipai
 * @date 2026/3/17
 */

class MainActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.S)
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themePreferenceManager = koinInject<ThemePreferenceManager>()
            val themePreference by themePreferenceManager.themePreference.collectAsStateWithLifecycle()
            val isThemeReady by themePreferenceManager.isReady.collectAsStateWithLifecycle()

            splashScreen.setKeepOnScreenCondition {
                !isThemeReady
            }

            if (!isThemeReady) return@setContent

            AppNavHost(themePreference = themePreference)
        }
    }
}
