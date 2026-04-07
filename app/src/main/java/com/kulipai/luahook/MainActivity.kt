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
import com.kulipai.luahook.core.data.repository.SettingsRepository
import com.kulipai.luahook.navigation.AppNavHost
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
            val settingsRepository = koinInject<SettingsRepository>()

            val userSettings by settingsRepository.userSettings
                .collectAsStateWithLifecycle(initialValue = null)

            val isReady = userSettings != null

            splashScreen.setKeepOnScreenCondition {
                !isReady
            }

            if (!isReady) return@setContent

            AppNavHost(themeSettings= userSettings!!.theme)
        }
    }
}
