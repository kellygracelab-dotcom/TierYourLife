package com.artiuillab.tieryourlife.feature.tier.board.components.rows

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.theme.color.TierYourLifeMedia
import com.artiuillab.tieryourlife.core.theme.ui.CardLabel
import com.artiuillab.tieryourlife.feature.tier.board.components.FocusedImage
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem

@Composable
fun ItemTile(item: TierItem, width: Dp, height: Dp) {
    // A picture its source took away (404/403) leaves the card's name, as on the website, not an empty tile.
    var broken by remember(item.imageUrl) { mutableStateOf(false) }
    val picture = item.imageUrl.takeUnless { broken }
    if (picture == null && width >= TEXT_CARD_MIN_WIDTH) {
        TextCard(title = item.title, width = width, height = height, bordered = width == COMPACT_TILE_WIDTH || width == ROOMY_TILE_WIDTH)
        return
    }
    val media = TierYourLifeMedia.current
    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(6.dp))
            .background(media.tilePlaceholder),
        contentAlignment = Alignment.Center,
    ) {
        if (picture != null) {
            // Each poster fades in on its own; one already in memory is simply there.
            FocusedImage(
                model = picture,
                focus = item.focus,
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                crossfadeMillis = POSTER_FADE_MILLIS,
                onError = { broken = true },
            )
        } else {
            CardLabel(
                text = item.title,
                modifier = Modifier.padding(horizontal = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                color = media.tileLabel,
            )
        }
    }
}

private const val POSTER_FADE_MILLIS = 150
private val COMPACT_TILE_WIDTH = 40.dp
private val ROOMY_TILE_WIDTH = 52.dp
