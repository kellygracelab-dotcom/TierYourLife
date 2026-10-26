package com.artiuillab.tieryourlife.feature.community.presentation.feed.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.feature.community.domain.model.Topic
import com.artiuillab.tieryourlife.feature.community.presentation.R
import com.artiuillab.tieryourlife.feature.community.presentation.feed.CommunityFeedTestTags

/** The topics this person follows or proposed, one tap from the home screen; a waiting one says so. */
@Composable
internal fun YourTopicsRow(topics: List<Topic>, onOpen: (Topic) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().testTag(CommunityFeedTestTags.YOUR_TOPICS)) {
        Text(
            text = stringResource(R.string.community_your_topics),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(topics, key = { it.id }) { topic ->
                AssistChip(
                    onClick = { onOpen(topic) },
                    label = {
                        Text(
                            if (topic.pending) stringResource(R.string.community_topic_pending_chip, topic.name) else topic.name,
                        )
                    },
                    modifier = Modifier.testTag(CommunityFeedTestTags.yourTopic(topic.id)),
                )
            }
        }
    }
}
