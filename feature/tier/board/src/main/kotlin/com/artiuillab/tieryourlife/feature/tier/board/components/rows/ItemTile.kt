package com.artiuillab.tieryourlife.feature.tier.board.components.rows

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.artiuillab.tieryourlife.core.theme.color.TierYourLifeMedia
import com.artiuillab.tieryourlife.core.theme.ui.CardLabel
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem

@Composable
fun ItemTile(item: TierItem, width: Dp, height: Dp) {
    val media = TierYourLifeMedia.current
    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(6.dp))
            .background(media.tilePlaceholder),
        contentAlignment = Alignment.Center,
    ) {
        if (item.imageUrl != null) {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else if (width == COMPACT_TILE_WIDTH || width == ROOMY_TILE_WIDTH) {
            TextOnlyCard(title = item.title, small = width == COMPACT_TILE_WIDTH)
        } else {
            Text(
                text = item.title,
                modifier = Modifier.padding(horizontal = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                color = media.tileLabel,
            )
        }
    }
}

private val COMPACT_TILE_WIDTH = 40.dp
private val ROOMY_TILE_WIDTH = 52.dp
private val SMALL_LABEL = TextStyle(fontSize = 8.sp, lineHeight = 10.sp, fontWeight = FontWeight.Medium)
private val LARGE_LABEL = TextStyle(fontSize = 10.sp, lineHeight = 12.sp, fontWeight = FontWeight.Medium)

/**
 * A card with no picture at the short window's and the tablet's sizes: its
 * words, broken only between words. "Interstellar" split as "Interste / llar"
 * read as two words, neither of them the film.
 */
@Composable
private fun TextOnlyCard(title: String, small: Boolean) {
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .padding(4.dp),
        contentAlignment = Alignment.Center,
    ) {
        CardLabel(
            text = title,
            style = if (small) SMALL_LABEL else LARGE_LABEL,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 3,
        )
    }
}
