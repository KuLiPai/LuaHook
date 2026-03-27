package com.kulipai.luahook.ui.screens.main.pager.setting

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ContactPage
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.ElectricalServices
import androidx.compose.material.icons.filled.Fence
import androidx.compose.material.icons.filled.FolderDelete
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.RemoveModerator
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kulipai.luahook.R
import com.kulipai.luahook.data.model.SettingsUiState
import com.kulipai.luahook.ui.components.material.SegmentedColumn
import com.kulipai.luahook.ui.components.material.SegmentedDropdownItem
import com.kulipai.luahook.ui.components.material.SegmentedListItem
import com.kulipai.luahook.ui.components.material.SegmentedSwitchItem
import com.kulipai.luahook.ui.navigation3.Navigator
import com.kulipai.luahook.ui.navigation3.Route
import com.kulipai.luahook.ui.theme.UiMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.viewmodel.koinViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingPagerMaterial(
    navigator: Navigator,
    bottomInnerPadding: Dp
) {

    val viewModel: SettingsViewModel = koinViewModel()
    val uiMode by viewModel.uiMode.collectAsStateWithLifecycle()
    val uiState = SettingsUiState()


    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
//    val snackBarHost = LocalSnackbarHost.current

    Scaffold(
        topBar = {
            TopBar(
                scrollBehavior = scrollBehavior
            )
        },
//        snackbarHost = { SnackbarHost(snackBarHost) },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    ) { paddingValues ->
//        val loadingDialog = rememberLoadingDialog()
        val showUninstallDialog = rememberSaveable { mutableStateOf(false) }

//        UninstallDialog(showUninstallDialog, navigator)

        Column(
            modifier = Modifier
                .padding(paddingValues)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState())
        ) {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
//            val logSavedText = stringResource(R.string.log_saved)
//            val sendLogText = stringResource(R.string.send_log)
/*

            val exportBugreportLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.CreateDocument("application/gzip")
            ) { uri: Uri? ->
                if (uri == null) return@rememberLauncherForActivityResult
                scope.launch(Dispatchers.IO) {
                    loadingDialog.show()
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        getBugreportFile(context).inputStream().use {
                            it.copyTo(output)
                        }
                    }
                    loadingDialog.hide()
                    snackBarHost.showSnackbar(logSavedText)
                }
            }
*/

            Spacer(modifier = Modifier.height(8.dp))
                SegmentedColumn(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    content = listOf(
                        {
                            SegmentedSwitchItem(
                                icon = Icons.Filled.Update,
                                title = "settings_check_update",
                                summary = "settings_check_update_summary",
                                checked = uiState.checkUpdate,
                                onCheckedChange = { bool ->
//                                    viewModel.setCheckUpdate(bool)
                                }
                            )
                        },
                        {
                            SegmentedSwitchItem(
                                icon = Icons.Rounded.UploadFile,
                                title = "settings_module_check_update",
                                summary = "settings_check_update_summary",
                                checked = uiState.checkModuleUpdate,
                                onCheckedChange = { bool ->
//                                    viewModel.setCheckModuleUpdate(bool)
                                }
                            )
                        }
                    )
                )

            val uiModeItems = listOf(UiMode.Miuix.name, UiMode.Material.name)
            SegmentedColumn(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                content = buildList {
                    add {
                        SegmentedDropdownItem(
                            icon = Icons.Rounded.Dashboard,
                            title = "settings_ui_mode",
                            summary = "settings_ui_mode_summary",
                            items = uiModeItems,
                            selectedIndex = if (uiMode == UiMode.Material) 1 else 0,
                            onItemSelected = { index ->
                                viewModel.setUiMode(if (index == 0) UiMode.Miuix.value else UiMode.Material.value)
                            }
                        )
                    }
                    add {
                        SegmentedListItem(
                            onClick = { navigator.push(Route.ColorPalette) },
                            headlineContent = { Text("settings_theme") },
                            supportingContent = { Text("settings_theme_summary") },
                            leadingContent = { Icon(Icons.Filled.Palette, "settings_theme") },
                            trailingContent = {
                                Icon(
                                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    null
                                )
                            }
                        )
                    }
                }
            )

            val profileTemplate = "settings_profile_template"
                SegmentedColumn(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    content = listOf {
                        SegmentedListItem(
                            onClick = { navigator.push(Route.AppProfileTemplate) },
                            headlineContent = { Text(profileTemplate) },
                            supportingContent = { Text("settings_profile_template_summary") },
                            leadingContent = { Icon(Icons.Filled.Fence, profileTemplate) },
                            trailingContent = {
                                Icon(
                                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    null
                                )
                            }
                        )
                    }
                )


                val suCompatModeItems = listOf(
                    "settings_mode_enable_by_default",
                    "settings_mode_disable_until_reboot",
                    "settings_mode_disable_always",
                )

                SegmentedColumn(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    content = listOf(
                        {
                            val suSummary = when (uiState.suCompatStatus) {
                                "unsupported" -> "feature_status_unsupported_summary"
                                "managed" -> "feature_status_managed_summary"
                                else -> "settings_sucompat_summary"
                            }
                            SegmentedDropdownItem(
                                icon = Icons.Filled.RemoveModerator,
                                title = "settings_sucompat",
                                summary = suSummary,
                                items = suCompatModeItems,
                                enabled = uiState.suCompatStatus == "supported",
                                selectedIndex = uiState.suCompatMode,
                                onItemSelected = { index ->
//                                    viewModel.setSuCompatMode(index)
                                }
                            )
                        },
                        {
                            val umountSummary = when (uiState.kernelUmountStatus) {
                                "unsupported" -> "feature_status_unsupported_summary"
                                "managed" -> "feature_status_managed_summary"
                                else -> "settings_kernel_umount_summary"
                            }
                            SegmentedSwitchItem(
                                icon = Icons.Filled.RemoveCircle,
                                title = "settings_kernel_umount",
                                summary = umountSummary,
                                enabled = uiState.kernelUmountStatus == "supported",
                                checked = uiState.isKernelUmountEnabled,
                            ) {
//                                viewModel.setKernelUmountEnabled(it)
                            }
                        },
                        {
                            SegmentedSwitchItem(
                                icon = Icons.Filled.FolderDelete,
                                title = "settings_umount_modules_default",
                                summary = "settings_umount_modules_default_summary",
                                checked = uiState.isDefaultUmountModules,
                                onCheckedChange = {
//                                    viewModel.setDefaultUmountModules(it)
                                }
                            )
                        },
                        {
                            SegmentedSwitchItem(
                                icon = Icons.Filled.DeveloperMode,
                                title = "enable_web_debugging",
                                summary = "enable_web_debugging_summary",
                                checked = uiState.enableWebDebugging,
                                onCheckedChange = {
//                                    viewModel.setEnableWebDebugging(it)
                                }
                            )
                        },
                        {
                            SegmentedSwitchItem(
                                icon = Icons.Filled.ElectricalServices,
                                title = "settings_auto_jailbreak",
                                summary = "settings_auto_jailbreak_summary",
                                checked = uiState.autoJailbreak,
                                onCheckedChange = {
//                                    viewModel.setAutoJailbreak(it)
                                }
                            )
                        }
                    )
                )
/*

            if (Natives.isLkmMode) {
                SegmentedColumn(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    content = listOf(
                        {
                            val uninstall = "settings_uninstall"
                            SegmentedListItem(
                                onClick = { showUninstallDialog.value = true },
                                headlineContent = { Text(uninstall) },
                                leadingContent = { Icon(Icons.Filled.Delete, uninstall) }
                            )
                        }
                    )
                )
            }
*/

            var showBottomsheet by remember { mutableStateOf(false) }
            SegmentedColumn(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                content = listOf(
                    {
                        SegmentedListItem(
                            onClick = { showBottomsheet = true },
                            headlineContent = { Text("send_log") },
                            leadingContent = {
                                Icon(
                                    Icons.Filled.BugReport,
                                    "send_log"
                                )
                            },
                        )
                    },
                    {
                        SegmentedListItem(
                            onClick = { navigator.push(Route.About) },
                            headlineContent = { Text("about") },
                            leadingContent = {
                                Icon(
                                    Icons.Filled.ContactPage,
                                    "about"
                                )
                            },
                        )
                    }
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (showBottomsheet) {
                ModalBottomSheet(
                    onDismissRequest = { showBottomsheet = false },
                    content = {
                        Row(
                            modifier = Modifier
                                .padding(10.dp)
                                .align(Alignment.CenterHorizontally)

                        ) {
                            Box {
                                Column(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .clickable {
                                            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH_mm")
                                            val current = LocalDateTime.now().format(formatter)
//                                            exportBugreportLauncher.launch("KernelSU_bugreport_${current}.tar.gz")
                                            showBottomsheet = false
                                        }
                                ) {
                                    Icon(
                                        Icons.Filled.Save,
                                        contentDescription = null,
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    )
                                    Text(
                                        text = "save_log",
                                        modifier = Modifier.padding(top = 16.dp),
                                        textAlign = TextAlign.Center.also {
                                            LineHeightStyle(
                                                alignment = LineHeightStyle.Alignment.Center,
                                                trim = LineHeightStyle.Trim.None
                                            )
                                        }

                                    )
                                }
                            }
                            Box {
                                Column(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .clickable {
                                            scope.launch {/*
                                                val bugreport = loadingDialog.withLoading {
                                                    withContext(Dispatchers.IO) {
                                                        getBugreportFile(context)
                                                    }
                                                }

                                                val uri: Uri =
                                                    FileProvider.getUriForFile(
                                                        context,
                                                        "${BuildConfig.APPLICATION_ID}.fileprovider",
                                                        bugreport
                                                    )

                                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                    putExtra(Intent.EXTRA_STREAM, uri)
                                                    setDataAndType(uri, "application/gzip")
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }

                                                context.startActivity(
                                                    Intent.createChooser(
                                                        shareIntent,
                                                        sendLogText
                                                    )
                                                )
                                            */}
                                        }
                                ) {
                                    Icon(
                                        Icons.Filled.Share,
                                        contentDescription = null,
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    )
                                    Text(
                                        text = "send_log",
                                        modifier = Modifier.padding(top = 16.dp),
                                        textAlign = TextAlign.Center.also {
                                            LineHeightStyle(
                                                alignment = LineHeightStyle.Alignment.Center,
                                                trim = LineHeightStyle.Trim.None
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                )
            }
            Spacer(modifier = Modifier.height(bottomInnerPadding))
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TopBar(
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    LargeFlexibleTopAppBar(
        title = { Text(stringResource(R.string.settings)) },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surface
        ),
        windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        scrollBehavior = scrollBehavior
    )
}

