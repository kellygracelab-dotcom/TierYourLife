package com.artiuillab.tieryourlife.feature.tier.board.components.rows

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artiuillab.tieryourlife.core.theme.color.TierYourLifeMedia
import com.artiuillab.tieryourlife.core.theme.layout.LocalBoardLayoutMemory
import com.artiuillab.tieryourlife.core.theme.ui.CardLabel
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailTestTags
import com.artiuillab.tieryourlife.feature.tier.board.components.FocusedImage
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem

/** [named]: the picture's name on a strip along its foot, as the board shows it unless turned off. */
@Composable
fun ItemTile(item: TierItem, width: Dp, height: Dp, named: Boolean = LocalBoardLayoutMemory.current.namesOnCards) {
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
            if (named) NameStrip(item.title, small = width < ROOMY_TILE_WIDTH)
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

/** White on a shade that darkens toward the foot, so a light poster still carries its name; two lines at most. */
@Composable
private fun BoxScope.NameStrip(title: String, small: Boolean) {
    Text(
        text = title,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, NAME_SHADE)))
            .padding(start = 3.dp, end = 3.dp, top = if (small) 6.dp else 10.dp, bottom = 2.dp)
            .testTag(TierDetailTestTags.NAME_STRIP)
            // The picture already says its name to a screen reader.
            .clearAndSetSemantics {},
        color = Color.White,
        fontSize = if (small) 8.sp else 10.sp,
        lineHeight = if (small) 9.sp else 12.sp,
        style = MaterialTheme.typography.labelSmall,
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

private val NAME_SHADE = Color.Black.copy(alpha = 0.78f)
private const val POSTER_FADE_MILLIS = 150
private val COMPACT_TILE_WIDTH = 40.dp
private val ROOMY_TILE_WIDTH = 52.dp
