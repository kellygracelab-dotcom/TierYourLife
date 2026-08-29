package com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.HomeTab
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.TierListsTestTags

private val TAB_HEIGHT = 48.dp

@Composable
internal fun HomeTabs(selected: HomeTab, onSelect: (HomeTab) -> Unit, modifier: Modifier = Modifier) {
    PrimaryTabRow(
        selectedTabIndex = HomeTab.entries.indexOf(selected),
        modifier = modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary,
    ) {
        HomeTab.entries.forEach { tab ->
            Tab(
                selected = tab == selected,
                onClick = { onSelect(tab) },
                modifier = Modifier
                    .height(TAB_HEIGHT)
                    .testTag(TierListsTestTags.tab(tab)),
                selectedContentColor = MaterialTheme.colorScheme.primary,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ) {
                Text(
                    text = stringResource(tab.labelRes()),
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }
    }
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
