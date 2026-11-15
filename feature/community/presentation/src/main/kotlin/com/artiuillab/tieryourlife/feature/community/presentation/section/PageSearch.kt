package com.artiuillab.tieryourlife.feature.community.presentation.section

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.theme.ui.ClearIcon
import com.artiuillab.tieryourlife.core.theme.ui.SearchTopBar
import com.artiuillab.tieryourlife.feature.community.domain.model.Section
import com.artiuillab.tieryourlife.feature.community.domain.model.Topic
import com.artiuillab.tieryourlife.feature.community.presentation.R
import com.artiuillab.tieryourlife.feature.community.presentation.explore.CardActions
import com.artiuillab.tieryourlife.feature.community.presentation.explore.HomeLayout
import com.artiuillab.tieryourlife.feature.community.presentation.explore.SearchResults
import com.artiuillab.tieryourlife.feature.community.presentation.explore.SearchResultsGrid

/** A section's or a topic's own search: what was typed, the server's answer, and whether it still looks only here. */
class PageSearch(
    val query: String?,
    val results: SearchResults,
    /** The page's name while the search stays on it; null once it looks everywhere. */
    val within: String?,
    /** Every section, to match what was typed once the search looks everywhere. */
    val sections: List<Section> = emptyList(),
    val calls: PageSearchCalls,
)

class PageSearchCalls(
    val onEnter: () -> Unit = {},
    val onQueryChange: (String) -> Unit = {},
    val onExit: () -> Unit = {},
    val onWiden: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onLoadMore: () -> Unit = {},
    val onOpenTopic: (Topic) -> Unit = {},
    val onOpenSection: (String) -> Unit = {},
)

/** In place of the page: the bar, the chip saying where it looks, and the answer. */
@Composable
internal fun PageSearchPane(search: PageSearch, layout: HomeLayout, cards: CardActions) {
    BackHandler(onBack = search.calls.onExit)
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .testTag(SectionTestTags.SEARCH),
    ) {
        SearchTopBar(
            query = search.query.orEmpty(),
            onQueryChange = search.calls.onQueryChange,
            onClose = search.calls.onExit,
            hint = stringResource(R.string.explore_search_hint),
            statusBarPadding = false,
        )
        search.within?.let { name ->
            val everywhere = stringResource(R.string.search_everywhere)
            InputChip(
                selected = true,
                onClick = search.calls.onWiden,
                label = { Text(stringResource(R.string.search_within, name)) },
                trailingIcon = { ClearIcon(18.dp, MaterialTheme.colorScheme.onSecondaryContainer) },
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .semantics { contentDescription = everywhere }
                    .testTag(SectionTestTags.SEARCH_SCOPE),
            )
        }
        SearchResultsGrid(
            results = search.results,
            query = search.query.orEmpty(),
            layout = layout,
            cards = cards,
            onRetry = search.calls.onRetry,
            onOpenTopic = search.calls.onOpenTopic,
            sections = if (search.within == null) search.sections else emptyList(),
            onOpenSection = search.calls.onOpenSection,
            onLoadMore = search.calls.onLoadMore,
        )
    }
}
