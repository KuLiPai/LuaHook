package com.kulipai.luahook.feature.main.screen.home

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kulipai.luahook.core.navigation.Navigator
import com.kulipai.luahook.core.navigation.Route
import com.kulipai.luahook.core.ui.component.bottombar.LocalMainPagerState
import com.kulipai.luahook.core.ui.component.statuscard.StatusCard
import com.kulipai.luahook.core.ui.component.topbar.TopBar
import com.kulipai.luahook.core.ui.component.warningcard.WarningCard
import com.kulipai.luahook.core.ui.component.infocard.InfoCard
import com.kulipai.luahook.core.ui.component.donatecard.DonateCard
import com.kulipai.luahook.core.ui.component.learnmorecard.LearnMoreCard

import com.kulipai.luahook.feature.main.R


@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun HomePagerMaterial(
    navigator: Navigator,
    bottomInnerPadding: Dp
) {
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.app_name),
                materialScrollBehavior = scrollBehavior
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    ) { innerPadding ->
        val context = LocalContext.current
        var refreshKey by remember { mutableIntStateOf(0) }
        val scope = rememberCoroutineScope()

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {


            val mainState = LocalMainPagerState.current


            StatusCard(
                1, true,
//                ksuVersion,
//                lkmMode,
//                fullFeatured,
//                isSafeMode = remember(refreshKey) { Natives.isSafeMode },
//                isLateLoadMode = remember(refreshKey) { Natives.isLateLoadMode },
//                superuserCount = getSuperuserCount(),
//                moduleCount = getModuleCount(),
                onClickInstall = {
                    navigator.push(Route.Install)
                },
                onClickSuperuser = {
                    mainState.animateToPage(1)
                },
                onclickModule = {
                    mainState.animateToPage(2)
                }
            )
//            if (isManager) {
//                if (BuildConfig.IS_PR_BUILD) {
            WarningCard("当前为 PR 调试构建，请勿使用！")
//                } else if (Natives.isPrBuild) {
//                    WarningCard(stringResource(id = R.string.home_pr_kernel_warning))
//                }
//            }
//            if (ksuVersion != null && !Natives.isLkmMode) {
//                WarningCard(
//                    stringResource(id = R.string.home_gki_warning),
//                    MaterialTheme.colorScheme.tertiaryContainer
//                )
//            }
//            if (isManager && Natives.requireNewKernel()) {
//                WarningCard(
//                    stringResource(id = R.string.require_kernel_version).format(
//                        ksuVersion, Natives.MINIMAL_SUPPORTED_KERNEL
//                    )
//                )
//            }
//            if (ksuVersion != null && !rootAvailable()) {
//                WarningCard(
//                    stringResource(id = R.string.grant_root_failed)
//                )
//            }
//            val checkUpdate = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
//                .getBoolean("check_update", true)
//            if (checkUpdate) {
//                UpdateCard()
//            }
            InfoCard()
            DonateCard()
            LearnMoreCard()
            Spacer(Modifier.height(bottomInnerPadding))
        }
    }
}
