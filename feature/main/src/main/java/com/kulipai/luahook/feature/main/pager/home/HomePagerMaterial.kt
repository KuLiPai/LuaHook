package com.kulipai.luahook.feature.main.pager.home

import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.kulipai.luahook.core.navigation.Navigator
import com.kulipai.luahook.core.navigation.Route

@Composable
fun HomePagerMaterial(
    navigator: Navigator,
    bottomInnerPadding: Dp
) {
    Button({
        navigator.push(Route.About)
    }, modifier = Modifier.safeContentPadding()) { Text("Material") }
}