package com.artiuillab.tieryourlife.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import com.artiuillab.tieryourlife.core.theme.layout.currentWindowShape
import com.artiuillab.tieryourlife.core.theme.ui.rememberArrival
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.HomeTab
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.components.HomeTabs

/**
 * The two halves of the home screen and the tabs between them. Neither half
 * owns the other, so which one is open is decided here; on a wide window the
 * rail does that job and no tabs are drawn. The halves come in as slots so
 * this can be drawn without their view models.
 *
 * [arrival] changes when this screen is arrived at or resumed and not when a
 * tab is switched, so a half can tell the two apart.
 */
@Composable
internal fun HomeScreen(
    startOnCommunity: Boolean,
    mine: @Composable (tabs: @Composable () -> Unit, arrival: Any?) -> Unit,
    community: @Composable (tabs: @Composable () -> Unit) -> Unit,
) {
    var tab by rememberSaveable { mutableStateOf(tabFor(startOnCommunity)) }
    // The route's flag wins only when it changes -- the rail navigating here
    // again to switch tabs. Applying it on every composition put somebody
    // back on Your lists each time they came back from a community list.
    var applied by rememberSaveable { mutableStateOf(startOnCommunity) }
    if (applied != startOnCommunity) {
        applied = startOnCommunity
        tab = tabFor(startOnCommunity)
    }
    val arrival = rememberArrival()

    val tabs: @Composable () -> Unit = if (currentWindowShape.hasRail) {
        {}
    } else {
        { HomeTabs(selected = tab, onSelect = { tab = it }) }
    }
    // Each half keeps what it remembered while the other was on screen: its
    // scroll, its open sheet, and the one board the rail asked for.
    val halves = rememberSaveableStateHolder()
    halves.SaveableStateProvider(tab) {
        when (tab) {
            HomeTab.Mine -> mine(tabs, arrival)
            HomeTab.Community -> community(tabs)
        }
    }
}

private fun tabFor(community: Boolean): HomeTab = if (community) HomeTab.Community else HomeTab.Mine
