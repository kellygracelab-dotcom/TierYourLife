package com.artiuillab.tieryourlife.feature.community.presentation.explore

import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.community.presentation.R
import com.artiuillab.tieryourlife.feature.community.presentation.components.AnonymousFace
import com.artiuillab.tieryourlife.feature.community.presentation.components.AuthorFace
import com.artiuillab.tieryourlife.feature.tier.board.components.ListArt

/** Spec type sizes the M3 scale has no name for. */
internal val CardTitleStyle = TextStyle(fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium)
internal val MetaStyle = TextStyle(fontSize = 11.sp, lineHeight = 14.sp)
internal val CaptionStyle = TextStyle(fontSize = 11.sp, lineHeight = 15.sp)
internal val Body13Style = TextStyle(fontSize = 13.sp, lineHeight = 18.sp)
internal val TileNameStyle = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium)

/** The cover heights per window (§1.4, §6.1, §7.1). */
internal object CoverHeights {
    val Phone = 132.dp
    val Landscape = 96.dp
    val Tablet = 140.dp
}

/**
 * A published list in a feed (§1.4): the cover, then under it the title, the
 * author and the counts, with the reason it is here on the Following feed.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun FeedCard(
    summary: PublishedListSummary,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    coverHeight: Dp = CoverHeights.Phone,
    reason: String? = null,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .testTag(ExploreTestTags.card(summary.id)),
    ) {
        ListArt(
            coverImageUrl = summary.coverImageUrl,
            previewImages = summary.previewImages,
            tierColors = summary.tierColors,
            modifier = Modifier
                .fillMaxWidth()
                .height(coverHeight),
        )
        Column(Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 12.dp)) {
            if (reason != null) {
                Text(
                    text = reason,
                    modifier = Modifier
                        .padding(bottom = 3.dp)
                        .testTag(ExploreTestTags.reason(summary.id)),
                    style = CaptionStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = summary.title,
                modifier = Modifier.heightIn(min = 36.dp),
                style = CardTitleStyle,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            // The app's own lists stand with no author row; somebody's anonymous one says so.
            if (!summary.editorial || summary.byNobody) {
                Row(
                    Modifier
                        .padding(top = 8.dp)
                        .heightIn(min = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (summary.byNobody) {
                        AnonymousFace(size = 20.dp)
                    } else {
                        AuthorFace(photoUrl = summary.authorPhotoUrl, name = summary.authorName, size = 20.dp)
                    }
                    Text(
                        text = if (summary.byNobody) stringResource(R.string.community_anonymous) else summary.authorName,
                        style = MetaStyle,
                        color = if (summary.byNobody) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Text(
                text = cardMeta(summary),
                modifier = Modifier.padding(top = 4.dp),
                style = MetaStyle,
                color = MaterialTheme.colorScheme.outline,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun cardMeta(summary: PublishedListSummary): String {
    val cards = countText(R.plurals.explore_cards, summary.itemCount)
    if (summary.takeCount <= 0) return cards
    return cards + " · " + countText(R.plurals.explore_rankings, summary.takeCount)
}

/** Left where the card was after hiding it: vanishing reads as deleted. */
@Composable
internal fun HiddenCard(reported: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(R.string.community_tile_hidden),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (reported) {
            Text(
                text = stringResource(R.string.community_tile_reported),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

/** One pulse for a whole screen, so every skeleton breathes together (§0.3). */
@Composable
internal fun rememberSkeletonAlpha(): Float {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = InfiniteRepeatableSpec(
            animation = keyframes {
                durationMillis = 1400
                1f at 0
                0.55f at 700
                1f at 1400
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "skeleton alpha",
    )
    return alpha
}

/** A card not arrived yet (§1.5): its real size, in blocks, no shimmer and no spinner. */
@Composable
internal fun SkeletonCard(alpha: Float, modifier: Modifier = Modifier, coverHeight: Dp = CoverHeights.Phone) {
    val block = MaterialTheme.colorScheme.surfaceContainerHighest
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .alpha(alpha)
            .testTag(ExploreTestTags.SKELETON),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(coverHeight)
                .background(block),
        )
        Column(
            Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Bar(Modifier.fillMaxWidth(0.85f).height(13.dp))
            Bar(Modifier.fillMaxWidth(0.5f).height(13.dp))
            Row(Modifier.padding(top = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(block),
                )
                Spacer(Modifier.width(6.dp))
                Bar(Modifier.width(56.dp).height(10.dp))
            }
            Bar(Modifier.width(110.dp).height(10.dp))
        }
    }
}

@Composable
private fun Bar(modifier: Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
    )
}
