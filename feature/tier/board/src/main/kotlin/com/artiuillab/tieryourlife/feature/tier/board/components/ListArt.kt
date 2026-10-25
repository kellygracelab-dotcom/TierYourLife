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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import coil3.compose.AsyncImage

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

/**
 * A published list is shown by whichever of three things it actually has. The
 * mosaic is not a placeholder: it is the author's own cards, which is the most
 * honest picture of what a reader is about to get.
 */
@Composable
fun ListArt(
    coverImageUrl: String?,
    previewImages: List<String>,
    tierColors: List<String>,
    modifier: Modifier = Modifier,
    testTag: String? = null,
) {
    val tagged = if (testTag == null) modifier else modifier.testTag(testTag)
    Box(tagged.background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
        when {
            coverImageUrl != null -> AsyncImage(
                model = coverImageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )

            previewImages.isNotEmpty() -> Mosaic(previewImages.distinct().take(MOSAIC_CELLS))
            else -> TierBands(tierColors)
        }
    }
}

/** Each picture once: a grid padded out with repeats read as a list of six copies of one film. */
@Composable
private fun Mosaic(images: List<String>) {
    var next = 0
    Column(Modifier.fillMaxSize()) {
        mosaicRows(images.size).forEach { perRow ->
            Row(Modifier.fillMaxWidth().weight(1f)) {
                repeat(perRow) {
                    AsyncImage(
                        model = images[next++],
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
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

private fun String.toColorOrNull(): Color? = runCatching {
    Color(this.toColorInt())
}.getOrNull()
