package com.artiuillab.tieryourlife.feature.community.presentation.section

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
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
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.artiuillab.tieryourlife.core.theme.ui.CheckIcon
import com.artiuillab.tieryourlife.core.theme.ui.ChevronDownIcon
import com.artiuillab.tieryourlife.core.theme.ui.SectionLabel
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
    const val SHOW_ALL_TOPICS = "section_show_all_topics"
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
 * The section's topics, apart from its lists: a heading with how many, low
 * tiles that read as a way in rather than as one more list, two rows at most
 * until Show all. This person's waiting ones come last, dimmed, and open what
 * is known about them. A line under the block parts it from the lists.
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
    val tiles = topics.map { it to false } + waiting.map { it to true }
    var all by rememberSaveable(section.id) { mutableStateOf(false) }
    val folded = tiles.size > columns * FOLDED_ROWS
    val shown = if (folded && !all) tiles.take(columns * FOLDED_ROWS) else tiles
    Column(Modifier.testTag(SectionTestTags.TOPICS)) {
        BlockHeading(stringResource(R.string.section_topics), tiles.size.takeIf { it > 0 }, Modifier.padding(top = 8.dp, bottom = 12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            shown.chunked(columns).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { (topic, isWaiting) ->
                        TopicTile(section, topic, isWaiting, Modifier.weight(1f)) { if (isWaiting) onOpenWaiting(topic) else onOpen(topic) }
                    }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
        if (folded) {
            TextButton(onClick = { all = !all }, modifier = Modifier.padding(top = 4.dp).testTag(SectionTestTags.SHOW_ALL_TOPICS)) {
                Text(
                    if (all) stringResource(R.string.section_show_fewer) else pluralStringResource(R.plurals.section_show_all_topics, tiles.size, tiles.size),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
        HorizontalDivider(Modifier.padding(top = 24.dp, bottom = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
    }
}

/** A block's heading in the section headings' style, with how many when that is known. */
@Composable
private fun BlockHeading(label: String, count: Int?, modifier: Modifier = Modifier) {
    SectionLabel(if (count == null) label else "$label · $count", modifier)
}

/** Topics a search found, from any section: each over its own section's tone. */
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

/**
 * A topic with a picture of its own shows only the section's tone until the
 * picture fades in, never the section's art first; one without wears the art.
 */
@Composable
private fun TopicTile(section: Section, topic: Topic, waiting: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .height(TOPIC_TILE_HEIGHT)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .semantics { contentDescription = topic.name }
            .testTag(SectionTestTags.topic(topic.id)),
    ) {
        val picture = topic.pictureUrl
        if (picture != null) {
            val context = LocalContext.current
            Box(Modifier.fillMaxSize().background(lookOf(section).tintNow))
            AsyncImage(
                model = remember(picture) { ImageRequest.Builder(context).data(picture).crossfade(TOPIC_FADE_MILLIS).build() },
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().testTag(SectionTestTags.TOPIC_PICTURE),
            )
        } else {
            SectionArt(section, Modifier.fillMaxSize(), scrim = false)
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = if (waiting) 0.45f else 0f),
                        1f to Color.Black.copy(alpha = 0.7f),
                    ),
                ),
        )
        if (waiting) {
            Row(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
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
                .padding(start = 12.dp, end = 10.dp, bottom = 8.dp),
            style = TileName,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private val TOPIC_TILE_HEIGHT = 64.dp
private const val FOLDED_ROWS = 2
private const val TOPIC_FADE_MILLIS = 150
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

/** The lists' heading, with how many when known, and their sort at the end of the row: it sorts only them. */
@Composable
internal fun SortRow(sort: FeedSort, onSort: (FeedSort) -> Unit, modifier: Modifier = Modifier, count: Int? = null) {
    var open by remember { mutableStateOf(false) }
    Row(
        modifier
            .fillMaxWidth()
            .height(44.dp)
            .testTag(SectionTestTags.SORT),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BlockHeading(stringResource(R.string.section_tier_lists), count, Modifier.weight(1f))
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
