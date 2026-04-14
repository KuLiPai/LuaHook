package com.kulipai.luahook.core.ui.component.statuscard


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kulipai.luahook.core.ui.R
import com.kulipai.luahook.core.ui.component.material.TonalCard
import com.kulipai.luahook.core.ui.component.statustag.StatusTag

@Composable
fun StatusCardMaterial(
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
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TonalCard(
            containerColor = run {
                if (ksuVersion != null) MaterialTheme.colorScheme.secondaryContainer
                else MaterialTheme.colorScheme.errorContainer
            }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClickInstall() }
                    .padding(24.dp), verticalAlignment = Alignment.CenterVertically
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
                            true -> "Root"
                            else -> "<GKI>"
                        }


                        val workingText = "${"工作中"}$workingMode"


                        Icon(Icons.Outlined.CheckCircle, "工作中")
                        Column(Modifier.padding(start = 20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = workingText,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                if (workingMode.isNotEmpty()) {
                                    Spacer(Modifier.width(8.dp))
                                    StatusTag(
                                        label = workingMode,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                        backgroundColor = MaterialTheme.colorScheme.primary
                                    )
                                }
//                                if (isSafeMode) {
//                                    Spacer(Modifier.width(8.dp))
//                                    StatusTag(
//                                        label = "id = R.string.safe_mode",
//                                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
//                                        backgroundColor = MaterialTheme.colorScheme.errorContainer
//                                    )
//                                }
//                                if (isLateLoadMode) {
//                                    Spacer(Modifier.width(8.dp))
//                                    StatusTag(
//                                        label = "id = R.string.jailbreak_mode",
//                                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
//                                        backgroundColor = MaterialTheme.colorScheme.tertiaryContainer
//                                    )
//                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "版本: 123123",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

//                    kernelVersion.isGKI() -> {
//                        Icon(Icons.Outlined.Warning, "R.string.home_not_installed")
//                        Column(
//                            Modifier
//                                .padding(start = 20.dp)
//                                .weight(1f)
//                        ) {
//                            Text(
//                                text = "R.string.home_not_installed",
//                                style = MaterialTheme.typography.titleMedium
//                            )
//                            Spacer(Modifier.height(4.dp))
//                            Text(
//                                text = "R.string.home_click_to_install",
//                                style = MaterialTheme.typography.bodyMedium
//                            )
//                        }
//                        if (isSELinuxPermissive) {
//                            Button(
//                                onClick = onClickJailbreak,
//                                colors = ButtonDefaults.buttonColors(
//                                    containerColor = MaterialTheme.colorScheme.error,
//                                    contentColor = MaterialTheme.colorScheme.onError
//                                )
//                            ) {
//                                Text("R.string.home_jailbreak")
//                            }
//                        }
//                    }

                    else -> {
                        Icon(Icons.Outlined.Block, "R.string.home_unsupported")
                        Column(Modifier.padding(start = 20.dp)) {
                            Text(
                                text = "R.string.home_unsupported",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "R.string.home_unsupported_reason",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
//        if (fullFeatured == true) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TonalCard(modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onClickSuperuser() }
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.apps),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = superuserCount.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
            TonalCard(modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onclickModule() }
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.module),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = moduleCount.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
//        }
    }
}
