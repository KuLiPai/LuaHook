package com.kulipai.luahook

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.navigation3.runtime.NavKey
import com.kulipai.luahook.core.navigation.Navigator

@Composable
fun rememberNavigator(startRoute: NavKey): Navigator {
    return rememberSaveable(startRoute, saver = Navigator.Saver) {
        Navigator(startRoute)
    }
}
