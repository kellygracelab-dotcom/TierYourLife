package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.artiuillab.tieryourlife.core.theme.TierYourLifeMedia
import com.artiuillab.tieryourlife.core.theme.TierYourLifeType
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.common.tierRowColors
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.TierDetailTestTags

// Mock-literal colors with no matching theme role (verified against the app's Material3
// defaults and the unmodified error/errorContainer roles), hardcoded per the design's
// light/dark pair rather than approximated from an unrelated token.
private val CurrentTierTintLight = Color(0xFFEDEBFA)
private val CurrentTierTintDark = Color(0xFF2E2F45)
private val RemoveCircleLight = Color(0xFFF9DEDC)
private val RemoveCircleDark = Color(0xFF601410)
private val RemoveIconLight = Color(0xFF8C1D18)
private val RemoveIconDark = Color(0xFFF9DEDC)
private val RemoveTitleLight = Color(0xFFB3261E)
private val RemoveTitleDark = Color(0xFFFFB4AB)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MoveItemSheet(
    item: TierItem,
    currentTierId: Long,
    rankedTiers: List<Tier>,
    pool: Tier?,
    onMoveToTier: (tierId: Long, toPosition: Int) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(Modifier.testTag(TierDetailTestTags.MOVE_SHEET)) {
            MoveSheetHeader(item)
            Column(Modifier.padding(top = 8.dp)) {
                rankedTiers.forEach { tier ->
                    val isCurrent = tier.id == currentTierId
                    MoveSheetTierRow(
                        tier = tier,
                        isCurrent = isCurrent,
                        onClick = if (isCurrent) null else ({ onMoveToTier(tier.id, tier.items.size) }),
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))

                val isDark = TierYourLifeMedia.current.isDark
                MoveSheetActionRow(
                    testTag = TierDetailTestTags.MOVE_SHEET_REMOVE,
                    iconCircleColor = if (isDark) RemoveCircleDark else RemoveCircleLight,
                    icon = { DeleteOutlineIcon(22.dp, if (isDark) RemoveIconDark else RemoveIconLight) },
                    title = stringResource(R.string.tier_detail_move_sheet_remove_title),
                    titleColor = if (isDark) RemoveTitleDark else RemoveTitleLight,
                    subtitle = stringResource(R.string.tier_detail_move_sheet_remove_subtitle),
                    onClick = onRemove,
                    bottomPadding = 10.dp,
                )

                if (pool != null && pool.id != currentTierId) {
                    MoveSheetActionRow(
                        testTag = TierDetailTestTags.MOVE_SHEET_POOL,
                        iconCircleColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        icon = { SouthIcon(22.dp, MaterialTheme.colorScheme.onSurfaceVariant) },
                        title = stringResource(R.string.tier_detail_move_sheet_pool_title),
                        titleColor = MaterialTheme.colorScheme.onSurface,
                        subtitle = stringResource(R.string.tier_detail_move_sheet_pool_subtitle),
                        onClick = { onMoveToTier(pool.id, pool.items.size) },
                        bottomPadding = 12.dp,
                    )
                }
            }
        }
    }
}

@Composable
private fun MoveSheetHeader(item: TierItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MoveSheetThumbnail(item)
        Column(Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.tier_detail_move_sheet_subtitle),
                style = TierYourLifeType.current.captionUnderTitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MoveSheetThumbnail(item: TierItem) {
    val media = TierYourLifeMedia.current
    Box(
        modifier = Modifier
            .size(32.dp, 48.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(media.tilePlaceholder),
    ) {
        if (item.imageUrl != null) {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Composable
private fun MoveSheetTierRow(tier: Tier, isCurrent: Boolean, onClick: (() -> Unit)?) {
    val colors = tierRowColors(tier.colorLight, tier.colorDark)
    val isDark = TierYourLifeMedia.current.isDark
    val rowBackground = if (isCurrent) {
        if (isDark) CurrentTierTintDark else CurrentTierTintLight
    } else {
        Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .background(rowBackground)
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    // Not clickable, but still one accessible unit: without this, a screen
                    // reader would announce the swatch letter, caption, and "Currently here"
                    // as three disconnected nodes instead of one row.
                    Modifier.semantics(mergeDescendants = true) {}
                },
            )
            .testTag(TierDetailTestTags.moveSheetTierOption(tier.id))
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.band),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = tier.label,
                style = TierYourLifeType.current.tierSwatchLetter,
                color = colors.onBand,
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = tier.caption ?: tier.label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (isCurrent) {
                Text(
                    text = stringResource(R.string.tier_detail_move_sheet_current_tier),
                    style = TierYourLifeType.current.captionUnderTitle,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        if (isCurrent) {
            CheckIcon(20.dp, MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun MoveSheetActionRow(
    testTag: String,
    iconCircleColor: Color,
    icon: @Composable () -> Unit,
    title: String,
    titleColor: Color,
    subtitle: String,
    onClick: () -> Unit,
    bottomPadding: Dp,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .testTag(testTag)
            .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = bottomPadding),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconCircleColor),
            contentAlignment = Alignment.Center,
        ) { icon() }
        Column(Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = titleColor)
            Text(
                text = subtitle,
                style = TierYourLifeType.current.captionUnderTitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
