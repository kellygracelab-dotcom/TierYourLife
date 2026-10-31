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
import com.artiuillab.tieryourlife.feature.community.domain.model.Section
import com.artiuillab.tieryourlife.feature.community.presentation.R
import com.artiuillab.tieryourlife.feature.community.presentation.navigation.CommunityRoute
import com.artiuillab.tieryourlife.feature.tier.board.common.LocalSections

/**
 * Community on the Flip's cover (§7): the sections and nothing else, every
 * one of them, scrolled. No Following tab, no feed and no long-press menu; a
 * tap opens the section.
 */
@Composable
fun CoverCommunityScreen(onBack: () -> Unit, onOpenSection: (CommunityRoute.Section) -> Unit, modifier: Modifier = Modifier) {
    CoverCommunityGrid(
        onBack = onBack,
        sections = LocalSections.current.directory.shown,
        onOpenSection = { onOpenSection(CommunityRoute.Section(it)) },
        modifier = modifier,
    )
}

@Composable
internal fun CoverCommunityGrid(
    onBack: (() -> Unit)?,
    sections: List<Section>,
    onOpenSection: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .testTag(ExploreTestTags.COVER_HOME),
    ) {
        CoverHeader(title = stringResource(R.string.explore_title_community), onBack = onBack)
        TilesBlock(
            layout = HomeLayout.Cover,
            sections = sections,
            actions = TileActions(onOpen = onOpenSection),
            modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 8.dp),
        )
    }
}
