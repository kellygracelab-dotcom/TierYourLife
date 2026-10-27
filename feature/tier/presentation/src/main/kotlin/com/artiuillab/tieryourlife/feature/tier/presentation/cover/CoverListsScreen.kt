package com.artiuillab.tieryourlife.feature.tier.presentation.cover

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.theme.ui.CoverHeader
import com.artiuillab.tieryourlife.core.theme.ui.CoverIconButton
import com.artiuillab.tieryourlife.core.theme.ui.CoverType
import com.artiuillab.tieryourlife.feature.tier.board.components.ListArt
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.core.theme.R as ThemeR
import com.artiuillab.tieryourlife.feature.tier.board.R as BoardR

/**
 * The cover's root (§4): your lists, in the main screen's order, one tap
 * from each. Nothing here is swiped, pressed long or selected.
 */
@Composable
fun CoverListsScreen(
    lists: List<TierList>,
    lastOpenedId: Long?,
    onOpen: (Long) -> Unit,
    onCommunity: () -> Unit,
    onOpenMain: () -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().testTag(CoverTestTags.LISTS),
        contentPadding = PaddingValues(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        item(key = "header") {
            CoverHeader(
                title = stringResource(ThemeR.string.tier_lists_title),
                actions = {
                    CoverIconButton(
                        onClick = onCommunity,
                        description = stringResource(R.string.cover_community),
                        testTag = CoverTestTags.COMMUNITY,
                    ) { GroupsIcon(20.dp, MaterialTheme.colorScheme.onSurfaceVariant) }
                },
            )
        }
        // Until the lists are read there is nothing to say about them, empty or not.
        if (lists.isEmpty() && !loading) {
            item(key = "empty") { NoListsYet(onOpenMain) }
        }
        items(lists, key = { it.id }) { list ->
            Box(Modifier.padding(horizontal = 8.dp)) {
                CoverListRow(list = list, selected = list.id == lastOpenedId, onClick = { onOpen(list.id) })
            }
        }
    }
}

@Composable
private fun CoverListRow(list: TierList, selected: Boolean, onClick: () -> Unit) {
    val colors = coverColors
    val title = if (selected) colors.onSelected else MaterialTheme.colorScheme.onSurface
    val meta = if (selected) colors.onSelected else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) colors.selected else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp)
            .testTag(CoverTestTags.listRow(list.id)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CoverThumbnail(list)
        Column(Modifier.weight(1f)) {
            Text(list.title, style = CoverType.RowTitle, color = title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(listMeta(list), style = CoverType.RowMeta, color = meta, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** The list's own cover, or the stripes of its tiers when it has no pictures. */
@Composable
internal fun CoverThumbnail(list: TierList) {
    ListArt(
        coverImageUrl = list.coverImageUrl,
        previewImages = list.coverImageUrls,
        tierColors = list.tiers.filterNot { it.isPool }.map { it.colorLight },
        modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)),
    )
}

/** "28 ranked · 6 in pool", "21 ranked", "34 ranked · saved". */
@Composable
private fun listMeta(list: TierList): String {
    val ranked = list.tiers.filterNot { it.isPool }.sumOf { it.items.size }
    val inPool = list.tiers.filter { it.isPool }.sumOf { it.items.size }
    return listOfNotNull(
        pluralStringResource(ThemeR.plurals.tier_lists_ranked_count, ranked, ranked),
        if (inPool > 0) pluralStringResource(BoardR.plurals.tier_lists_in_pool_count, inPool, inPool) else null,
        if (list.authorName != null) stringResource(R.string.cover_saved) else null,
    ).joinToString(" · ")
}

@Composable
private fun NoListsYet(onOpenMain: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ViewListIcon(32.dp, MaterialTheme.colorScheme.outline)
        Text(
            stringResource(R.string.cover_empty_title),
            style = CoverListType.EmptyTitle,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(R.string.cover_empty_body),
            style = CoverListType.HandOffBody,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Box(Modifier.padding(top = 4.dp)) {
            OutlinedButton(onClick = onOpenMain, modifier = Modifier.height(36.dp).testTag(CoverTestTags.OPEN_MAIN)) {
                Text(stringResource(R.string.cover_open_main), style = CoverType.Button)
            }
        }
    }
}
