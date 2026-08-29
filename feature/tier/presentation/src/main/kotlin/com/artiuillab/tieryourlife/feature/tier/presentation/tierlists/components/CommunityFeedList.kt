package com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.tier.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.ChevronRightIcon
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.CommunityFeed
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.TierListsTestTags

@Composable
internal fun CommunityFeedList(
    feed: CommunityFeed,
    onOpen: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (feed) {
        CommunityFeed.Loading -> Box(
            modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(Modifier.testTag(TierListsTestTags.COMMUNITY_LOADING))
        }

        CommunityFeed.Failed -> CommunityMessage(
            title = stringResource(R.string.home_community_failed),
            body = stringResource(R.string.home_community_failed_body),
            action = stringResource(R.string.action_try_again),
            onAction = onRetry,
            testTag = TierListsTestTags.COMMUNITY_FAILED,
            modifier = modifier,
        )

        is CommunityFeed.Ready -> if (feed.lists.isEmpty()) {
            CommunityMessage(
                title = stringResource(R.string.home_community_empty),
                body = stringResource(R.string.home_community_empty_body),
                action = null,
                onAction = {},
                testTag = TierListsTestTags.COMMUNITY_EMPTY,
                modifier = modifier,
            )
        } else {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(feed.lists, key = { it.id }) { summary ->
                    CommunityCard(summary = summary, onClick = { onOpen(summary.id) })
                }
            }
        }
    }
}

@Composable
private fun CommunityCard(summary: PublishedListSummary, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .clickable(onClick = onClick)
            .testTag(TierListsTestTags.communityCard(summary.id))
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = summary.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.community_by_author, summary.authorName),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        ChevronRightIcon(20.dp, MaterialTheme.colorScheme.outline)
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
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
            .padding(bottom = 96.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
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

@Preview(showBackground = true, heightDp = 500)
@Composable
private fun CommunityFeedPreview() = TierYourLifeTheme(false) {
    CommunityFeedList(
        feed = CommunityFeed.Ready(
            listOf(
                PublishedListSummary("1", "Every A24 film", "Danylo K.", 34, 0),
                PublishedListSummary("2", "Ramen in Kyiv", "Olena", 12, 0),
            ),
        ),
        onOpen = {},
        onRetry = {},
    )
}

@Preview(showBackground = true, heightDp = 500)
@Composable
private fun CommunityFeedEmptyDarkPreview() = TierYourLifeTheme(true) {
    CommunityFeedList(feed = CommunityFeed.Ready(emptyList()), onOpen = {}, onRetry = {})
}
