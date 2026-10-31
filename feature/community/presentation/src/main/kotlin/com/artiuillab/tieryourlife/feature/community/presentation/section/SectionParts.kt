package com.artiuillab.tieryourlife.feature.community.presentation.section

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.artiuillab.tieryourlife.core.theme.ui.CheckIcon
import com.artiuillab.tieryourlife.core.theme.ui.ChevronDownIcon
import com.artiuillab.tieryourlife.core.theme.ui.PlusIcon
import com.artiuillab.tieryourlife.feature.community.domain.model.FeedSort
import com.artiuillab.tieryourlife.feature.community.domain.model.Section
import com.artiuillab.tieryourlife.feature.community.domain.model.Topic
import com.artiuillab.tieryourlife.feature.community.presentation.R
import com.artiuillab.tieryourlife.feature.community.presentation.explore.FollowSize
import com.artiuillab.tieryourlife.feature.community.presentation.explore.FollowToggle
import com.artiuillab.tieryourlife.feature.community.presentation.explore.ScheduleIcon
import com.artiuillab.tieryourlife.feature.community.presentation.explore.edgeToEdge
import com.artiuillab.tieryourlife.feature.community.presentation.followfeed.TopicSquare
import com.artiuillab.tieryourlife.feature.tier.board.common.label
import com.artiuillab.tieryourlife.feature.tier.board.common.sectionLabel
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory

internal object SectionTestTags {
    const val SCREEN = "section_screen"
    const val HEADER = "section_header"
    const val BAR = "section_bar"
    const val CHIPS = "section_chips"
    const val CHIP_ALL = "section_chip_all"
    const val CHIP_PICTURE = "section_chip_picture"
    const val SUGGEST = "section_chip_suggest"
    const val TOPIC_ROW = "section_topic_row"
    const val SORT = "section_sort"
    const val WAITING_SHEET = "section_waiting_sheet"
    const val COVER = "section_cover"
    const val COVER_RETRY = "section_cover_retry"

    fun chip(id: String) = "section_chip_$id"

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

/** All · approved topics · the viewer's waiting ones · Suggest a topic (§3.1, §1.7). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TopicChipsRow(
    topics: List<Topic>,
    waiting: List<Topic>,
    selected: Topic?,
    onSelect: (Topic?) -> Unit,
    onOpenWaiting: (Topic) -> Unit,
    onSuggest: (() -> Unit)?,
    side: Dp = 0.dp,
) {
    val listState = rememberLazyListState()
    val chips = topics
    val selectedIndex = selected?.let { s -> chips.indexOfFirst { it.id == s.id } }?.takeIf { it >= 0 }
    LaunchedEffect(selectedIndex) {
        // Kept fully in view: the chip that filters the page is the one to see.
        if (selectedIndex != null) listState.animateScrollToItem(selectedIndex + 1, scrollOffset = -48)
    }
    LazyRow(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .edgeToEdge(side)
            .testTag(SectionTestTags.CHIPS),
        contentPadding = PaddingValues(start = side, top = 12.dp, end = side, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "all") {
            TopicChip(stringResource(R.string.section_all), selected == null, Modifier.testTag(SectionTestTags.CHIP_ALL)) { onSelect(null) }
        }
        chips.forEach { topic ->
            item(key = topic.id) {
                TopicChip(topic.name, selected?.id == topic.id, Modifier.testTag(SectionTestTags.chip(topic.id)), topic.pictureUrl) { onSelect(topic) }
            }
        }
        waiting.forEach { topic ->
            item(key = "w-${topic.id}") { WaitingChip(topic.name, Modifier.testTag(SectionTestTags.chip(topic.id))) { onOpenWaiting(topic) } }
        }
        if (onSuggest != null) item(key = "suggest") {
            AssistChip(
                onClick = onSuggest,
                label = { Text(stringResource(R.string.section_suggest_topic), style = ChipLabel) },
                leadingIcon = { PlusIcon(16.dp, MaterialTheme.colorScheme.primary) },
                colors = AssistChipDefaults.assistChipColors(labelColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.height(32.dp).testTag(SectionTestTags.SUGGEST),
            )
        }
    }
}

private val ChipLabel = TextStyle(fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopicChip(label: String, selected: Boolean, modifier: Modifier, pictureUrl: String? = null, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = ChipLabel) },
        leadingIcon = when {
            selected -> {
                { CheckIcon(16.dp, MaterialTheme.colorScheme.onPrimaryContainer) }
            }
            pictureUrl != null -> {
                { ChipPicture(pictureUrl) }
            }
            else -> null
        },
        shape = RoundedCornerShape(8.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        modifier = modifier.height(32.dp),
    )
}

/** A topic's own picture at the size of a chip's icon, cropped to its rounded square, on the chip's tint while it loads. */
@Composable
private fun ChipPicture(url: String) {
    AsyncImage(
        model = url,
        contentDescription = null,
        modifier = Modifier
            .size(20.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .testTag(SectionTestTags.CHIP_PICTURE),
        contentScale = ContentScale.Crop,
    )
}

/** Like an unselected chip, but dashed: a word of this person's still waiting (§1.7). */
@Composable
private fun WaitingChip(name: String, modifier: Modifier, onClick: () -> Unit) {
    val outline = MaterialTheme.colorScheme.outline
    Row(
        modifier
            .height(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .drawBehind {
                drawRoundRect(
                    color = outline,
                    cornerRadius = CornerRadius(8.dp.toPx()),
                    style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))),
                )
            }
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ScheduleIcon(16.dp, outline)
        Text(stringResource(R.string.section_topic_waiting_chip, name), style = ChipLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Under the chips while a topic is chosen (§3.2): what it is, and its own Follow. */
@Composable
internal fun TopicRow(topic: Topic, sectionId: String, onToggleFollow: () -> Unit) {
    Column(Modifier.testTag(SectionTestTags.TOPIC_ROW)) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TopicSquare(topic.section ?: sectionId, 36.dp, 18.dp, topic.pictureUrl)
            Text(
                topic.name,
                Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            FollowToggle(following = topic.following, onClick = onToggleFollow, size = FollowSize.Topic)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

/** Popular or Newest from a menu, at the end of the row (§3.1). */
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
