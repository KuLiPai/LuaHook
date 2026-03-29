package com.kulipai.luahook.feature.main.pager.setting

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.ContactPage
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeveloperMode
import androidx.compose.material.icons.rounded.ElectricalServices
import androidx.compose.material.icons.rounded.Fence
import androidx.compose.material.icons.rounded.FolderDelete
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.RemoveCircle
import androidx.compose.material.icons.rounded.RemoveModerator
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kulipai.luahook.R
import com.kulipai.luahook.core.data.model.SettingsUiState
import com.kulipai.luahook.ui.navigation3.Navigator
import com.kulipai.luahook.ui.navigation3.Route
import com.kulipai.luahook.core.theme.UiMode
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import org.koin.compose.viewmodel.koinViewModel
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.extra.SuperArrow
import top.yukonga.miuix.kmp.extra.SuperDropdown
import top.yukonga.miuix.kmp.extra.SuperSwitch
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic


@Composable
fun SettingPagerMiuix(
    navigator: Navigator,
    bottomInnerPadding: Dp
) {

    val viewModel: SettingsViewModel = koinViewModel()
    val uiState = SettingsUiState()

    val scrollBehavior = MiuixScrollBehavior()
    val hazeState = remember { HazeState() }
    val hazeStyle = HazeStyle(
        backgroundColor = colorScheme.surface,
        tint = HazeTint(colorScheme.surface.copy(0.8f))
    )

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = if (uiState.enableBlur) {
                    Modifier.hazeEffect(hazeState) {
                        style = hazeStyle
                        blurRadius = 30.dp
                        noiseFactor = 0f
                    }
                } else {
                    Modifier
                },
                color = if (uiState.enableBlur) Color.Transparent else colorScheme.surface,
                title = stringResource(R.string.settings),
                scrollBehavior = scrollBehavior
            )
        },
        popupHost = { },
        contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout).only(WindowInsetsSides.Horizontal)
    ) { innerPadding ->
//        val loadingDialog = rememberLoadingDialog()
        val showUninstallDialog = rememberSaveable { mutableStateOf(false) }
        val showSendLogDialog = rememberSaveable { mutableStateOf(false) }

        LazyColumn(
            modifier = Modifier
                .fillMaxHeight()
                .scrollEndHaptic()
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .hazeSource(state = hazeState)
                .padding(horizontal = 12.dp),
            contentPadding = innerPadding,
            overscrollEffect = null,
        ) {
            item {
                Card(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth(),
                ) {
                    SuperSwitch(
                        title = "settings_check_update",
                        summary = "settings_check_update_summary",
                        startAction = {
                            Icon(
                                Icons.Rounded.Update,
                                modifier = Modifier.padding(end = 6.dp),
                                contentDescription = "settings_check_update",
                                tint = colorScheme.onBackground
                            )
                        },
                        checked = uiState.checkUpdate,
                        onCheckedChange = {
//                            viewModel.setCheckUpdate(it)
                        }
                    )

                        SuperSwitch(
                            title = "settings_module_check_update",
                            summary = "settings_check_update_summary",
                            startAction = {
                                Icon(
                                    Icons.Rounded.UploadFile,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = "settings_check_update",
                                    tint = colorScheme.onBackground
                                )
                            },
                            checked = uiState.checkModuleUpdate,
                            onCheckedChange = {
//                                viewModel.setCheckModuleUpdate(it)
                            }
                        )
                    }


                Card(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth(),
                ) {
                    val uiModeItems = listOf(UiMode.Miuix.name, UiMode.Material.name)
                    SuperDropdown(
                        title = "settings_ui_mode",
                        summary = "settings_ui_mode_summary",
                        items = uiModeItems,
                        startAction = {
                            Icon(
                                Icons.Rounded.Dashboard,
                                modifier = Modifier.padding(end = 6.dp),
                                contentDescription = "settings_ui_mode",
                                tint = colorScheme.onBackground
                            )
                        },
                        selectedIndex = if (uiState.uiMode == UiMode.Material.value) 1 else 0,
                        onSelectedIndexChange = { index ->
                            viewModel.setUiMode(if (index == 0) UiMode.Miuix.value else UiMode.Material.value)
                        }
                    )
                    SuperArrow(
                        title = "settings_theme",
                        summary = "settings_theme_summary",
                        startAction = {
                            Icon(
                                Icons.Rounded.Palette,
                                modifier = Modifier.padding(end = 6.dp),
                                contentDescription = "settings_theme",
                                tint = colorScheme.onBackground
                            )
                        },
                        onClick = {
                            navigator.push(Route.ColorPalette)
                        }
                    )
                }

                    Card(
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .fillMaxWidth(),
                    ) {
                        val profileTemplate = "settings_profile_template"
                        SuperArrow(
                            title = profileTemplate,
                            summary = "settings_profile_template_summary",
                            startAction = {
                                Icon(
                                    Icons.Rounded.Fence,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = profileTemplate,
                                    tint = colorScheme.onBackground
                                )
                            },
                            onClick = {
                                navigator.push(Route.AppProfileTemplate)
                            }
                        )
                    }



                    Card(
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .fillMaxWidth(),
                    ) {
                        val suCompatModeItems = listOf(
                            "settings_mode_enable_by_default",
                            "settings_mode_disable_until_reboot",
                            "settings_mode_disable_always",
                        )

                        val suSummary = when (uiState.suCompatStatus) {
                            "unsupported" -> "feature_status_unsupported_summary"
                            "managed" -> "feature_status_managed_summary"
                            else -> "settings_sucompat_summary"
                        }
                        SuperDropdown(
                            title = "settings_sucompat",
                            summary = suSummary,
                            items = suCompatModeItems,
                            startAction = {
                                Icon(
                                    Icons.Rounded.RemoveModerator,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = "settings_sucompat",
                                    tint = colorScheme.onBackground
                                )
                            },
                            enabled = uiState.suCompatStatus == "supported",
                            selectedIndex = uiState.suCompatMode,
                            onSelectedIndexChange = { index ->
//                                viewModel.setSuCompatMode(index)
                            }
                        )

                        val umountSummary = when (uiState.kernelUmountStatus) {
                            "unsupported" -> "feature_status_unsupported_summary"
                            "managed" -> "feature_status_managed_summary"
                            else -> "settings_kernel_umount_summary"
                        }
                        SuperSwitch(
                            title = "settings_kernel_umount",
                            summary = umountSummary,
                            startAction = {
                                Icon(
                                    Icons.Rounded.RemoveCircle,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = "settings_kernel_umount",
                                    tint = colorScheme.onBackground
                                )
                            },
                            enabled = uiState.kernelUmountStatus == "supported",
                            checked = uiState.isKernelUmountEnabled,
                            onCheckedChange = { checked ->
//                                viewModel.setKernelUmountEnabled(checked)
                            }
                        )

                        SuperSwitch(
                            title = "settings_umount_modules_default",
                            summary = "settings_umount_modules_default_summary",
                            startAction = {
                                Icon(
                                    Icons.Rounded.FolderDelete,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = "settings_umount_modules_default",
                                    tint = colorScheme.onBackground
                                )
                            },
                            checked = uiState.isDefaultUmountModules,
                            onCheckedChange = {
//                                viewModel.setDefaultUmountModules(it)
                            }
                        )

                        SuperSwitch(
                            title = "enable_web_debugging",
                            summary = "enable_web_debugging_summary",
                            startAction = {
                                Icon(
                                    Icons.Rounded.DeveloperMode,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = "enable_web_debugging",
                                    tint = colorScheme.onBackground
                                )
                            },
                            checked = uiState.enableWebDebugging,
                            onCheckedChange = {
//                                viewModel.setEnableWebDebugging(it)
                            }
                        )
                        SuperSwitch(
                            title = "settings_auto_jailbreak",
                            summary = "settings_auto_jailbreak_summary",
                            startAction = {
                                Icon(
                                    Icons.Rounded.ElectricalServices,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = "settings_auto_jailbreak",
                                    tint = colorScheme.onBackground
                                )
                            },
                            checked = uiState.autoJailbreak,
                            onCheckedChange = {
//                                viewModel.setAutoJailbreak(it)
                            }
                        )
                    }



                    Card(
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .fillMaxWidth(),
                    ) {
                        if (uiState.isLkmMode) {
                            val uninstall = "settings_uninstall"
                            SuperArrow(
                                title = uninstall,
                                startAction = {
                                    Icon(
                                        Icons.Rounded.Delete,
                                        modifier = Modifier.padding(end = 6.dp),
                                        contentDescription = uninstall,
                                        tint = colorScheme.onBackground,
                                    )
                                },
                                onClick = {
                                    showUninstallDialog.value = true
                                }
                            )
//                            UninstallDialogMiuix(showUninstallDialog, navigator)
                        }
                    }


                Card(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .fillMaxWidth(),
                ) {
                    SuperArrow(
                        title = "send_log",
                        startAction = {
                            Icon(
                                Icons.Rounded.BugReport,
                                modifier = Modifier.padding(end = 6.dp),
                                contentDescription = "send_log",
                                tint = colorScheme.onBackground
                            )
                        },
                        onClick = {
                            showSendLogDialog.value = true
                        },
                    )
//                    SendLogDialogMiuix(showSendLogDialog, loadingDialog)
                    val about = "about"
                    SuperArrow(
                        title = about,
                        startAction = {
                            Icon(
                                Icons.Rounded.ContactPage,
                                modifier = Modifier.padding(end = 6.dp),
                                contentDescription = about,
                                tint = colorScheme.onBackground
                            )
                        },
                        onClick = {
                            navigator.push(Route.About)
                        }
                    )
                }
                Spacer(Modifier.height(bottomInnerPadding))
            }
        }
    }

}