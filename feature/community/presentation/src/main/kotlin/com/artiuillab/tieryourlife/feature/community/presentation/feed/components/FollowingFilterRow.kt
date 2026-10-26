package com.artiuillab.tieryourlife.feature.community.presentation.feed.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.feature.community.presentation.R
import com.artiuillab.tieryourlife.feature.community.presentation.feed.CommunityFeedTestTags
import com.artiuillab.tieryourlife.feature.community.presentation.feed.FollowingFilter

/** Everything followed, or only the people, or only the topics: three chips, since the feed arrives as one. */
@Composable
internal fun FollowingFilterRow(
    selected: FollowingFilter,
    onSelect: (FollowingFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FollowingFilter.entries.forEach { filter ->
            FilterChip(
                selected = filter == selected,
                onClick = { onSelect(filter) },
                label = { Text(stringResource(filter.labelRes)) },
                modifier = Modifier.testTag(CommunityFeedTestTags.followingFilter(filter)),
            )
        }
    }
}

private val FollowingFilter.labelRes: Int
    get() = when (this) {
        FollowingFilter.All -> R.string.following_filter_all
        FollowingFilter.People -> R.string.following_filter_people
        FollowingFilter.Topics -> R.string.following_filter_topics
    }
