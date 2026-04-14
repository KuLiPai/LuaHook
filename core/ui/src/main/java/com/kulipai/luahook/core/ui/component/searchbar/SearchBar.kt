package com.kulipai.luahook.core.ui.component.searchbar

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kulipai.luahook.core.model.UiMode
import com.kulipai.luahook.core.theme.currentUiMode
import com.kulipai.luahook.core.ui.component.searchstatus.SearchStatus

@Composable
fun SearchBar(
    searchStatus: SearchStatus,
    onSearchStatusChange: (SearchStatus) -> Unit,
    searchBarTopPadding: Dp = 12.dp,
) {
    when(currentUiMode()) {
        UiMode.Material -> {}//TODO material 是 searchAppbar 参数不一致
        UiMode.Miuix -> SearchBarMiuix(searchStatus, onSearchStatusChange, searchBarTopPadding)
    }
}