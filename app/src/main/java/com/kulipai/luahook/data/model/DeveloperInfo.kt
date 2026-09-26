package com.kulipai.luahook.data.model

import androidx.annotation.DrawableRes

data class DeveloperInfo(
    val name: String,
    val contribution: String,
    @DrawableRes val avatarRes: Int,
    val githubUrl: String
)
