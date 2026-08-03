package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.artiuillab.tieryourlife.core.theme.TierYourLifeMedia
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.common.tierRowColors
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.TierDetailTestTags

// LazyRow does not support intrinsic measurements (SubcomposeLayout), so the row
// can't size itself from its content via IntrinsicSize.Min. The height is instead
// derived explicitly: item tile height (64dp) + LazyRow's own vertical padding
// (10dp top + 10dp bottom).
private val TIER_ROW_HEIGHT = 84.dp

@Composable
internal fun TierRow(tier: Tier) {
    val colors = tierRowColors(tier.colorLight, tier.colorDark)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(TIER_ROW_HEIGHT)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.rowTint),
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(66.dp)
                .background(colors.band),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = tier.label,
                fontSize = 24.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Medium,
                color = colors.onBand,
            )
            tierCaption(tier.label)?.let { caption ->
                Text(
                    text = caption,
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    color = colors.onBand.copy(alpha = 0.7f),
                )
            }
        }

        LazyRow(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(10.dp)
                .testTag(TierDetailTestTags.tierItems(tier.id)),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(tier.items, key = { it.id }) { item ->
                ItemTile(item = item, width = 44.dp, height = 64.dp)
            }
        }
    }
}

@Composable
private fun tierCaption(label: String): String? = when (label) {
    "S" -> stringResource(R.string.tier_detail_caption_s)
    "A" -> stringResource(R.string.tier_detail_caption_a)
    "B" -> stringResource(R.string.tier_detail_caption_b)
    "C" -> stringResource(R.string.tier_detail_caption_c)
    "D" -> stringResource(R.string.tier_detail_caption_d)
    else -> null
}

@Composable
internal fun ItemTile(item: TierItem, width: Dp, height: Dp) {
    val media = TierYourLifeMedia.current
    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(6.dp))
            .background(media.tilePlaceholder),
        contentAlignment = Alignment.BottomCenter,
    ) {
        if (item.imageUrl != null) {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Text(
                text = item.title.take(6).uppercase(),
                modifier = Modifier.padding(bottom = 4.dp),
                fontSize = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = media.tileLabel,
            )
        }
    }
}
