package com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.layout.ContentWidth
import com.artiuillab.tieryourlife.core.theme.layout.atMost
import com.artiuillab.tieryourlife.feature.tier.domain.model.FeedSort
import com.artiuillab.tieryourlife.feature.tier.domain.model.FeedSource
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import com.artiuillab.tieryourlife.feature.tier.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.common.labelRes
import com.artiuillab.tieryourlife.feature.tier.presentation.community.components.AuthorPill
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.CommunityFeed
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.TierListsTestTags

private const val CARD_ART_ASPECT = 1f

/**
 * Two cards on a phone, counted rather than measured. An Adaptive grid counts
 * columns at every width, and one left to run on a phone quietly finds a third
 * somewhere around 464dp -- which the display-size setting alone is enough to
 * reach.
 *
 * Past the breakpoint it does the counting, and 200dp is what it counts by: two
 * at 600dp, three near 760, four at 1024, five near 1280, with no table of
 * widths to keep in step with anything. A hard-coded "four on a tablet" is
 * right for exactly one tablet.
 */
private val WIDE_CARD_MIN_WIDTH = 200.dp
private val WIDE_ENOUGH_FOR_MORE_COLUMNS = 600.dp
private const val PHONE_COLUMNS = 2

private val CAPTION_SCRIM = Brush.verticalGradient(
    listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f)),
)

// Far enough ahead that the next page is usually there before the
// bottom is, close enough that an idle feed does not fetch on its own.
private const val LOAD_MORE_ROWS_AHEAD = 4

@Composable
internal fun CommunityFeedList(
    feed: CommunityFeed,
    category: ListCategory?,
    onSelectCategory: (ListCategory?) -> Unit,
    onOpen: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenAuthor: ((String) -> Unit)? = null,
    onLongPress: ((PublishedListSummary) -> Unit)? = null,
    onNearEnd: () -> Unit = {},
    showCategories: Boolean = true,
    showAuthor: Boolean = true,
    /** Null on the screens that are already one author's or one person's own. */
    controls: FeedControls? = null,
) {
    val gridState = rememberLazyGridState()
    val shown = (feed as? CommunityFeed.Ready)?.lists?.size ?: 0

    LaunchedEffect(gridState, shown) {
        if (shown == 0) return@LaunchedEffect
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .collect { lastVisible ->
                if (lastVisible >= shown - LOAD_MORE_ROWS_AHEAD) onNearEnd()
            }
    }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val wideEnoughForMoreColumns = maxWidth >= WIDE_ENOUGH_FOR_MORE_COLUMNS
        Column(Modifier.fillMaxSize()) {
        if (controls != null) {
            FeedControlsRow(
                source = controls.source,
                sort = controls.sort,
                onSelectSource = controls.onSelectSource,
                onSelectSort = controls.onSelectSort,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        if (showCategories) {
            CategoryFilterRow(
                selected = category,
                onSelect = onSelectCategory,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        when (feed) {
            CommunityFeed.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(Modifier.testTag(TierListsTestTags.COMMUNITY_LOADING))
            }

            CommunityFeed.Failed -> CommunityMessage(
                title = stringResource(R.string.home_community_failed),
                body = stringResource(R.string.home_community_failed_body),
                action = stringResource(R.string.action_try_again),
                onAction = onRetry,
                testTag = TierListsTestTags.COMMUNITY_FAILED,
            )

            is CommunityFeed.FollowingNobody -> FollowingNobody(
                authors = feed.authors,
                loading = feed.loading,
                followed = feed.followed,
                onFollow = controls?.onFollow ?: {},
                onOpenAuthor = onOpenAuthor ?: {},
            )

            is CommunityFeed.Ready -> if (feed.lists.isEmpty()) {
                CommunityMessage(
                    // Following says who is missing, everybody says what is.
                    // "No lists" from a feed you built yourself is the wrong
                    // sentence: the lists are not missing, these people have
                    // not published any.
                    title = if (controls?.source == FeedSource.Following) {
                        followingEmptyTitle(category)
                    } else {
                        stringResource(R.string.home_community_empty)
                    },
                    body = if (controls?.source == FeedSource.Following) {
                        ""
                    } else {
                        stringResource(R.string.home_community_empty_body)
                    },
                    action = null,
                    onAction = {},
                    testTag = TierListsTestTags.COMMUNITY_EMPTY,
                )
            } else {
                LazyVerticalGrid(
                    columns = if (wideEnoughForMoreColumns) {
                        GridCells.Adaptive(minSize = WIDE_CARD_MIN_WIDTH)
                    } else {
                        GridCells.Fixed(PHONE_COLUMNS)
                    },
                    state = gridState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = if (wideEnoughForMoreColumns) 24.dp else 16.dp,
                        end = if (wideEnoughForMoreColumns) 24.dp else 16.dp,
                        bottom = 96.dp,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(if (wideEnoughForMoreColumns) 16.dp else 12.dp),
                    verticalArrangement = Arrangement.spacedBy(if (wideEnoughForMoreColumns) 16.dp else 12.dp),
                ) {
                    items(feed.lists, key = { it.id }) { summary ->
                        val reported = feed.justHidden[summary.id]
                        if (reported != null) {
                            HiddenTile(reported)
                            return@items
                        }
                        CommunityCard(
                            summary = summary,
                            onClick = { onOpen(summary.id) },
                            onLongClick = onLongPress?.let { press -> { press(summary) } },
                            showAuthor = showAuthor,
                            onAuthorClick = onOpenAuthor?.let { open ->
                                { open(summary.authorUid) }
                            },
                        )
                    }

                    if (feed.loadingMore) {
                        item(
                            key = TierListsTestTags.COMMUNITY_LOADING_MORE,
                            span = { GridItemSpan(maxLineSpan) },
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .testTag(TierListsTestTags.COMMUNITY_LOADING_MORE)
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center,
                            ) { CircularProgressIndicator(Modifier.size(24.dp)) }
                        }
                    }
                }
            }
        }
    }
    }
}

/**
 * What is left where a card was. Vanishing silently reads as "deleted",
 * which is not what happened, and a card that stayed would read as though
 * nothing had. It goes on the next load.
 */
@Composable
private fun HiddenTile(reported: Boolean) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
            .testTag(TierListsTestTags.COMMUNITY_HIDDEN_TILE)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(R.string.community_tile_hidden),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (reported) {
            Text(
                text = stringResource(R.string.community_tile_reported),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CommunityCard(
    summary: PublishedListSummary,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    showAuthor: Boolean,
    onAuthorClick: (() -> Unit)?,
) {
    Column(
        Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .testTag(TierListsTestTags.communityCard(summary.id)),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(CARD_ART_ASPECT),
        ) {
            ListArt(
                coverImageUrl = summary.coverImageUrl,
                previewImages = summary.previewImages,
                tierColors = summary.tierColors,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .background(CAPTION_SCRIM)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            ) {
                Text(
                    text = summary.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Column(Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
            // On an author's own profile the name is already the heading, so
            // repeating it on every card of theirs says nothing.
            if (showAuthor) {
                AuthorPill(
                    name = summary.authorName,
                    photoUrl = summary.authorPhotoUrl,
                    onClick = onAuthorClick,
                    testTag = TierListsTestTags.communityCardAuthor(summary.id),
                )
            }
            Text(
                modifier = Modifier.padding(start = 8.dp, top = 2.dp),
                // How many cards, and -- once anybody has taken it -- how many
                // people have. The second number is what the popular ordering
                // sorts by, so a reader can see why a list is where it is
                // rather than having to trust the word "popular".
                text = pluralStringResource(
                    R.plurals.community_item_count,
                    summary.itemCount,
                    summary.itemCount,
                ) + if (summary.takeCount > 0) {
                    " · " + pluralStringResource(
                        R.plurals.tier_lists_rankings_count,
                        summary.takeCount,
                        summary.takeCount,
                    )
                } else {
                    ""
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
private fun CommunityMessage(
    title: String,
    body: String,
    action: String?,
    onAction: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
            .padding(bottom = 96.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .atMost(ContentWidth.Message)
                .testTag(testTag),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (action != null) {
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = onAction) { Text(action) }
            }
        }
    }
}

private val previewFeed = CommunityFeed.Ready(
    listOf(
        PublishedListSummary(
            id = "1",
            title = "Every A24 film",
            authorUid = "u1",
            authorName = "Olena M.",
            category = ListCategory.FilmTv,
            itemCount = 34,
            tierColors = listOf("#B03A32", "#C06A25", "#B79A1F", "#5E8C3A", "#2F6E8F"),
            updatedAtMillis = 0,
        ),
        PublishedListSummary(
            id = "2",
            title = "Ramen in Kyiv",
            authorUid = "u2",
            authorName = "Taras",
            category = ListCategory.Food,
            itemCount = 12,
            tierColors = listOf("#B03A32", "#C06A25", "#B79A1F"),
            updatedAtMillis = 0,
        ),
    ),
)

@Preview(showBackground = true, heightDp = 560)
@Composable
private fun CommunityFeedPreview() = TierYourLifeTheme(false) {
    CommunityFeedList(
        feed = previewFeed,
        category = null,
        onSelectCategory = {},
        onOpen = {},
        onRetry = {},
    )
}

@Preview(showBackground = true, heightDp = 560)
@Composable
private fun CommunityFeedDarkPreview() = TierYourLifeTheme(true) {
    CommunityFeedList(
        feed = previewFeed,
        category = ListCategory.Food,
        onSelectCategory = {},
        onOpen = {},
        onRetry = {},
    )
}

@Preview(showBackground = true, heightDp = 500)
@Composable
private fun CommunityFeedEmptyDarkPreview() = TierYourLifeTheme(true) {
    CommunityFeedList(
        feed = CommunityFeed.Ready(emptyList()),
        category = null,
        onSelectCategory = {},
        onOpen = {},
        onRetry = {},
    )
}

/**
 * What the feed's two other questions are answered with, and how to change
 * them. Absent on the screens where they make no sense: one author's lists are
 * already one author's, and your own published lists are your own.
 */
internal data class FeedControls(
    val source: FeedSource,
    val sort: FeedSort,
    val onSelectSource: (FeedSource) -> Unit,
    val onSelectSort: (FeedSort) -> Unit,
    val onFollow: (String) -> Unit,
)

@Composable
private fun followingEmptyTitle(category: ListCategory?): String = if (category == null) {
    stringResource(R.string.home_following_empty_all)
} else {
    stringResource(R.string.home_following_empty, stringResource(category.labelRes))
}
