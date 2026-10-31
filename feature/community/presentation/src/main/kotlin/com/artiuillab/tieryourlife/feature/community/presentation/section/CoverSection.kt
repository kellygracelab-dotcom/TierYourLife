package com.artiuillab.tieryourlife.feature.community.presentation.section

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.theme.ui.CoverHeader
import com.artiuillab.tieryourlife.core.theme.ui.CoverType
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.community.presentation.R
import com.artiuillab.tieryourlife.feature.community.presentation.explore.CloudOffIcon
import com.artiuillab.tieryourlife.feature.community.presentation.explore.FeedPhase
import com.artiuillab.tieryourlife.feature.community.presentation.explore.countText
import com.artiuillab.tieryourlife.feature.community.presentation.explore.rememberSkeletonAlpha
import com.artiuillab.tieryourlife.feature.tier.board.common.label
import com.artiuillab.tieryourlife.feature.tier.board.components.ListArt

/**
 * A section on the Flip's cover (§8): the header, then one
 * 56dp row per list. A tap hands the list to the main screen; there is
 * nothing else to press.
 */
@Composable
internal fun CoverSection(
    state: SectionUiState,
    onBack: () -> Unit,
    onOpenList: (PublishedListSummary) -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
) {
    val list = rememberLazyListState()
    val alpha = rememberSkeletonAlpha()
    val rows = state.feed.lists
    val nearEnd by remember { derivedStateOf { (list.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) >= list.layoutInfo.totalItemsCount - NEAR_END } }
    LaunchedEffect(nearEnd, rows.size) { if (nearEnd && rows.isNotEmpty() && state.feed.canLoadMore) onLoadMore() }
    LazyColumn(
        state = list,
        modifier = Modifier.fillMaxSize().testTag(SectionTestTags.COVER),
        contentPadding = PaddingValues(bottom = 8.dp),
    ) {
        item(key = "header") {
            CoverHeader(title = state.section.label(), onBack = onBack)
        }
        when {
            rows.isNotEmpty() -> items(rows, key = { it.id }) { summary -> CoverRow(summary) { onOpenList(summary) } }
            state.feed.phase == FeedPhase.Failed -> item(key = "failed") { CoverFailed(onRetry) }
            else -> items(SKELETON_ROWS) { CoverSkeletonRow(alpha) }
        }
    }
}

@Composable
private fun CoverRow(summary: PublishedListSummary, onClick: () -> Unit) {
    val cards = countText(R.plurals.explore_cards, summary.itemCount)
    val meta = if (summary.editorial) cards else "${summary.authorName} · $cards"
    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp)
            .testTag(SectionTestTags.coverRow(summary.id)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ListArt(
            coverImageUrl = summary.coverImageUrl,
            previewImages = summary.previewImages,
            tierColors = summary.tierColors,
            modifier = Modifier
                .size(width = 36.dp, height = 48.dp)
                .clip(RoundedCornerShape(6.dp)),
        )
        Column(Modifier.weight(1f)) {
            Text(summary.title, style = CoverType.SectionRowTitle, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(meta, style = CoverType.RowMeta, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun CoverSkeletonRow(alpha: Float) {
    val tone = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = alpha)
    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(width = 36.dp, height = 48.dp).clip(RoundedCornerShape(6.dp)).background(tone))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.width(150.dp).height(11.dp).clip(RoundedCornerShape(4.dp)).background(tone))
            Box(Modifier.width(96.dp).height(9.dp).clip(RoundedCornerShape(4.dp)).background(tone))
        }
    }
}

/** Nothing kept and nothing loaded: say so, and offer the one thing that can help. */
@Composable
private fun CoverFailed(onRetry: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CloudOffIcon(28.dp, MaterialTheme.colorScheme.outline)
        Text(
            stringResource(R.string.explore_failed_title),
            style = CoverType.SectionRowTitle,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        OutlinedButton(onClick = onRetry, modifier = Modifier.height(36.dp).testTag(SectionTestTags.COVER_RETRY)) {
            Text(stringResource(R.string.explore_try_again), style = CoverType.Button)
        }
    }
}

private const val SKELETON_ROWS = 4
private const val NEAR_END = 3
