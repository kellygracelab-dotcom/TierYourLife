package com.artiuillab.tieryourlife.feature.tier.board.share

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
fun rememberShareBoard(): (TierList) -> Unit {
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
            val caption = resources.getString(R.string.share_caption, list.title, SHARE_LINK)
            if (!shareBoard(context, list, palette, caption, resources.getString(R.string.share_footer), names)) {
                Toast.makeText(context, resources.getString(R.string.share_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }
}
