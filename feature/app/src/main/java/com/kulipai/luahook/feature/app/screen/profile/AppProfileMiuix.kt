package com.kulipai.luahook.feature.app.screen.profile

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import com.kulipai.luahook.core.model.AppInfo
import com.kulipai.luahook.core.navigation.LocalNavigator
import com.kulipai.luahook.core.navigation.Route
import com.kulipai.luahook.core.theme.currentEnableBlur
import com.kulipai.luahook.core.ui.component.appiconimage.AppIconImage
import com.kulipai.luahook.feature.app.R
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeSource
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/**
 * @author weishu
 * @date 2023/5/16.
 */
@RequiresApi(Build.VERSION_CODES.P)
@Composable
fun AppProfileScreenMiuix(
    uid: Int,
    packageName: String,
) {
    val viewModel = koinViewModel<AppProfileViewModel>()
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(uid, packageName) {
        viewModel.load(uid, packageName)
    }
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val enableBlur = currentEnableBlur()
    val scrollBehavior = MiuixScrollBehavior()
    val hazeState = remember { HazeState() }
    val hazeStyle = if (enableBlur) {
        HazeStyle(
            backgroundColor = colorScheme.surface,
            tint = HazeTint(colorScheme.surface.copy(0.8f))
        )
    } else {
        HazeStyle.Unspecified
    }
    val scope = rememberCoroutineScope()
    val appInfo = uiState.appInfo
    if (appInfo == null) {
        if (uiState.isLoading) {
            return
        }
        LaunchedEffect(Unit) {
            navigator.pop()
        }
        return
    }
    val failToUpdateAppProfile = stringResource(R.string.failed_to_update_app_profile).format(appInfo.label).format(appInfo.uid)
    val failToUpdateSepolicy = stringResource(R.string.failed_to_update_sepolicy).format(appInfo.label)
    val suNotAllowed = stringResource(R.string.su_not_allowed).format(appInfo.label)
    val sameUidApps = uiState.sameUidApps
    val isUidGroup = sameUidApps.size > 1
    // The package name from the SuperUser is the primary package, so no need to recalculate.
    val sharedUserId = remember(sameUidApps, appInfo) {
        appInfo.packageInfo.sharedUserId
            ?: sameUidApps.firstOrNull { it.packageInfo.sharedUserId != null }?.packageInfo?.sharedUserId
            ?: ""
    }

//    val initialProfile = Natives.getAppProfile(packageName, uid)
//    if (initialProfile.allowSu) {
//        initialProfile.rules = getSepolicy(packageName)
//    }
//    var profile by rememberSaveable {
//        mutableStateOf(initialProfile)
//    }

    Scaffold(
        topBar = {
            TopBar(
                onBack = dropUnlessResumed { navigator.pop() },
                packageName = packageName,
                showActions = !isUidGroup,
                scrollBehavior = scrollBehavior,
                hazeState = hazeState,
                hazeStyle = hazeStyle,
                enableBlur = enableBlur,
            )
        },
        popupHost = { },
        contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout).only(WindowInsetsSides.Horizontal)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxHeight()
                .padding(top = 16.dp)
                .scrollEndHaptic()
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .let { if (enableBlur) it.hazeSource(state = hazeState) else it },
            contentPadding = innerPadding,
            overscrollEffect = null
        ) {
            item {
                AppProfileInner(
                    packageName = if (isUidGroup) "" else appInfo.packageName,
                    appLabel =/* if (isUidGroup) ownerNameForUid(appInfo.uid) else*/ appInfo.label,
                    appIcon = {
                        AppIconImage(
                            packageInfo = appInfo.packageInfo,
                            label = appInfo.label,
                            modifier = Modifier.size(54.dp)
                        )
                    },
                    appUid = uid,
                    sharedUserId = if (isUidGroup) sharedUserId else "",
                    appVersionName = if (isUidGroup) "" else (appInfo.packageInfo.versionName ?: ""),
                    appVersionCode = if (isUidGroup) 0L else appInfo.packageInfo.longVersionCode,
//                    profile = profile,
                    isUidGroup = isUidGroup,
                    affectedApps = sameUidApps,
                    onViewTemplate = {/*
                        getTemplateInfoById(it)?.let { info ->
                            navigator.push(Route.TemplateEditor(info, readOnly = true))
                        }
                    */},
                    onManageTemplate = { navigator.push(Route.AppProfileTemplate) },
//                    onProfileChange = {/*
//                        scope.launch {
//                            if (it.allowSu) {
//                                if (uid < 2000 && uid != 1000) {
//                                    Toast.makeText(context, suNotAllowed, Toast.LENGTH_SHORT).show()
//                                    return@launch
//                                }
//                                if (!it.rootUseDefault && it.rules.isNotEmpty() && !setSepolicy(profile.name, it.rules)) {
//                                    Toast.makeText(context, failToUpdateSepolicy, Toast.LENGTH_SHORT).show()
//                                    return@launch
//                                }
//                            }
//                            if (!Natives.setAppProfile(it)) {
//                                Toast.makeText(context, failToUpdateAppProfile, Toast.LENGTH_SHORT).show()
//                            } else {
//                                profile = it
//                            }
//                        }
//                    */},
                )
                Spacer(
                    Modifier.height(
                        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() +
                                WindowInsets.captionBar.asPaddingValues().calculateBottomPadding()
                    )
                )
            }
        }
    }
}

@Composable
private fun AppProfileInner(
    modifier: Modifier = Modifier,
    packageName: String,
    appLabel: String,
    appIcon: @Composable (() -> Unit),
    appUid: Int,
    sharedUserId: String = "",
    appVersionName: String,
    appVersionCode: Long,
//    profile: Natives.Profile,
    isUidGroup: Boolean = false,
    affectedApps: List<AppInfo> = emptyList(),
    onViewTemplate: (id: String) -> Unit = {},
    onManageTemplate: () -> Unit = {},
//    onProfileChange: (Natives.Profile) -> Unit,
) {/*
//    val isRootGranted = profile.allowSu
    val userId = appUid / 100000
    val appId = appUid % 100000

    Column(
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp),
            insideMargin = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                appIcon()
                Column(
                    modifier = Modifier
                        .padding(start = 16.dp, end = 8.dp)
                        .weight(1f),
                ) {
                    Text(
                        text = appLabel,
                        color = colorScheme.onSurface,
                        fontWeight = FontWeight(550),
                        modifier = Modifier
                            .basicMarquee(),
                        maxLines = 1,
                        softWrap = false
                    )
                    if (!isUidGroup) {
                        Text(
                            text = "$appVersionName ($appVersionCode)",
                            fontSize = 12.sp,
                            color = colorScheme.onSurfaceVariantSummary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .basicMarquee(),
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = packageName,
                            fontSize = 12.sp,
                            color = colorScheme.onSurfaceVariantSummary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .basicMarquee(),
                            maxLines = 1,
                            softWrap = false
                        )
                    } else {
                        if (sharedUserId.isNotEmpty()) {
                            Text(
                                text = sharedUserId,
                                fontSize = 12.sp,
                                color = colorScheme.onSurfaceVariantSummary,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier
                                    .basicMarquee(),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        Text(
                            text = stringResource(com.kulipai.luahook.core.ui.R.string.group_contains_apps, affectedApps.size),
                            fontSize = 12.sp,
                            color = colorScheme.onSurfaceVariantSummary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .basicMarquee(),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
                Column(
                    modifier = Modifier,
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (userId != 0) {
                        StatusTag(
                            label = "USER $userId",
                            backgroundColor = colorScheme.primary.copy(alpha = 0.8f),
                            contentColor = colorScheme.onPrimary
                        )
                        StatusTag(
                            label = "UID $appId",
                            backgroundColor = colorScheme.primary.copy(alpha = 0.8f),
                            contentColor = colorScheme.onPrimary
                        )
                    } else {
                        StatusTag(
                            label = "UID $appUid",
                            backgroundColor = colorScheme.primary.copy(alpha = 0.8f),
                            contentColor = colorScheme.onPrimary
                        )
                    }
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp),
        ) {
            SuperSwitch(
                startAction = {
                    Icon(
                        imageVector = Icons.Rounded.Security,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 16.dp),
                        tint = colorScheme.onBackground
                    )
                },
                title = stringResource(id = com.kulipai.luahook.core.ui.R.string.apps),
                checked = true *//*isRootGranted*//*,
                onCheckedChange = { onProfileChange(profile.copy(allowSu = it)) },
            )
        }

        val initialRootMode = if (profile.rootUseDefault) {
            Mode.Default
        } else if (profile.rootTemplate != null) {
            Mode.Template
        } else {
            Mode.Custom
        }
        var rootMode by rememberSaveable {
            mutableStateOf(initialRootMode)
        }
        val nonRootMode = if (profile.nonRootUseDefault) Mode.Default else Mode.Custom
        val dropdownMode = if (isRootGranted) rootMode else nonRootMode
        ProfileBox(dropdownMode, isRootGranted) { mode ->
            if (isRootGranted) {
                when (mode) {
                    Mode.Default, Mode.Custom -> {
                        onProfileChange(
                            profile.copy(
                                rootUseDefault = mode == Mode.Default,
                                rootTemplate = null
                            )
                        )
                        rootMode = mode
                    }

                    Mode.Template -> {
                        val templates = listAppProfileTemplates()
                        if (templates.isNotEmpty()) {
                            val selected = profile.rootTemplate ?: templates[0]
                            val info = getTemplateInfoById(selected)
                            if (info != null && setSepolicy(selected, info.rules.joinToString("\n"))) {
                                onProfileChange(
                                    profile.copy(
                                        rootUseDefault = false,
                                        rootTemplate = selected,
                                        uid = info.uid,
                                        gid = info.gid,
                                        groups = info.groups,
                                        capabilities = info.capabilities,
                                        context = info.context,
                                        namespace = info.namespace,
                                    )
                                )
                            } else if (profile.rootTemplate != selected || profile.rootUseDefault) {
                                onProfileChange(
                                    profile.copy(
                                        rootUseDefault = false,
                                        rootTemplate = selected
                                    )
                                )
                            }
                            rootMode = Mode.Template
                        }
                    }
                }
            } else {
                onProfileChange(profile.copy(nonRootUseDefault = (mode == Mode.Default)))
            }
        }
        Spacer(Modifier.height(12.dp))

        AnimatedVisibility(
            visible = isRootGranted,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(bottom = if (rootMode != Mode.Default) 12.dp else 0.dp),
            ) {
                AnimatedVisibility(
                    visible = rootMode == Mode.Template,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    TemplateConfig(
                        profile = profile,
                        onViewTemplate = onViewTemplate,
                        onManageTemplate = onManageTemplate,
                        onProfileChange = onProfileChange
                    )
                }
                AnimatedVisibility(
                    visible = rootMode == Mode.Custom,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    RootProfileConfig(
                        fixedName = true,
                        enabled = rootMode == Mode.Custom,
                        profile = profile,
                        onProfileChange = onProfileChange
                    )
                }
            }
        }
        AnimatedVisibility(
            visible = !false*//*isRootGranted*//*,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(bottom =*//* if (nonRootMode != Mode.Default) 12.dp else *//*0.dp),
            ) {
                AnimatedVisibility(
                    visible = true*//*nonRootMode == Mode.Custom*//*,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    AppProfileConfig(
                        fixedName = true,
                        profile = profile,
                        enabled = true,
                        onProfileChange = onProfileChange
                    )
                }
            }
        }

        if (isUidGroup) {
            SmallTitle(
                text = "stringResource(R.string.app_profile_affects_following_apps)",
                modifier = Modifier.padding(top = 4.dp)
            )
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 12.dp),
            ) {
                Spacer(Modifier.height(3.dp))
                affectedApps.forEach { app ->
                    BasicComponent(
                        startAction = {
                            AppIconImage(
                                packageInfo = app.packageInfo,
                                label = app.label,
                                modifier = Modifier
                                    .padding(end = 12.dp)
                                    .size(40.dp)
                            )
                        },
                        title = app.label,
                        summary = app.packageName,
                        insideMargin = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
                Spacer(Modifier.height(3.dp))
            }
        }
    }
*/}

@Composable
private fun TopBar(
    onBack: () -> Unit,
    packageName: String,
    showActions: Boolean = true,
    scrollBehavior: ScrollBehavior,
    hazeState: HazeState,
    hazeStyle: HazeStyle,
    enableBlur: Boolean
) {/*
    TopAppBar(
        modifier = if (enableBlur) {
            Modifier.hazeEffect(hazeState) {
                style = hazeStyle
                blurRadius = 30.dp
                noiseFactor = 0f
            }
        } else {
            Modifier
        },
        color = if (enableBlur) Color.Transparent else colorScheme.surface,
        title = stringResource(R.string.profile),
        navigationIcon = {
            IconButton(
                modifier = Modifier.padding(start = 16.dp),
                onClick = onBack
            ) {
                val layoutDirection = LocalLayoutDirection.current
                Icon(
                    modifier = Modifier.graphicsLayer {
                        if (layoutDirection == LayoutDirection.Rtl) scaleX = -1f
                    },
                    imageVector = MiuixIcons.Back,
                    contentDescription = null,
                    tint = colorScheme.onBackground
                )
            }
        },
        actions = {
            if (showActions) {
                val showTopPopup = remember { mutableStateOf(false) }
                IconButton(
                    modifier = Modifier.padding(end = 16.dp),
                    onClick = { showTopPopup.value = true },
                    holdDownState = showTopPopup.value
                ) {
                    Icon(
                        imageVector = MiuixIcons.MoreCircle,
                        tint = colorScheme.onSurface,
                        contentDescription = stringResource(id = R.string.settings)
                    )
                }
                SuperListPopup(
                    show = showTopPopup,
                    onDismissRequest = { showTopPopup.value = false },
                    popupPositionProvider = ListPopupDefaults.ContextMenuPositionProvider,
                    alignment = PopupPositionProvider.Align.TopEnd,
                ) {
                    ListPopupColumn {
                        val items = listOf(
                            stringResource(id = R.string.launch_app),
                            stringResource(id = R.string.force_stop_app),
                            stringResource(id = R.string.restart_app)
                        )

                        items.forEachIndexed { index, text ->
                            DropdownItem(
                                text = text,
                                optionSize = items.size,
                                index = index,
                                onSelectedIndexChange = { selectedIndex ->
                                    when (selectedIndex) {
                                        0 -> launchApp(packageName)
                                        1 -> forceStopApp(packageName)
                                        2 -> restartApp(packageName)
                                    }
                                    showTopPopup.value = false
                                }
                            )
                        }
                    }
                }
            }
        },
        scrollBehavior = scrollBehavior
    )
*/}

@Composable
private fun ProfileBox(
/*    mode: Mode,
    hasTemplate: Boolean,
    onModeChange: (Mode) -> Unit,*/
) {/*
    val defaultText = stringResource(R.string.profile_default)
    val templateText = stringResource(R.string.profile_template)
    val customText = stringResource(R.string.profile_custom)
    val list =
        remember(hasTemplate, defaultText, templateText, customText) {
            buildList {
                add(defaultText)
                if (hasTemplate) {
                    add(templateText)
                }
                add(customText)
            }
        }

    val modesAndTitles = remember(hasTemplate, defaultText, templateText, customText) {
        buildList {
            add(Mode.Default to defaultText)
            if (hasTemplate) {
                add(Mode.Template to templateText)
            }
            add(Mode.Custom to customText)
        }
    }
    val selectedIndex = modesAndTitles.indexOfFirst { it.first == mode }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
    ) {
        SuperDropdown(
            title = stringResource(R.string.profile),
            items = list,
            startAction = {
                Icon(
                    Icons.Rounded.AccountCircle,
                    modifier = Modifier.padding(end = 16.dp),
                    contentDescription = null,
                    tint = colorScheme.onBackground
                )
            },
            selectedIndex = if (selectedIndex == -1) 0 else selectedIndex,
        ) {
            onModeChange(modesAndTitles[it].first)
        }
    }
*/}
