package com.artiuillab.tieryourlife.feature.community.presentation.section

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.artiuillab.tieryourlife.core.theme.ui.CheckIcon
import com.artiuillab.tieryourlife.core.theme.ui.ChevronDownIcon
import com.artiuillab.tieryourlife.feature.community.domain.model.FeedSort
import com.artiuillab.tieryourlife.feature.community.domain.model.Section
import com.artiuillab.tieryourlife.feature.community.domain.model.Topic
import com.artiuillab.tieryourlife.feature.community.presentation.R
import com.artiuillab.tieryourlife.feature.community.presentation.explore.FollowToggle
import com.artiuillab.tieryourlife.feature.community.presentation.explore.ScheduleIcon
import com.artiuillab.tieryourlife.feature.tier.board.common.label
import com.artiuillab.tieryourlife.feature.tier.board.common.rememberSection
import com.artiuillab.tieryourlife.feature.tier.board.common.sectionLabel
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory

internal object SectionTestTags {
    const val SCREEN = "section_screen"
    const val HEADER = "section_header"
    const val BAR = "section_bar"
    const val TOPICS = "section_topics"
    const val TOPIC_PICTURE = "section_topic_picture"
    const val SORT = "section_sort"
    const val LOADING = "section_loading"
    const val EMPTY = "section_empty"
    const val WAITING_SHEET = "section_waiting_sheet"
    const val COVER = "section_cover"
    const val COVER_RETRY = "section_cover_retry"

    fun topic(id: String) = "section_topic_$id"

    fun coverRow(id: String) = "section_cover_row_$id"
}

internal val SectionTitleStyle = TextStyle(fontSize = 28.sp, lineHeight = 36.sp)

/**
 * The expanded header (§3.1): the four top covers, or the section's art when
 * fewer qualify, under a scrim, with the name and Follow.
 */
@Composable
internal fun SectionHeader(
    section: Section,
    stats: Section?,
    onToggleFollow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(200.dp)
            .testTag(SectionTestTags.HEADER),
    ) {
        val collage = stats?.collage.orEmpty()
        if (collage.size >= COLLAGE_COVERS) {
            Row(Modifier.fillMaxSize().background(lookOf(section).tintNow), horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                collage.take(COLLAGE_COVERS).forEach { url ->
                    AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.weight(1f).fillMaxSize())
                }
            }
        } else {
            SectionArt(section, Modifier.fillMaxSize(), scrim = false)
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.35f),
                        0.35f to Color.Black.copy(alpha = 0.05f),
                        1f to Color.Black.copy(alpha = 0.68f),
                    ),
                ),
        )
        Row(
            Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(section.label(), Modifier.weight(1f), style = SectionTitleStyle, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            FollowToggle(following = stats?.following == true, onClick = onToggleFollow)
        }
    }
}

private const val COLLAGE_COVERS = 4

/**
 * The section's topics as tiles, like the sections on Home: the topic's own
 * picture, or the section's art until it has one, under its name. This
 * person's waiting ones come last, dimmed, and open what is known about them.
 */
@Composable
internal fun TopicTiles(
    section: Section,
    topics: List<Topic>,
    waiting: List<Topic>,
    onOpen: (Topic) -> Unit,
    onOpenWaiting: (Topic) -> Unit,
) = BoxWithConstraints {
    val columns = (maxWidth / TOPIC_TILE_MIN_WIDTH).toInt().coerceAtLeast(2)
    Column(Modifier.testTag(SectionTestTags.TOPICS), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            stringResource(R.string.section_topics),
            modifier = Modifier.padding(top = 16.dp),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        val tiles = topics.map { it to false } + waiting.map { it to true }
        tiles.chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { (topic, isWaiting) ->
                    TopicTile(section, topic, isWaiting, Modifier.weight(1f)) { if (isWaiting) onOpenWaiting(topic) else onOpen(topic) }
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** Topics a search found, from any section: each over its own section's art. */
@Composable
internal fun FoundTopicTiles(topics: List<Topic>, onOpen: (Topic) -> Unit) = BoxWithConstraints {
    val columns = (maxWidth / TOPIC_TILE_MIN_WIDTH).toInt().coerceAtLeast(2)
    Column(Modifier.testTag(SectionTestTags.TOPICS), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        topics.chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { topic ->
                    TopicTile(rememberSection(topic.section ?: ListCategory.Other.id), topic, waiting = false, Modifier.weight(1f)) { onOpen(topic) }
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun TopicTile(section: Section, topic: Topic, waiting: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .aspectRatio(TOPIC_TILE_RATIO)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .semantics { contentDescription = topic.name }
            .testTag(SectionTestTags.topic(topic.id)),
    ) {
        SectionArt(section, Modifier.fillMaxSize(), scrim = false)
        val picture = topic.pictureUrl
        if (picture != null) {
            AsyncImage(
                model = picture,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().testTag(SectionTestTags.TOPIC_PICTURE),
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.3f to Color.Black.copy(alpha = if (waiting) 0.45f else 0f),
                        1f to Color.Black.copy(alpha = 0.7f),
                    ),
                ),
        )
        if (waiting) {
            Row(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                ScheduleIcon(12.dp, Color.White)
                Text(stringResource(R.string.section_waiting_pill), style = TextStyle(fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium), color = Color.White)
            }
        }
        Text(
            topic.name,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 12.dp, end = 10.dp, bottom = 10.dp),
            style = TileName,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private const val TOPIC_TILE_RATIO = 2f
private val TOPIC_TILE_MIN_WIDTH = 170.dp
private val TileName = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium)

/** Popular or Newest from a menu, at the end of the row (§3.1). */
@Composable
internal fun SectionLoading() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(top = 96.dp)
            .testTag(SectionTestTags.LOADING),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
internal fun SectionEmpty() {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 32.dp, end = 32.dp, top = 72.dp)
            .testTag(SectionTestTags.EMPTY),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            stringResource(R.string.home_community_empty),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(R.string.home_community_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
internal fun SortRow(sort: FeedSort, onSort: (FeedSort) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .height(44.dp)
            .testTag(SectionTestTags.SORT),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.weight(1f))
        Box {
            TextButton(onClick = { open = true }) {
                Text(stringResource(sort.labelRes), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                ChevronDownIcon(22.dp, MaterialTheme.colorScheme.onSurfaceVariant)
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                listOf(FeedSort.Popular, FeedSort.Recent).forEach { option ->
                    DropdownMenuItem(
                        text = { Text(stringResource(option.labelRes)) },
                        trailingIcon = { if (option == sort) CheckIcon(18.dp, MaterialTheme.colorScheme.primary) },
                        onClick = {
                            open = false
                            onSort(option)
                        },
                    )
                }
            }
        }
    }
}

private val FeedSort.labelRes: Int
    get() = if (this == FeedSort.Popular) R.string.section_sort_popular else R.string.section_sort_newest

/** The proposer's sheet for a topic still waiting (§3.3). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WaitingTopicSheet(topic: Topic, onDismiss: () -> Unit) {
    val placed = topic.section ?: ListCategory.Other.id
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.testTag(SectionTestTags.WAITING_SHEET),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 40.dp)
                .navigationBarsPadding(),
        ) {
            Text(topic.name, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            Row(
                Modifier
                    .padding(top = 10.dp, bottom = 14.dp)
                    .height(24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                ScheduleIcon(14.dp, MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    stringResource(R.string.section_waiting_status),
                    style = TextStyle(fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                stringResource(R.string.section_waiting_body, sectionLabel(placed)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                stringResource(R.string.section_waiting_only_you),
                modifier = Modifier.padding(top = 10.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
            Row(Modifier.fillMaxWidth().padding(top = 22.dp)) {
                Spacer(Modifier.weight(1f))
                FilledTonalButton(onClick = onDismiss) { Text(stringResource(R.string.section_ok)) }
            }
        }
    }
}
