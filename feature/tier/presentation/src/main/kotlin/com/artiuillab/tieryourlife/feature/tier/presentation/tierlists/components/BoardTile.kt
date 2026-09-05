package com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.TierListsTestTags

/**
 * Your own board drawn the way the feed draws a stranger's: twenty boards of
 * album covers are told apart by their art faster than by name. The same art
 * as the feed, from [ListArt], so two ways of drawing one thing cannot drift.
 */
@OptIn(ExperimentalFoundationApi::class)
/** A disc under the star, so an outline survives a pale poster. */
private val SCRIM_DISC = 32.dp

/** Amber, and the same amber in both themes: the disc under it is always dark. */
private val STAR_ON = Color(0xFFFFC94D)
private const val SCRIM_ALPHA = 0.55f

@Composable
internal fun BoardTile(
    list: TierList,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    selectionMode: Boolean = false,
    selected: Boolean = false,
    onToggleFavourite: (() -> Unit)? = null,
) {
    val ranked = list.tiers.filterNot { it.isPool }.sumOf { it.items.size }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .then(
                if (selected) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                } else {
                    Modifier
                },
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .testTag(TierListsTestTags.tile(list.id)),
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(ART_RATIO), contentAlignment = Alignment.TopEnd) {
            ListArt(
                coverImageUrl = list.coverImageUrl,
                // A local photograph is drawn as it is on the board; only
                // publishing needs a picture to have travelled.
                previewImages = list.tiers
                    .flatMap { tier -> tier.items }
                    .mapNotNull { item -> item.imageUrl }
                    .take(PREVIEW_IMAGES),
                tierColors = list.tiers.filterNot { it.isPool }.map { it.colorLight }.take(TIER_COLOURS),
                modifier = Modifier.fillMaxWidth().aspectRatio(ART_RATIO),
            )
            if (selectionMode) {
                Box(Modifier.padding(8.dp)) { SelectionCheckbox(selected) }
            } else if (onToggleFavourite != null) {
                // On a disc: a bare outline disappears into a pale poster.
                Box(
                    Modifier
                        .padding(6.dp)
                        .size(SCRIM_DISC)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = SCRIM_ALPHA)),
                    contentAlignment = Alignment.Center,
                ) {
                    StarButton(
                        on = list.favouritedAt != null,
                        id = list.id,
                        onClick = onToggleFavourite,
                        size = SCRIM_DISC,
                        // Not the theme's grey, which vanished on a light poster:
                        // white on a dark disc reads on any picture.
                        colorOverride = if (list.favouritedAt != null) STAR_ON else Color.White,
                    )
                }
            }
        }
        Text(
            text = list.title,
            modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = pluralStringResource(R.plurals.tier_lists_ranked_count, ranked, ranked),
            modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 2.dp, bottom = 12.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Three to four across on a phone, the same shape the feed's cards have. */
internal val TILE_MIN_WIDTH = 150.dp

private const val ART_RATIO = 1.35f
private const val PREVIEW_IMAGES = 6
private const val TIER_COLOURS = 5
