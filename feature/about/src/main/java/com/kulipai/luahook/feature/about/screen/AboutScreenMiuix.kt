package com.kulipai.luahook.feature.about.screen

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar

@Composable
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
fun AboutScreenMiuix() {
    Scaffold(
        topBar = { TopAppBar(title = "关于") }
    ) { _ ->

    }
}
