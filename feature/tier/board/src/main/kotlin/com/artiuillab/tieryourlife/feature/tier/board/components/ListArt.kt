package com.artiuillab.tieryourlife.feature.tier.board.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.feature.tier.board.common.tierColorOrNull
import com.artiuillab.tieryourlife.feature.tier.domain.model.CoverFocus

private const val MOSAIC_COLUMNS = 3

/** The most card pictures a mosaic shows: two rows of columns. */
const val MOSAIC_CELLS = MOSAIC_COLUMNS * 2

/**
 * How many pictures go in each row of a mosaic of [count] distinct pictures:
 * one row while they fit in it, two rows after, the fuller row on top.
 */
fun mosaicRows(count: Int): List<Int> {
    val shown = count.coerceIn(1, MOSAIC_CELLS)
    return if (shown <= MOSAIC_COLUMNS) listOf(shown) else listOf((shown + 1) / 2, shown / 2)
}

/** What [ListArt] draws, by test tag. */
object ListArtTestTags {
    const val COLLAGE = "list_art_collage"
    const val MOSAIC = "list_art_mosaic"
}

/**
 * A published list is shown by whichever of three things it actually has. The
 * mosaic is not a placeholder: it is the author's own cards, which is the most
 * honest picture of what a reader is about to get. [collageUrl] is that
 * mosaic drawn once as one picture, one load instead of six; on any failure
 * the mosaic shows as before.
 */
@Composable
fun ListArt(
    coverImageUrl: String?,
    previewImages: List<String>,
    tierColors: List<String>,
    modifier: Modifier = Modifier,
    testTag: String? = null,
    coverFocus: CoverFocus? = null,
    pictureFocus: Map<String, CoverFocus> = emptyMap(),
    collageUrl: String? = null,
) {
    val tagged = if (testTag == null) modifier else modifier.testTag(testTag)
    var collageFailed by remember(collageUrl) { mutableStateOf(false) }
    Box(tagged.background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
        when {
            coverImageUrl != null -> FocusedImage(
                model = coverImageUrl,
                focus = coverFocus,
                modifier = Modifier.fillMaxSize(),
            )

            collageUrl != null && !collageFailed -> FocusedImage(
                model = collageUrl,
                focus = null,
                modifier = Modifier.fillMaxSize().testTag(ListArtTestTags.COLLAGE),
                onError = { collageFailed = true },
            )

            previewImages.isNotEmpty() -> Mosaic(previewImages.distinct().take(MOSAIC_CELLS), pictureFocus)
            else -> TierBands(tierColors)
        }
    }
}

/**
 * Each picture once: a grid padded out with repeats read as a list of six
 * copies of one film. Each cell cut by the covers' formula, at the part of the
 * card's picture somebody chose, or its default: a poster's title near the top stays in.
 */
@Composable
private fun Mosaic(images: List<String>, pictureFocus: Map<String, CoverFocus>) {
    var next = 0
    Column(Modifier.fillMaxSize().testTag(ListArtTestTags.MOSAIC)) {
        mosaicRows(images.size).forEach { perRow ->
            Row(Modifier.fillMaxWidth().weight(1f)) {
                repeat(perRow) {
                    val image = images[next++]
                    FocusedImage(
                        model = image,
                        focus = pictureFocus[image],
                        modifier = Modifier.fillMaxSize().weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun TierBands(tierColors: List<String>) {
    val colors = tierColors.mapNotNull { it.toColorOrNull() }
        .ifEmpty { listOf(MaterialTheme.colorScheme.surfaceVariant) }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        colors.forEach { color ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(color),
            )
        }
    }
}

private fun String.toColorOrNull(): Color? = tierColorOrNull(this)
