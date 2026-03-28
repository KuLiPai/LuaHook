package com.kulipai.luahook.main

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.kulipai.luahook.main.components.bottombar.BottomBar
import com.kulipai.luahook.main.components.bottombar.LocalMainPagerState
import com.kulipai.luahook.main.components.bottombar.MainPagerState
import com.kulipai.luahook.main.components.bottombar.SideRail
import com.kulipai.luahook.main.components.bottombar.rememberMainPagerState
import com.kulipai.luahook.main.navigation3.LocalNavigator
import com.kulipai.luahook.main.navigation3.Navigator
import com.kulipai.luahook.main.navigation3.Route
import com.kulipai.luahook.feature.main.pager.home.HomePager
import com.kulipai.luahook.feature.main.pager.module.ModulePager
import com.kulipai.luahook.feature.main.pager.setting.SettingPager
import com.kulipai.luahook.feature.main.pager.superuser.SuperUserPager
import com.kulipai.luahook.core.theme.LocalEnableBlur
import com.kulipai.luahook.core.theme.LocalEnableFloatingBottomBar
import com.kulipai.luahook.core.theme.LocalEnableFloatingBottomBarBlur
import com.kulipai.luahook.core.theme.LocalUiMode
import com.kulipai.luahook.core.theme.UiMode
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeSource
import top.yukonga.miuix.kmp.theme.MiuixTheme

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MainScreen() {
    val navController = LocalNavigator.current
    val enableBlur = LocalEnableBlur.current
    val enableFloatingBottomBar = LocalEnableFloatingBottomBar.current
    val enableFloatingBottomBarBlur = LocalEnableFloatingBottomBarBlur.current
    var currentPage by rememberSaveable { mutableIntStateOf(0) }
    val pagerState = rememberPagerState(initialPage = currentPage, pageCount = { 4 })
    LaunchedEffect(pagerState.currentPage) {
        currentPage = pagerState.currentPage
    }
    val mainPagerState = rememberMainPagerState(pagerState)
    var userScrollEnabled by remember { mutableStateOf(true) }
    val uiMode = LocalUiMode.current
    val surfaceColor = when (uiMode) {
        UiMode.Material -> MaterialTheme.colorScheme.surface // Haze is not used in Material, this is just a placeholder
        UiMode.Miuix -> MiuixTheme.colorScheme.surface
    }
    val hazeState = remember { HazeState() }
    val hazeStyle = if (enableBlur) {
        HazeStyle(
            backgroundColor = surfaceColor,
            tint = HazeTint(surfaceColor.copy(0.8f))
        )
    } else {
        HazeStyle.Unspecified
    }

    val backdrop = rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }

    LaunchedEffect(mainPagerState.pagerState.currentPage) {
        mainPagerState.syncPage()
    }

    MainScreenBackHandler(mainPagerState, navController)

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val useNavigationRail = isLandscape && !(uiMode == UiMode.Miuix && enableFloatingBottomBar)

    CompositionLocalProvider(
        LocalMainPagerState provides mainPagerState
    ) {
        val pagerContent = @Composable { bottomInnerPadding: Dp ->
            HorizontalPager(
                modifier = Modifier
                    .then(if (enableBlur) Modifier.hazeSource(state = hazeState) else Modifier)
                    .then(if (enableFloatingBottomBar && enableFloatingBottomBarBlur) Modifier.layerBackdrop(backdrop) else Modifier),
                state = mainPagerState.pagerState,
                beyondViewportPageCount = 3,
                userScrollEnabled = userScrollEnabled,
            ) {
                when (it) {
                    0 -> HomePager(navController, bottomInnerPadding)
                    1 -> SuperUserPager(navController, bottomInnerPadding)
                    2 -> ModulePager(navController, bottomInnerPadding)
                    3 -> SettingPager(navController, bottomInnerPadding)
                }
            }
        }

        if (useNavigationRail) {
            val startInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout)
                .only(WindowInsetsSides.Start)
            val navBarBottomPadding = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()

            when (uiMode) {
                UiMode.Material -> Scaffold { _ ->
                    Row {
                        SideRail(
                            hazeState = hazeState,
                            hazeStyle = hazeStyle,
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .consumeWindowInsets(startInsets)
                        ) {
                            pagerContent(navBarBottomPadding)
                        }
                    }
                }

                UiMode.Miuix -> Scaffold { _ ->
                    Row {
                        SideRail(
                            hazeState = hazeState,
                            hazeStyle = hazeStyle,
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .consumeWindowInsets(startInsets)
                        ) {
                            pagerContent(navBarBottomPadding)
                        }
                    }
                }
            }
        } else {
            val bottomBar = @Composable {
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    BottomBar(
                        hazeState = hazeState,
                        hazeStyle = hazeStyle,
                        backdrop = backdrop,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            }

            when (uiMode) {
                UiMode.Material -> Scaffold(bottomBar = bottomBar) { innerPadding ->
                    pagerContent(innerPadding.calculateBottomPadding())
                }

                UiMode.Miuix -> Scaffold(bottomBar = bottomBar) { innerPadding ->
                    pagerContent(innerPadding.calculateBottomPadding())
                }
            }
        }
    }

}


@Composable
private fun MainScreenBackHandler(
    mainState: MainPagerState,
    navController: Navigator,
) {
    val isPagerBackHandlerEnabled by remember {
        derivedStateOf {
            navController.current() is Route.Main && navController.backStackSize() == 1 && mainState.selectedPage != 0
        }
    }

    val navEventState = rememberNavigationEventState(NavigationEventInfo.None)

    NavigationBackHandler(
        state = navEventState,
        isBackEnabled = isPagerBackHandlerEnabled,
        onBackCompleted = {
            mainState.animateToPage(0)
        }
    )
}