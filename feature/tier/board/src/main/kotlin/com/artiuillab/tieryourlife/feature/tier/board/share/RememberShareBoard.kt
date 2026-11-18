package com.artiuillab.tieryourlife.feature.tier.board.share

import android.content.res.Resources
import android.net.Uri
import android.widget.Toast
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import com.artiuillab.tieryourlife.core.theme.color.TierYourLifeMedia
import com.artiuillab.tieryourlife.core.theme.layout.LocalBoardLayoutMemory
import com.artiuillab.tieryourlife.feature.tier.board.R
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import kotlinx.coroutines.launch

/**
 * Share as image, as the list's menu does it: the board as a picture in the
 * theme on screen, names on the pictures when the board shows them, and a
 * word when nothing could take it.
 */
@Composable
fun rememberShareBoard(ownName: String? = null): (TierList) -> Unit {
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val media = TierYourLifeMedia.current
    val colors = MaterialTheme.colorScheme
    val names = LocalBoardLayoutMemory.current.namesOnCards
    val palette = BoardPalette(
        surface = colors.surface,
        onSurface = colors.onSurface,
        onSurfaceVariant = colors.onSurfaceVariant,
        outlineVariant = colors.outlineVariant,
        onBand = media.onTierBand,
        unrankedBand = media.unrankedRibbon,
        isDark = media.isDark,
    )
    return { list ->
        scope.launch {
            val link = list.publishedId?.let(::publishedListLink)
            val caption = resources.getString(R.string.share_caption, list.title, link ?: SHARE_LINK)
            val words = SharedWords(
                counts = sharedCounts(resources, list, author = ownName.takeIf { list.publishedId != null && !list.anonymous }),
                notPlaced = list.tiers.filter { it.isPool }.sumOf { it.items.size }.takeIf { it > 0 }
                    ?.let { resources.getQuantityString(R.plurals.share_more_not_placed, it, it) },
                footer = resources.getString(R.string.share_rank_yourself, (link ?: SHARE_LINK).removePrefix("https://")),
            )
            if (!shareBoard(context, list, palette, caption, words, names)) {
                Toast.makeText(context, resources.getString(R.string.share_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }
}

/** The words a shared picture carries; the board itself is drawn from the list. */
class SharedWords(val counts: String, val notPlaced: String?, val footer: String)

/** As a published list's page is reached; the same link Copy link gives. */
fun publishedListLink(publishedId: String): String = "$SHARE_LINK/l/${Uri.encode(publishedId)}"

/** "30 cards · 18 placed · by Danylo": the author only on a published list. */
internal fun sharedCounts(resources: Resources, list: TierList, author: String?): String {
    val cards = list.tiers.sumOf { it.items.size }
    val placed = list.tiers.filterNot { it.isPool }.sumOf { it.items.size }
    return listOfNotNull(
        resources.getQuantityString(R.plurals.board_cards_count, cards, cards),
        resources.getQuantityString(R.plurals.board_cards_placed, placed, placed),
        author?.takeIf { it.isNotBlank() }?.let { resources.getString(R.string.share_by_author, it) },
    ).joinToString(" · ")
}
