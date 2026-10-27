package com.artiuillab.tieryourlife.feature.community.presentation.explore

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.theme.ui.CoverHeader
import com.artiuillab.tieryourlife.feature.community.presentation.R
import com.artiuillab.tieryourlife.feature.community.presentation.navigation.CommunityRoute
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory

/**
 * Community on the Flip's cover (§7): the nine sections and nothing else. No
 * Following tab, no feed and no long-press menu; a tap opens the section.
 */
@Composable
fun CoverCommunityScreen(onBack: () -> Unit, onOpenSection: (CommunityRoute.Section) -> Unit, modifier: Modifier = Modifier) {
    CoverCommunityGrid(onBack = onBack, onOpenSection = { onOpenSection(CommunityRoute.Section(it.id)) }, modifier = modifier)
}

@Composable
internal fun CoverCommunityGrid(onBack: (() -> Unit)?, onOpenSection: (ListCategory) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .testTag(ExploreTestTags.COVER_HOME),
    ) {
        CoverHeader(title = stringResource(R.string.explore_title_community), onBack = onBack)
        // Only the names and the art: the counts are a phone's business.
        TilesBlock(
            layout = HomeLayout.Cover,
            sections = emptyList(),
            countsKnown = false,
            actions = TileActions(onOpen = onOpenSection),
            modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 8.dp),
        )
    }
}
