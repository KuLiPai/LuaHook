package com.kulipai.luahook.feature.about

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier


@Composable
fun AboutScreenMaterial() {
    Scaffold() { innerPadding ->

        Column(
            modifier = Modifier.padding(innerPadding)
        ) {
            Button({

            }, modifier = Modifier.safeContentPadding()) { Text("MiUiX") }
        }

    }


}