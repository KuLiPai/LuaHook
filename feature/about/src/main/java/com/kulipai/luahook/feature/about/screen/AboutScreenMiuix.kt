package com.kulipai.luahook.feature.about.screen

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Scaffold

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun AboutScreenMiuix() {

    Scaffold() { innerPadding ->

        Column(
            modifier = Modifier.padding(innerPadding)
        ) {
            Button({

            }, modifier = Modifier.safeContentPadding()) { Text("MiUiX") }
        }

    }

}
