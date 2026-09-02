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
 * One of your own boards, drawn the way the community feed draws a stranger's.
 *
 * The rows next door say how many cards are ranked and how many still wait,
 * which is the more useful thing most of the time and is why they are the
 * default. This is for the other times: twenty boards of album covers are told
 * apart by their art faster than by their names.
 *
 * Deliberately the same art as the feed, from [ListArt]: a board should look
 * the same to you as it does to everybody else, and two ways of drawing the
 * same thing would drift apart within a month.
 */
@OptIn(ExperimentalFoundationApi::class)
/** A disc under the star, so an outline survives a pale poster. */
private val SCRIM_DISC = 32.dp
private const val SCRIM_ALPHA = 0.35f

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
                // A local photograph is drawn here just as it is on the board.
                // Publishing is where a picture has to have travelled; looking
                // at your own boards is not.
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
                // On a disc of its own, because the art underneath is somebody
                // else's picture and a bare outline disappears into a pale one.
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
