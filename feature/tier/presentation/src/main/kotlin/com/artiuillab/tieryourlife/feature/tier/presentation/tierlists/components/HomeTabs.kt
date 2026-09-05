package com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.layout.ContentWidth
import com.artiuillab.tieryourlife.core.theme.layout.atMost
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.HomeTab
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.TierListsTestTags

private val TAB_HEIGHT = 48.dp
private val INDICATOR_HEIGHT = 3.dp
private val INDICATOR_SHAPE = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)

/**
 * Not [androidx.compose.material3.PrimaryTabRow]: its indicator subtracts 32dp
 * of padding a bare Text does not have, leaving a stub under "Your lists".
 * The label is the only thing here a translation resizes, so it is measured.
 */
@Composable
internal fun HomeTabs(selected: HomeTab, onSelect: (HomeTab) -> Unit, modifier: Modifier = Modifier) {
    val tabs = HomeTab.entries
    val density = LocalDensity.current
    val labelWidths = remember { mutableStateListOf(*Array(tabs.size) { 0.dp }) }
    val selectedIndex = tabs.indexOf(selected)

    Surface(modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface) {
        Column {
            // The bar crosses the window; the tabs do not -- two tabs across
            // 1600dp are two 800dp targets. The cap goes here so the arithmetic
            // below divides a measure, not the window.
            BoxWithConstraints(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .atMost(ContentWidth.Reading)
                    .selectableGroup(),
            ) {
                val tabWidth = maxWidth / tabs.size
                Row(Modifier.fillMaxWidth().height(TAB_HEIGHT)) {
                    tabs.forEachIndexed { index, tab ->
                        Box(
                            modifier = Modifier
                                .width(tabWidth)
                                .height(TAB_HEIGHT)
                                .selectable(
                                    selected = tab == selected,
                                    role = Role.Tab,
                                    onClick = { onSelect(tab) },
                                )
                                .testTag(TierListsTestTags.tab(tab)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(tab.labelRes()),
                                modifier = Modifier.onSizeChanged {
                                    labelWidths[index] = with(density) { it.width.toDp() }
                                },
                                style = MaterialTheme.typography.titleSmall,
                                color = if (tab == selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                    }
                }
                // Held until the label is measured: composed at zero and then
                // animated, it grew out of the middle of the tab on every entry.
                val selectedWidth = labelWidths[selectedIndex]
                if (selectedWidth > 0.dp) {
                    Indicator(
                        width = selectedWidth,
                        start = tabWidth * selectedIndex + (tabWidth - selectedWidth) / 2,
                        modifier = Modifier.align(Alignment.BottomStart),
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun Indicator(width: Dp, start: Dp, modifier: Modifier = Modifier) {
    val animatedWidth by animateDpAsState(width, label = "indicatorWidth")
    val animatedStart by animateDpAsState(start, label = "indicatorStart")
    Box(
        modifier
            // The lambda overload reads the animation during placement. It
            // places relative to the layout direction, so Arabic mirrors on its
            // own; mirroring here as well put the indicator off the screen.
            .offset { IntOffset(animatedStart.roundToPx(), 0) }
            .width(animatedWidth)
            .height(INDICATOR_HEIGHT)
            .background(MaterialTheme.colorScheme.primary, INDICATOR_SHAPE),
    )
}

private fun HomeTab.labelRes(): Int = when (this) {
    HomeTab.Mine -> R.string.home_tab_mine
    HomeTab.Community -> R.string.home_tab_community
}

@Preview(showBackground = true)
@Composable
private fun HomeTabsPreview() = TierYourLifeTheme(false) {
    HomeTabs(selected = HomeTab.Mine, onSelect = {})
}

@Preview(showBackground = true)
@Composable
private fun HomeTabsCommunityDarkPreview() = TierYourLifeTheme(true) {
    HomeTabs(selected = HomeTab.Community, onSelect = {})
}
