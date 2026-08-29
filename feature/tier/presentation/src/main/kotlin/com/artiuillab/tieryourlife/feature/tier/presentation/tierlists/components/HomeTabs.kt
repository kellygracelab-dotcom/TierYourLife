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
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.HomeTab
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.TierListsTestTags

private val TAB_HEIGHT = 48.dp
private val INDICATOR_HEIGHT = 3.dp
private val INDICATOR_SHAPE = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)

/**
 * Written out rather than taken from [androidx.compose.material3.PrimaryTabRow]
 * for one reason: the indicator has to be as wide as the label. Material's own
 * asks for that too, but it takes the tab's intrinsic width and subtracts 32dp
 * for padding a tab whose content is a bare Text does not have. That leaves
 * 34dp under "Community" and hits the 24dp floor under "Your lists" -- a stub
 * centred beneath a word three times its length. The label is the only thing
 * in this bar whose size a translation changes, so it is measured.
 */
@Composable
internal fun HomeTabs(selected: HomeTab, onSelect: (HomeTab) -> Unit, modifier: Modifier = Modifier) {
    val tabs = HomeTab.entries
    val density = LocalDensity.current
    val labelWidths = remember { mutableStateListOf(*Array(tabs.size) { 0.dp }) }
    val selectedIndex = tabs.indexOf(selected)

    Surface(modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface) {
        Column {
            BoxWithConstraints(Modifier.selectableGroup()) {
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
                // Held back until the label has been measured. Composed at zero
                // and then animated, it grew out from the middle of the tab on
                // every entry to the screen -- a flourish nobody asked for,
                // where the point is that the bar is simply already there.
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
            .offset(x = animatedStart)
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
