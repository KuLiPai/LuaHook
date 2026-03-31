package com.kulipai.luahook.feature.main.pager.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kulipai.luahook.feature.main.components.bottombar.LocalMainPagerState
import com.kulipai.luahook.feature.main.components.miuix.DonateCard
import com.kulipai.luahook.feature.main.components.miuix.InfoCard
import com.kulipai.luahook.feature.main.components.miuix.LearnMoreCard
import com.kulipai.luahook.feature.main.components.miuix.StatusCard
import com.kulipai.luahook.feature.main.components.miuix.TopBar
import com.kulipai.luahook.feature.main.components.miuix.WarningCard
import com.kulipai.luahook.core.navigation.Navigator
import com.kulipai.luahook.core.navigation.Route
import com.kulipai.luahook.core.theme.currentEnableBlur
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeSource
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun HomePagerMiuix(
    navigator: Navigator,
    bottomInnerPadding: Dp
) {

    val scrollBehavior = MiuixScrollBehavior()
    val enableBlur = currentEnableBlur()
    val hazeState = remember { HazeState() }
    val hazeStyle = if (enableBlur) {
        HazeStyle(
            backgroundColor = colorScheme.surface,
            tint = HazeTint(colorScheme.surface.copy(0.8f))
        )
    } else {
        HazeStyle.Unspecified
    }

    Scaffold(
        topBar = {
            TopBar(
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
                .scrollEndHaptic()
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(horizontal = 12.dp)
                .let { if (enableBlur) it.hazeSource(state = hazeState) else it },
            contentPadding = innerPadding,
            overscrollEffect = null,
        ) {
            item {
//                val loadingDialog = rememberLoadingDialog()
                var refreshKey by remember { mutableIntStateOf(0) }
                val scope = rememberCoroutineScope()

//                val isManager = remember(refreshKey) { Natives.isManager }
//                val ksuVersion = remember(refreshKey) { if (isManager) Natives.version else null }
//                val lkmMode = remember(refreshKey) {
//                    ksuVersion?.let {
//                        if (kernelVersion.isGKI()) Natives.isLkmMode else null
//                    }
//                }
                val mainState = LocalMainPagerState.current

                Column(
                    modifier = Modifier.padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
//                    if (isManager) {
//                        if (BuildConfig.IS_PR_BUILD) {
                            WarningCard("当前为 PR 调试构建，请勿使用！")
//                        } else if (Natives.isPrBuild) {
//                            WarningCard(stringResource(id = R.string.home_pr_kernel_warning))
//                        }
//                    }
//                    if (ksuVersion != null && !Natives.isLkmMode) {
//                        WarningCard(stringResource(id = R.string.home_gki_warning))
//                    }
//                    if (isManager && Natives.requireNewKernel()) {
//                        WarningCard(
//                            stringResource(id = R.string.require_kernel_version)
//                                .format(ksuVersion, Natives.MINIMAL_SUPPORTED_KERNEL),
//                        )
//                    }
//                    if (ksuVersion != null && !rootAvailable()) {
//                        WarningCard(stringResource(id = R.string.grant_root_failed))
//                    }
                    StatusCard(
//                        kernelVersion,
                        1, true,
//                        isSafeMode = remember(refreshKey) { Natives.isSafeMode },
//                        isLateLoadMode = remember(refreshKey) { Natives.isLateLoadMode },
//                        isSELinuxPermissive = isSELinuxPermissive(),
//                        superuserCount = getSuperuserCount(),
//                        moduleCount = getModuleCount(),
                        onClickInstall = {
                            navigator.push(Route.Install)
                        },
//                        onClickJailbreak = {
////                            loadingDialog.showLoading()
////                            context.startService(Intent(context, MagicaService::class.java))
//                            scope.launch(Dispatchers.IO) {
//                                delay(30_000)
//                                withContext(Dispatchers.Main) {
////                                    loadingDialog.hide()
////                                    Toast.makeText(context, R.string.jailbreak_timeout, Toast.LENGTH_LONG).show()
//                                }
//                            }
//                        },
                        onClickSuperuser = {
                            mainState.animateToPage(1)
                        },
                        onclickModule = {
                            mainState.animateToPage(2)
                        },
                    )

//                    if (checkUpdate) {
//                        UpdateCard()
//                    }
                    InfoCard()
                    DonateCard()
                    LearnMoreCard()
                }
                Spacer(Modifier.height(bottomInnerPadding))
            }
        }

    }
}
