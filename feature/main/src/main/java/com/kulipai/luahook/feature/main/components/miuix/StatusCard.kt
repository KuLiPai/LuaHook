/*
 * This file is based on code from KernelSU
 * (https://github.com/tiann/KernelSU)
 *
 * Licensed under GPL-3.0
 *
 * Modifications by KuLiPai
 */

package com.kulipai.luahook.feature.main.components.miuix

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kulipai.luahook.core.theme.isInDarkTheme
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.isDynamicColor
import top.yukonga.miuix.kmp.utils.PressFeedbackType

@Composable
fun StatusCard(
//    kernelVersion: KernelVersion,
    ksuVersion: Int?,
    lkmMode: Boolean?,
    isSafeMode: Boolean = true,
    isLateLoadMode: Boolean = true,
//    isSELinuxPermissive: Boolean,
    superuserCount: Int = 100,
    moduleCount: Int = 213980,
    onClickInstall: () -> Unit = {},
//    onClickJailbreak: () -> Unit = {},
    onClickSuperuser: () -> Unit = {},
    onclickModule: () -> Unit = {},
) {
    Column(
        modifier = Modifier
    ) {
        when {
            ksuVersion != null -> {
                val workingState = ""/*buildString {
                    if (isSafeMode) {
                        append(" ${"Lsposed"}")
                    }
                    if (isLateLoadMode) {
                        append(" ${"Lsposed"}")
                    }
                }*/

                val workingMode = when (lkmMode) {
                    null -> ""
                    true -> " Root"
                    else -> " <GKI>"
                }

                val workingText = "${"工作中"}$workingMode$workingState"

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        colors = CardDefaults.defaultColors(
                            color = when {
                                isDynamicColor -> colorScheme.secondaryContainer
                                isInDarkTheme() -> Color(0xFF1A3825)
                                else -> Color(0xFFDFFAE4)
                            }
                        ),
                        onClick = {
                            onClickInstall()
                        },
                        showIndication = true,
                        pressFeedbackType = PressFeedbackType.Tilt
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .offset(38.dp, 45.dp),
                                contentAlignment = Alignment.BottomEnd
                            ) {
                                Icon(
                                    modifier = Modifier.size(170.dp),
                                    imageVector = Icons.Rounded.CheckCircleOutline,
                                    tint = if (isDynamicColor) {
                                        colorScheme.primary.copy(alpha = 0.8f)
                                    } else {
                                        Color(0xFF36D167)
                                    },
                                    contentDescription = null
                                )
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(all = 16.dp)
                            ) {
                                Text(
                                    modifier = Modifier.fillMaxWidth(),
                                    text = workingText,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    modifier = Modifier.fillMaxWidth(),
                                    text = "版本: 123932",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            insideMargin = PaddingValues(16.dp),
                            onClick = { onClickSuperuser() },
                            showIndication = true,
                            pressFeedbackType = PressFeedbackType.Tilt
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    modifier = Modifier.fillMaxWidth(),
                                    text = "应用",
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 15.sp,
                                    color = colorScheme.onSurfaceVariantSummary,
                                )
                                Text(
                                    modifier = Modifier.fillMaxWidth(),
                                    text = superuserCount.toString(),
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colorScheme.onSurface,
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            insideMargin = PaddingValues(16.dp),
                            onClick = { onclickModule() },
                            showIndication = true,
                            pressFeedbackType = PressFeedbackType.Tilt
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    modifier = Modifier.fillMaxWidth(),
                                    text = "脚本",
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 15.sp,
                                    color = colorScheme.onSurfaceVariantSummary,
                                )
                                Text(
                                    modifier = Modifier.fillMaxWidth(),
                                    text = moduleCount.toString(),
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }

//            kernelVersion.isGKI() -> {
//                Row(
//                    horizontalArrangement = Arrangement.spacedBy(12.dp),
//                ) {
//                    Card(
//                        modifier = Modifier.weight(1f),
//                        onClick = {
//                            onClickInstall()
//                        },
//                        showIndication = true,
//                        pressFeedbackType = PressFeedbackType.Sink
//                    ) {
//                        BasicComponent(
//                            title = "R.string.home_not_installed",
//                            summary = "R.string.home_click_to_install",
//                            startAction = {
//                                Icon(
//                                    Icons.Rounded.ErrorOutline,
//                                    "R.string.home_not_installed",
//                                    modifier = Modifier
//                                        .padding(end = 16.dp),
//                                    tint = colorScheme.onBackground,
//                                )
//                            },
//                            endActions = {
//                                if (isSELinuxPermissive) {
//                                    TextButton(
//                                        text = "R.string.home_jailbreak",
//                                        insideMargin = PaddingValues(12.dp),
//                                        onClick = {
//                                            onClickJailbreak()
//                                        },
//                                        colors = ButtonDefaults.textButtonColorsPrimary()
//                                    )
//                                }
//                            }
//                        )
//                    }
//                }
//            }

            else -> {
                Card(
                    onClick = {
                        onClickInstall()
                    },
                    showIndication = true,
                    pressFeedbackType = PressFeedbackType.Sink
                ) {
                    BasicComponent(
                        title = "R.string.home_unsupported",
                        summary = "R.string.home_unsupported_reason",
                        startAction = {
                            Icon(
                                Icons.Rounded.ErrorOutline,
                                "R.string.home_unsupported",
                                modifier = Modifier
                                    .padding(end = 16.dp),
                                tint = colorScheme.onBackground,
                            )
                        }
                    )
                }
            }
        }
    }
}
