package com.artiuillab.tieryourlife.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.artiuillab.tieryourlife.core.theme.layout.currentWindowShape
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.HomeTab
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.components.HomeTabs

/**
 * The two halves of the home screen and the tabs between them. Neither half
 * owns the other, so which one is open is decided here; on a wide window the
 * rail does that job and no tabs are drawn. The halves come in as slots so
 * this can be drawn without their view models.
 */
@Composable
internal fun HomeScreen(
    startOnCommunity: Boolean,
    mine: @Composable (tabs: @Composable () -> Unit) -> Unit,
    community: @Composable (tabs: @Composable () -> Unit) -> Unit,
) {
    var tab by rememberSaveable { mutableStateOf(tabFor(startOnCommunity)) }
    // Arriving here from the rail is how tabs are chosen on a wide window, and
    // the rail says which one by navigating.
    LaunchedEffect(startOnCommunity) { tab = tabFor(startOnCommunity) }

    val tabs: @Composable () -> Unit = if (currentWindowShape.hasRail) {
        {}
    } else {
        { HomeTabs(selected = tab, onSelect = { tab = it }) }
    }
    when (tab) {
        HomeTab.Mine -> mine(tabs)
        HomeTab.Community -> community(tabs)
    }
}

private fun tabFor(community: Boolean): HomeTab = if (community) HomeTab.Community else HomeTab.Mine
